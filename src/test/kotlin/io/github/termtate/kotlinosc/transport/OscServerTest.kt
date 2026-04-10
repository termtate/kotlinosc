package io.github.termtate.kotlinosc.transport

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import io.github.termtate.kotlinosc.type.OscMessage
import io.github.termtate.kotlinosc.codec.encodeToByteArray
import io.github.termtate.kotlinosc.exception.OscLifecycleException
import io.github.termtate.kotlinosc.io.OscByteWriter
import io.github.termtate.kotlinosc.route.DispatchMode
import io.github.termtate.kotlinosc.route.OscRouter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OscServerTest {

    private fun getAvailablePort() = DatagramSocket(0).use { it.localPort }

    @Test
    fun `server should receive udp packet and dispatch to router`() = runBlocking {
        val bindAddress = InetSocketAddress("127.0.0.1", getAvailablePort())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val router = OscRouter()
        val received = CompletableDeferred<OscMessage>()
        val server = OscServer(
            bindAddress = bindAddress,
            router = router,
            dispatchMode = DispatchMode.ALL_MATCH,
            scope = scope
        )

        router.on("/ping") { message ->
            received.complete(message)
        }

        val client = OscClient(bindAddress, scope)
        try {
            server.start()

            client.send(OscMessage("/ping"))

            val message = withTimeout(2_000) { received.await() }
            assertEquals("/ping", message.address)
        } finally {
            client.closeAndJoin()
            server.stop()
            scope.cancel()
        }
    }

    @Test
    fun `osc server calling start() after stop() should throw`(): Unit = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val server = OscServer(
            bindAddress = InetSocketAddress("127.0.0.1", getAvailablePort()),
            router = OscRouter(),
            dispatchMode = DispatchMode.ALL_MATCH,
            scope = scope
        )

        try {
            assertFailsWith<OscLifecycleException> {
                server.start()
                server.stop()
                server.start()
            }
        } finally {
            server.stop()
            scope.cancel()
        }
    }

    @Test
    fun `osc server should receive and dispatch multiple packets successfully`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val address = InetSocketAddress("127.0.0.1", getAvailablePort())
        var countA = 0
        var countB = 0
        val completedA = CompletableDeferred<Boolean>()
        val completedB = CompletableDeferred<Boolean>()
        val router = OscRouter().apply {
            on("/a") {
                countA++
                if (countA == 3) {
                    completedA.complete(true)
                }
            }
            on("/b") {
                countB++
                if (countB == 2) {
                    completedB.complete(true)
                }
            }
        }
        val server = OscServer(
            bindAddress = address,
            router = router,
            dispatchMode = DispatchMode.ALL_MATCH,
            scope = scope
        )
        val client = OscClient(
            targetAddress = address,
            scope = scope
        )

        try {
            server.start()

            client.send(OscMessage("/a"))
            client.send(OscMessage("/a"))
            client.send(OscMessage("/a"))

            client.send(OscMessage("/b"))
            client.send(OscMessage("/b"))

            assertTrue(withTimeout(2_000) { completedA.await() })
            assertTrue(withTimeout(2_000) { completedB.await() })

        } finally {
            client.closeAndJoin()
            server.stop()
            scope.cancel()
        }
    }

    @Test
    fun `osc server start called repeatedly while active should be harmless`() = runBlocking {
        val bindAddress = InetSocketAddress("127.0.0.1", getAvailablePort())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val router = OscRouter()
        val received = CompletableDeferred<OscMessage>()
        val server = OscServer(
            bindAddress = bindAddress,
            router = router,
            dispatchMode = DispatchMode.ALL_MATCH,
            scope = scope
        )
        val client = OscClient(bindAddress, scope)

        router.on("/ping") { message ->
            if (!received.isCompleted) {
                received.complete(message)
            }
        }

        try {
            server.start()
            server.start()

            client.send(OscMessage("/ping"))
            val message = withTimeout(2_000) { received.await() }
            assertEquals("/ping", message.address)
        } finally {
            client.closeAndJoin()
            server.stop()
            scope.cancel()
        }
    }

    @Test
    fun `server receive loop should not be corrupted after received invalid packet`() = runBlocking {
        val bindAddress = InetSocketAddress("127.0.0.1", getAvailablePort())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val router = OscRouter()
        val received = CompletableDeferred<OscMessage>()
        val server = OscServer(
            bindAddress = bindAddress,
            router = router,
            dispatchMode = DispatchMode.ALL_MATCH,
            scope = scope
        )

        router.on("/ping") { message ->
            received.complete(message)
        }

        val client = DatagramSocket()
        try {
            server.start()
            val writer = OscByteWriter()
            writer.writeString("/bad")
            writer.writeString("i")
            writer.writeInt32(1)

            val bad = writer.toByteArray()
            val datagram = DatagramPacket(
                bad,
                bad.size,
                bindAddress
            )

            client.send(datagram)

            val normal = OscMessage("/ping").encodeToByteArray()

            client.send(DatagramPacket(
                normal,
                normal.size,
                bindAddress
            ))

            val message = withTimeout(2_000) { received.await() }
            assertEquals("/ping", message.address)
        } finally {
            client.close()
            server.stop()
            scope.cancel()
        }
    }

    @Test
    fun `server stop should not fail when buffered packets still pending`() = runBlocking {
        val parentJob = Job()
        val scope = CoroutineScope(parentJob + Dispatchers.Default)
        val address = InetSocketAddress("127.0.0.1", getAvailablePort())
        val router = OscRouter().apply {
            on("/slow") {
                delay(5)
            }
        }
        val server = OscServer(
            bindAddress = address,
            router = router,
            dispatchMode = DispatchMode.ALL_MATCH,
            scope = scope
        )
        val client = OscClient(address, scope)

        try {
            server.start()
            repeat(80) {
                client.send(OscMessage("/slow"))
            }

            withTimeout(5_000) {
                server.stop()
            }

            assertTrue(parentJob.isActive)
        } finally {
            client.closeAndJoin()
            server.stop()
            scope.cancel()
        }
    }

}

