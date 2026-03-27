package org.example.route

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.example.type.OscBundle
import org.example.type.OscMessage
import org.example.arg.OscTimetag
import org.example.arg.toOscTimetag
import org.example.exception.OscLifecycleException
import org.example.exception.OscPacketSchedulerException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock

class OscPacketSchedulerTest {
    @Test
    fun `maxConcurrentDispatches one should keep strict dispatch order`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var now = Instant.fromEpochSeconds(10_000, 0)
        val events = Channel<String>(Channel.UNLIMITED)
        val scheduler = OscPacketScheduler.factory(
            dispatch = { message, _ ->
                events.send("start:${message.address}")
                if (message.address == "/a") delay(120)
                events.send("end:${message.address}")
            },
            scope = scope,
            now = { now },
            continueOnDispatchError = true,
            maxConcurrentDispatches = 1
        )

        val bundle = OscBundle(
            timeTag = (now + 1.seconds).toOscTimetag(),
            elements = listOf(
                OscMessage("/a"),
                OscMessage("/b")
            )
        )

        try {
            scheduler.start()
            scheduler.schedule(bundle)

            now += 1.seconds
            scheduler.tick()

            assertEquals("start:/a", withTimeout(1_000) { events.receive() })
            assertEquals("end:/a", withTimeout(1_000) { events.receive() })
            assertEquals("start:/b", withTimeout(1_000) { events.receive() })
            assertEquals("end:/b", withTimeout(1_000) { events.receive() })
        } finally {
            scheduler.stop()
            scope.cancel()
        }
    }

    @Test
    fun `maxConcurrentDispatches greater than one should allow parallel dispatch start`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val started = Channel<String>(Channel.UNLIMITED)
        val unblock = CompletableDeferred<Unit>()
        val scheduler = OscPacketScheduler.factory(
            dispatch = { message, _ ->
                started.send(message.address)
                unblock.await()
            },
            scope = scope,
            now = Clock.System::now,
            continueOnDispatchError = true,
            maxConcurrentDispatches = 2
        )

        try {
            scheduler.start()
            scheduler.schedule(OscMessage("/a"))
            scheduler.schedule(OscMessage("/b"))

            val s1 = withTimeout(1_000) { started.receive() }
            val s2 = withTimeout(1_000) { started.receive() }
            assertEquals(setOf("/a", "/b"), setOf(s1, s2))
        } finally {
            unblock.complete(Unit)
            scheduler.stop()
            scope.cancel()
        }
    }

    @Test
    fun `maxConcurrentDispatches should cap in flight dispatches`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val started = Channel<String>(Channel.UNLIMITED)
        val proceed = Channel<Unit>(Channel.UNLIMITED)
        val scheduler = OscPacketScheduler.factory(
            dispatch = { message, _ ->
                started.send(message.address)
                proceed.receive()
            },
            scope = scope,
            now = Clock.System::now,
            continueOnDispatchError = true,
            maxConcurrentDispatches = 2
        )

        try {
            scheduler.start()
            scheduler.schedule(OscMessage("/a"))
            scheduler.schedule(OscMessage("/b"))
            scheduler.schedule(OscMessage("/c"))

            withTimeout(1_000) { started.receive() }
            withTimeout(1_000) { started.receive() }
            assertEquals(null, withTimeoutOrNull(120) { started.receive() })
        } finally {
            repeat(3) { proceed.trySend(Unit) }
            proceed.close()
            scheduler.stop()
            scope.cancel()
        }
    }

    @Test
    fun `start after stop should throw`(): Unit = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val dispatched = CompletableDeferred<Unit>()
        val scheduler = OscPacketScheduler.factory(
            dispatch = { _, _ -> dispatched.complete(Unit) },
            scope = scope,
            now = { Clock.System.now() },
            continueOnDispatchError = true
        )

        try {
            assertFailsWith<OscLifecycleException> {
                scheduler.stop()
                scheduler.start()
                scheduler.schedule(OscMessage("/a"))
                withTimeout(1_000) { dispatched.await() }
            }

        } finally {
            scheduler.stop()
            scope.cancel()
        }
    }

    @Test
    fun `dispatch exception should not stop event loop when continueOnDispatchError is true`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val secondDispatched = CompletableDeferred<Unit>()
        var callCount = 0
        val scheduler = OscPacketScheduler.factory(
            dispatch = { _, _ ->
                callCount++
                if (callCount == 1) error("first dispatch failed")
                secondDispatched.complete(Unit)
            },
            scope = scope,
            now = { Clock.System.now() },
            continueOnDispatchError = true
        )

        try {
            scheduler.start()
            scheduler.schedule(OscMessage("/first"))
            scheduler.schedule(OscMessage("/second"))

            withTimeout(1_000) { secondDispatched.await() }
            assertEquals(2, callCount)
        } finally {
            scheduler.stop()
            scope.cancel()
        }
    }

    @Test
    fun `suspend schedule should enqueue multiple tasks`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val allDispatched = CompletableDeferred<Unit>()
        var callCount = 0
        val scheduler = OscPacketScheduler.factory(
            dispatch = { _, _ ->
                callCount++
                if (callCount == 3) allDispatched.complete(Unit)
            },
            scope = scope,
            now = { Clock.System.now() },
            continueOnDispatchError = true
        )

        try {
            scheduler.start()
            scheduler.schedule(OscMessage("/a"))
            scheduler.schedule(OscMessage("/b"))
            scheduler.schedule(OscMessage("/c"))

            withTimeout(1_000) { allDispatched.await() }
            assertEquals(3, callCount)
        } finally {
            scheduler.stop()
            scope.cancel()
        }
    }

    @Test
    fun `schedule should throw after scheduler stopped`(): Unit = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val scheduler = OscPacketScheduler.factory(
            dispatch = { _, _ -> },
            scope = scope,
            now = { Clock.System.now() },
            continueOnDispatchError = true
        )

        try {
            scheduler.start()
            scheduler.stop()

            assertFailsWith<OscPacketSchedulerException> {
                scheduler.schedule(OscMessage("/a"))
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `bundle recursive timetag should use max of parent and child`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var now = Instant.fromEpochSeconds(1_000, 0)
        val dispatched = Channel<String>(Channel.UNLIMITED)
        val scheduler = OscPacketScheduler.factory(
            dispatch = { packet, _ ->
                dispatched.send(packet.address)
            },
            scope = scope,
            now = { now },
            continueOnDispatchError = true
        )

        val rootTime = (now + 10.seconds).toOscTimetag()
        val earlierChildTime = (now + 5.seconds).toOscTimetag()
        val laterChildTime = (now + 20.seconds).toOscTimetag()
        val bundle = OscBundle(
            timeTag = rootTime,
            elements = listOf(
                OscMessage("/a"),
                OscBundle(
                    timeTag = earlierChildTime,
                    elements = listOf(OscMessage("/b"))
                ),
                OscBundle(
                    timeTag = laterChildTime,
                    elements = listOf(OscMessage("/c"))
                )
            )
        )

        try {
            scheduler.start()
            scheduler.schedule(bundle)

            now += 9.seconds
            scheduler.tick()
            assertEquals(null, withTimeoutOrNull(100) { dispatched.receive() })

            now += 1.seconds
            scheduler.tick()
            val first = withTimeout(1_000) { dispatched.receive() }
            val second = withTimeout(1_000) { dispatched.receive() }
            assertEquals(setOf("/a", "/b"), setOf(first, second))
            assertEquals(null, withTimeoutOrNull(100) { dispatched.receive() })

            now += 10.seconds
            scheduler.tick()
            assertEquals("/c", withTimeout(1_000) { dispatched.receive() })
        } finally {
            scheduler.stop()
            scope.cancel()
        }
    }

    @Test
    fun `immediately timetag should still respect parent max rule`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var now = Instant.fromEpochSeconds(2_000, 0)
        val dispatched = Channel<String>(Channel.UNLIMITED)
        val scheduler = OscPacketScheduler.factory(
            dispatch = { packet, _ ->
                dispatched.send(packet.address)
            },
            scope = scope,
            now = { now },
            continueOnDispatchError = true
        )

        val futureBundle = OscBundle(
            timeTag = (now + 10.seconds).toOscTimetag(),
            elements = listOf(
                OscBundle(
                    timeTag = OscTimetag.IMMEDIATELY,
                    elements = listOf(OscMessage("/future"))
                )
            )
        )
        val immediateBundle = OscBundle(
            timeTag = OscTimetag.IMMEDIATELY,
            elements = listOf(
                OscBundle(
                    timeTag = OscTimetag.IMMEDIATELY,
                    elements = listOf(OscMessage("/now"))
                )
            )
        )

        try {
            scheduler.start()
            scheduler.schedule(futureBundle)
            scheduler.schedule(immediateBundle)

            assertEquals("/now", withTimeout(1_000) { dispatched.receive() })
            assertEquals(null, withTimeoutOrNull(100) { dispatched.receive() })

            now += 10.seconds
            scheduler.tick()
            assertEquals("/future", withTimeout(1_000) { dispatched.receive() })
        } finally {
            scheduler.stop()
            scope.cancel()
        }
    }

    @Test
    fun `schedule message returns task id and immediate message cannot be cancelled reliably`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val dispatched = Channel<String>(Channel.UNLIMITED)
        val scheduler = OscPacketScheduler.factory(
            dispatch = { message, _ -> dispatched.send(message.address) },
            scope = scope,
            now = Clock.System::now,
            continueOnDispatchError = true
        )

        try {
            scheduler.start()
            val id = scheduler.schedule(OscMessage("/cancel-me"))
            assertTrue(id >= 0)
            assertFalse(scheduler.cancel(id))
            assertEquals("/cancel-me", withTimeout(1_000) { dispatched.receive() })
        } finally {
            scheduler.stop()
            scope.cancel()
        }
    }

    @Test
    fun `schedule message ids should be monotonic`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val scheduler = OscPacketScheduler.factory(
            dispatch = { _, _ -> },
            scope = scope,
            now = Clock.System::now,
            continueOnDispatchError = true
        )

        try {
            scheduler.start()
            val id1 = scheduler.schedule(OscMessage("/a"))
            val id2 = scheduler.schedule(OscMessage("/b"))
            val id3 = scheduler.schedule(OscMessage("/c"))

            assertTrue(id1 < id2 && id2 < id3)
        } finally {
            scheduler.stop()
            scope.cancel()
        }
    }

    @Test
    fun `cancel unknown id should return false`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val scheduler = OscPacketScheduler.factory(
            dispatch = { _, _ -> },
            scope = scope,
            now = Clock.System::now,
            continueOnDispatchError = true
        )

        try {
            scheduler.start()
            assertFalse(scheduler.cancel(999_999L))
        } finally {
            scheduler.stop()
            scope.cancel()
        }
    }

    @Test
    fun `schedule bundle returns group id and cancel group should cancel all messages`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var now = Instant.fromEpochSeconds(5_000, 0)
        val dispatched = Channel<String>(Channel.UNLIMITED)
        val scheduler = OscPacketScheduler.factory(
            dispatch = { message, _ -> dispatched.send(message.address) },
            scope = scope,
            now = { now },
            continueOnDispatchError = true
        )

        val bundle = OscBundle(
            timeTag = (now + 5.seconds).toOscTimetag(),
            elements = listOf(
                OscMessage("/a"),
                OscBundle(
                    timeTag = (now + 10.seconds).toOscTimetag(),
                    elements = listOf(OscMessage("/b"), OscMessage("/c"))
                )
            )
        )

        try {
            scheduler.start()
            val groupId = scheduler.schedule(bundle)
            assertTrue(scheduler.cancel(groupId))
            assertFalse(scheduler.cancel(groupId))

            now += 20.seconds
            scheduler.tick()
            assertEquals(null, withTimeoutOrNull(100) { dispatched.receive() })
        } finally {
            scheduler.stop()
            scope.cancel()
        }
    }

    @Test
    fun `cancel group after partial dispatch should cancel remaining tasks only`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var now = Instant.fromEpochSeconds(8_000, 0)
        val dispatched = Channel<String>(Channel.UNLIMITED)
        val scheduler = OscPacketScheduler.factory(
            dispatch = { message, _ -> dispatched.send(message.address) },
            scope = scope,
            now = { now },
            continueOnDispatchError = true
        )

        val bundle = OscBundle(
            timeTag = (now + 5.seconds).toOscTimetag(),
            elements = listOf(
                OscMessage("/first"),
                OscBundle(
                    timeTag = (now + 15.seconds).toOscTimetag(),
                    elements = listOf(OscMessage("/second"), OscMessage("/third"))
                )
            )
        )

        try {
            scheduler.start()
            val groupId = scheduler.schedule(bundle)

            now += 5.seconds
            scheduler.tick()
            assertEquals("/first", withTimeout(1_000) { dispatched.receive() })

            assertTrue(scheduler.cancel(groupId))

            now += 20.seconds
            scheduler.tick()
            assertEquals(null, withTimeoutOrNull(100) { dispatched.receive() })
        } finally {
            scheduler.stop()
            scope.cancel()
        }
    }
}
