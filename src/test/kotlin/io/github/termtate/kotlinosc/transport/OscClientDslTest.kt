package io.github.termtate.kotlinosc.transport

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import io.github.termtate.kotlinosc.exception.OscTransportException
import io.github.termtate.kotlinosc.transport.dsl.oscClient
import io.github.termtate.kotlinosc.transport.dsl.oscServer
import io.github.termtate.kotlinosc.type.OscMessage
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OscClientDslTest {
    private fun getAvailablePort(): Int = DatagramSocket(0).use { it.localPort }

    private fun getAvailableTcpPort(): Int = ServerSocket(0).use { it.localPort }

    @Test
    fun `oscClient socketAddress overload should send packet over default udp`() = runBlocking {
        val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val address = InetSocketAddress("127.0.0.1", getAvailablePort())
        val received = CompletableDeferred<OscMessage>()

        val server = oscServer(address) {
            scope = serverScope
            route {
                on("/ping") { message -> received.complete(message) }
            }
        }
        val client = oscClient(address)

        try {
            server.start()
            client.send(OscMessage("/ping"))
            assertEquals("/ping", withTimeout(2_000) { received.await() }.address)
        } finally {
            client.closeAndJoin()
            server.stop()
            serverScope.cancel()
        }
    }

    @Test
    fun `oscClient ip-port overload should send packet over default udp`() = runBlocking {
        val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val port = getAvailablePort()
        val address = InetSocketAddress("127.0.0.1", port)
        val received = CompletableDeferred<OscMessage>()

        val server = oscServer(address) {
            scope = serverScope
            route {
                on("/ping") { message -> received.complete(message) }
            }
        }
        val client = oscClient("127.0.0.1", port)

        try {
            server.start()
            client.send(OscMessage("/ping"))
            assertEquals("/ping", withTimeout(2_000) { received.await() }.address)
        } finally {
            client.closeAndJoin()
            server.stop()
            serverScope.cancel()
        }
    }

    @Test
    fun `oscClient protocol tcp should send packet with default framing`() = runBlocking {
        val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val address = InetSocketAddress("127.0.0.1", getAvailableTcpPort())
        val received = CompletableDeferred<OscMessage>()

        val server = oscServer(address) {
            scope = serverScope
            protocol { tcp() }
            route {
                on("/tcp/client-dsl") { message -> received.complete(message) }
            }
        }
        val client = oscClient(address) {
            protocol { tcp() }
        }

        try {
            server.start()
            client.send(OscMessage("/tcp/client-dsl"))

            val message = withTimeout(2_000) { received.await() }
            assertEquals("/tcp/client-dsl", message.address)
        } finally {
            client.closeAndJoin()
            server.stop()
            serverScope.cancel()
        }
    }

    @Test
    fun `oscClient should apply transportHook from dsl`() = runBlocking {
        var transportErrorCount = 0
        val client = oscClient(InetSocketAddress("127.0.0.1", getAvailablePort())) {
            transportHook = object : OscTransportHook {
                override fun onTransportError(error: Throwable) {
                    transportErrorCount++
                }
            }
        }

        try {
            client.closeAndJoin()
            assertFailsWith<OscTransportException> {
                client.send(OscMessage("/x"))
            }
            assertEquals(1, transportErrorCount)
        } finally {
            client.closeAndJoin()
        }
    }
}


