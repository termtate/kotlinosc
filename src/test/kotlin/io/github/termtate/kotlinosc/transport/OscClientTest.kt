package io.github.termtate.kotlinosc.transport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    fun `closeAndJoin should close client`(): Unit = runBlocking {
        val client = OscClient(InetSocketAddress("127.0.0.1", getAvailablePort()))

        client.closeAndJoin()

        assertFailsWith<OscTransportException> {
            client.send(OscMessage("/x"))
        }
    }

    @Test
    fun `closeAsync should close client`(): Unit = runBlocking {
        val client = OscClient(InetSocketAddress("127.0.0.1", getAvailablePort()))

        client.closeAsync().await()

        assertFailsWith<OscTransportException> {
            client.send(OscMessage("/x"))
        }
    }

    @Test
    fun `closeAsync should be idempotent under concurrent calls`(): Unit = runBlocking {
        val client = OscClient(InetSocketAddress("127.0.0.1", getAvailablePort()))

        val deferreds = (1..32).map {
            async(Dispatchers.Default) { client.closeAsync() }
        }.awaitAll()

        val first = deferreds.first()
        assertTrue(deferreds.all { it === first })
        first.await()
        assertTrue(first.isCompleted)
    }

    @Test
    fun `close should trigger async shutdown and eventually close client`(): Unit = runBlocking {
        val client = OscClient(InetSocketAddress("127.0.0.1", getAvailablePort()))

        client.close()
        client.closeAndJoin()

        assertFailsWith<OscTransportException> {
            client.send(OscMessage("/x"))
        }
    }
}

