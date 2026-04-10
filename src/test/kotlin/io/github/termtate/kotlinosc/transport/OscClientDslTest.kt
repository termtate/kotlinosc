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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OscClientDslTest {
    private fun getAvailablePort(): Int = DatagramSocket(0).use { it.localPort }

    @Test
    fun `oscClient socketAddress overload should send packet`() = runBlocking {
        val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val address = InetSocketAddress("127.0.0.1", getAvailablePort())
        val received = CompletableDeferred<OscMessage>()

        val server = oscServer(address) {
            scope = serverScope
            route {
                on("/ping") { message -> received.complete(message) }
            }
        }
        val client = oscClient(address) {
            scope = clientScope
        }

        try {
            server.start()
            client.send(OscMessage("/ping"))
            assertEquals("/ping", withTimeout(2_000) { received.await() }.address)
        } finally {
            client.closeAndJoin()
            server.stop()
            clientScope.cancel()
            serverScope.cancel()
        }
    }

    @Test
    fun `oscClient ip-port overload should send packet`() = runBlocking {
        val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val port = getAvailablePort()
        val address = InetSocketAddress("127.0.0.1", port)
        val received = CompletableDeferred<OscMessage>()

        val server = oscServer(address) {
            scope = serverScope
            route {
                on("/ping") { message -> received.complete(message) }
            }
        }
        val client = oscClient("127.0.0.1", port) {
            scope = clientScope
        }

        try {
            server.start()
            client.send(OscMessage("/ping"))
            assertEquals("/ping", withTimeout(2_000) { received.await() }.address)
        } finally {
            client.closeAndJoin()
            server.stop()
            clientScope.cancel()
            serverScope.cancel()
        }
    }

    @Test
    fun `oscClient should apply transportHook from dsl`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var transportErrorCount = 0
        val client = oscClient(InetSocketAddress("127.0.0.1", getAvailablePort())) {
            this.scope = scope
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
            scope.cancel()
        }
    }
}


