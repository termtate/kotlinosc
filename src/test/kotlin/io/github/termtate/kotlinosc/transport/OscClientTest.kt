package io.github.termtate.kotlinosc.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import io.github.termtate.kotlinosc.exception.OscTransportException
import io.github.termtate.kotlinosc.type.OscMessage
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OscClientTest {
    private fun getAvailablePort(): Int = DatagramSocket(0).use { it.localPort }

    @Test
    fun `closeAndJoin should not cancel external scope`() = runBlocking {
        val parentJob = Job()
        val scope = CoroutineScope(parentJob + Dispatchers.Default)
        val client = OscClient(InetSocketAddress("127.0.0.1", getAvailablePort()), scope)

        try {
            client.closeAndJoin()
            assertTrue(parentJob.isActive)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `closeAsync should work even if external scope is already cancelled`(): Unit = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val client = OscClient(InetSocketAddress("127.0.0.1", getAvailablePort()), scope)

        scope.cancel()
        client.closeAsync().await()

        assertFailsWith<OscTransportException> {
            client.send(OscMessage("/x"))
        }
    }

    @Test
    fun `closeAsync should be idempotent under concurrent calls`(): Unit = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val client = OscClient(InetSocketAddress("127.0.0.1", getAvailablePort()), scope)

        try {
            val deferreds = (1..32).map {
                async(Dispatchers.Default) { client.closeAsync() }
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
    fun `close should trigger async shutdown and eventually close client`(): Unit = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val client = OscClient(InetSocketAddress("127.0.0.1", getAvailablePort()), scope)

        try {
            client.closeAsync().await()

            assertFailsWith<OscTransportException> {
                client.send(OscMessage("/x"))
            }
        } finally {
            scope.cancel()
        }
    }
}

