package io.github.termtate.kotlinosc.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import io.github.termtate.kotlinosc.exception.OscLifecycleException
import io.github.termtate.kotlinosc.route.OscRouter
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OscServerLifecycleTest {
    private fun getAvailablePort(): Int = DatagramSocket(0).use { it.localPort }

    @Test
    fun `startAsync should start server and return completed deferred`(): Unit = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val server = OscServer(
            OscServerOptions(
                bindAddress = InetSocketAddress("127.0.0.1", getAvailablePort()),
                router = OscRouter(),
                scope = scope
            )
        )

        try {
            val startDeferred = server.startAsync()
            startDeferred.await()
            assertTrue(startDeferred.isCompleted)
            server.stop()
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `stopAndJoin should not cancel external scope`() = runBlocking {
        val parentJob = Job()
        val scope = CoroutineScope(parentJob + Dispatchers.Default)
        val server = OscServer(
            OscServerOptions(
                bindAddress = InetSocketAddress("127.0.0.1", getAvailablePort()),
                router = OscRouter(),
                scope = scope
            )
        )

        try {
            server.stop()
            assertTrue(parentJob.isActive)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `stopAsync should be idempotent under concurrent calls`(): Unit = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val server = OscServer(
            OscServerOptions(
                bindAddress = InetSocketAddress("127.0.0.1", getAvailablePort()),
                router = OscRouter(),
                scope = scope
            )
        )

        try {
            server.start()
            val deferreds = (1..32).map {
                async(Dispatchers.Default) { server.stopAsync() }
            }.awaitAll()

            val first = deferreds.first()
            assertTrue(deferreds.all { it === first })
            first.await()
            assertTrue(first.isCompleted)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `start should throw after server has been stopped`(): Unit = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val server = OscServer(
            OscServerOptions(
                bindAddress = InetSocketAddress("127.0.0.1", getAvailablePort()),
                router = OscRouter(),
                scope = scope
            )
        )

        try {
            server.start()
            server.stop()
            assertFailsWith<OscLifecycleException> {
                server.start()
            }
        } finally {
            scope.cancel()
        }
    }
}

