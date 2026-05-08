package io.github.termtate.kotlinosc.transport

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.route.DispatchMode
import io.github.termtate.kotlinosc.transport.dsl.OscServerOptionsBuilder
import io.github.termtate.kotlinosc.transport.dsl.oscServer
import io.github.termtate.kotlinosc.transport.tcp.codec.OscTcpFramingStrategy
import io.github.termtate.kotlinosc.type.OscMessage
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OscServerDslTest {
    private fun getAvailablePort(): Int = DatagramSocket(0).use { it.localPort }

    private fun getAvailableTcpPort(): Int = ServerSocket(0).use { it.localPort }

    @Test
    fun `dsl defaults should match osc server options defaults`() {
        val bindAddress = InetSocketAddress("127.0.0.1", getAvailablePort())

        val options = OscServerOptionsBuilder().build(bindAddress)

        assertEquals(bindAddress, options.bindAddress)
        assertEquals(DispatchMode.ALL_MATCH, options.dispatchMode)
        assertTrue(options.continueOnDispatchError)
        assertEquals(1, options.maxConcurrentDispatches)
        assertEquals(Dispatchers.IO, options.dispatchDispatcher)
        assertEquals(OscConfig.Codec.default, options.codecConfig)
        assertEquals(OscConfig.AddressPattern.default, options.addressPatternConfig)
        assertEquals(OscTransportProtocol.Udp, options.protocol)
        assertNotNull(options.router)
    }

    @Test
    fun `dsl should map dispatch config correctly`() {
        val bindAddress = InetSocketAddress("127.0.0.1", getAvailablePort())

        val options = OscServerOptionsBuilder().apply {
            dispatch {
                dispatchMode = DispatchMode.FIRST_MATCH
                continueOnDispatchError = false
                maxConcurrentDispatches = 4
                dispatchDispatcher = Dispatchers.Default
            }
        }.build(bindAddress)

        assertEquals(DispatchMode.FIRST_MATCH, options.dispatchMode)
        assertEquals(false, options.continueOnDispatchError)
        assertEquals(4, options.maxConcurrentDispatches)
        assertEquals(Dispatchers.Default, options.dispatchDispatcher)
    }

    @Test
    fun `dsl should map tcp protocol default framing correctly`() {
        val bindAddress = InetSocketAddress("127.0.0.1", getAvailableTcpPort())

        val options = OscServerOptionsBuilder().apply {
            protocol { tcp() }
        }.build(bindAddress)

        assertEquals(OscTransportProtocol.Tcp(), options.protocol)
    }

    @Test
    fun `dsl should map tcp protocol slip framing correctly`() {
        val bindAddress = InetSocketAddress("127.0.0.1", getAvailableTcpPort())

        val options = OscServerOptionsBuilder().apply {
            protocol {
                tcp {
                    framingStrategy = OscTcpFramingStrategy.SLIP
                }
            }
        }.build(bindAddress)

        assertEquals(OscTransportProtocol.Tcp(OscTcpFramingStrategy.SLIP), options.protocol)
    }

    @Test
    fun `dsl route block should register handlers over default udp`() = runBlocking {
        val bindAddress = InetSocketAddress("127.0.0.1", getAvailablePort())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val received = CompletableDeferred<OscMessage>()

        val server = oscServer(bindAddress) {
            this.scope = scope
            route {
                on("/ping") { message ->
                    received.complete(message)
                }
            }
        }
        val client = OscClient(bindAddress)

        try {
            server.start()
            client.send(OscMessage("/ping"))

            val msg = withTimeout(2_000) { received.await() }
            assertEquals("/ping", msg.address)
        } finally {
            client.closeAndJoin()
            server.stop()
            scope.cancel()
        }
    }

    @Test
    fun `dsl route block should register handlers over tcp default framing`() = runBlocking {
        val bindAddress = InetSocketAddress("127.0.0.1", getAvailableTcpPort())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val received = CompletableDeferred<OscMessage>()

        val server = oscServer(bindAddress) {
            this.scope = scope
            protocol { tcp() }
            route {
                on("/tcp/server-dsl") { message ->
                    received.complete(message)
                }
            }
        }
        val client = OscClient(
            targetAddress = bindAddress,
            protocol = OscTransportProtocol.Tcp()
        )

        try {
            server.start()
            client.send(OscMessage("/tcp/server-dsl"))

            val msg = withTimeout(2_000) { received.await() }
            assertEquals("/tcp/server-dsl", msg.address)
        } finally {
            client.closeAndJoin()
            server.stop()
            scope.cancel()
        }
    }

    @Test
    fun `dsl socketAddress and ip-port overloads should both work over default udp`() = runBlocking {
        suspend fun runRoundTrip(server: OscServer, target: InetSocketAddress) {
            val received = CompletableDeferred<OscMessage>()
            server.router.on("/ping") { message ->
                if (!received.isCompleted) {
                    received.complete(message)
                }
            }
            val client = OscClient(target)
            try {
                server.start()
                client.send(OscMessage("/ping"))
                val msg = withTimeout(2_000) { received.await() }
                assertEquals("/ping", msg.address)
            } finally {
                client.closeAndJoin()
                server.stop()
            }
        }

        val port1 = getAvailablePort()
        val scope1 = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val address1 = InetSocketAddress("127.0.0.1", port1)
        val server1 = oscServer(address1) {
            this.scope = scope1
        }

        val port2 = getAvailablePort()
        val scope2 = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val server2 = oscServer("127.0.0.1", port2) {
            this.scope = scope2
        }

        try {
            runRoundTrip(server1, address1)
            runRoundTrip(server2, InetSocketAddress("127.0.0.1", port2))
        } finally {
            scope1.cancel()
            scope2.cancel()
        }
    }
}

