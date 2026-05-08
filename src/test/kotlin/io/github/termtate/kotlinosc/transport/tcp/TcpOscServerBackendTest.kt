package io.github.termtate.kotlinosc.transport.tcp

import io.github.termtate.kotlinosc.codec.OscPacketCodecImpl
import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.exception.OscCodecException
import io.github.termtate.kotlinosc.io.OscByteWriter
import io.github.termtate.kotlinosc.transport.OscTransportHook
import io.github.termtate.kotlinosc.transport.TcpPeer
import io.github.termtate.kotlinosc.transport.tcp.codec.OscTcpFramingStrategy
import io.github.termtate.kotlinosc.transport.tcp.codec.OscFrameCodec
import io.github.termtate.kotlinosc.transport.tcp.codec.OscFrameCodec.Companion.SLIP_END
import io.github.termtate.kotlinosc.type.OscMessage
import io.github.termtate.kotlinosc.type.OscPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TcpOscServerBackendTest {
    private val packetCodec = OscPacketCodecImpl(OscConfig.Codec.default)

    @Test
    fun `server should receive length-prefixed packet`(): Unit = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val address = localAddress()
        val backend = TcpOscServerBackend(
            scope = scope,
            bindAddress = address,
            framingStrategy = OscTcpFramingStrategy.LENGTH_PREFIXED
        )
        val client = Socket()

        try {
            backend.start()
            connect(client, address)

            client.writeFrame(OscMessage("/ping"), OscTcpFramingStrategy.LENGTH_PREFIXED)

            val received = withTimeout(2_000) { backend.receivedPackets.first() }
            val message = assertIs<OscMessage>(received.packet)
            assertEquals("/ping", message.address)
            assertIs<TcpPeer>(received.peer)
        } finally {
            client.close()
            backend.stop()
            scope.cancel()
        }
    }

    @Test
    fun `server should receive split length-prefixed frame`(): Unit = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val address = localAddress()
        val backend = TcpOscServerBackend(
            scope = scope,
            bindAddress = address,
            framingStrategy = OscTcpFramingStrategy.LENGTH_PREFIXED
        )
        val client = Socket()

        try {
            backend.start()
            connect(client, address)

            val frame = frameOf(OscMessage("/split"), OscTcpFramingStrategy.LENGTH_PREFIXED)
            write(client, frame.copyOfRange(0, 3))
            write(client, frame.copyOfRange(3, frame.size))

            val received = withTimeout(2_000) { backend.receivedPackets.first() }
            val message = assertIs<OscMessage>(received.packet)
            assertEquals("/split", message.address)
        } finally {
            client.close()
            backend.stop()
            scope.cancel()
        }
    }

    @Test
    fun `server should receive multiple length-prefixed frames in one write`(): Unit = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val address = localAddress()
        val backend = TcpOscServerBackend(
            scope = scope,
            bindAddress = address,
            framingStrategy = OscTcpFramingStrategy.LENGTH_PREFIXED
        )
        val client = Socket()

        try {
            backend.start()
            connect(client, address)

            write(
                client,
                frameOf(OscMessage("/a"), OscTcpFramingStrategy.LENGTH_PREFIXED) +
                    frameOf(OscMessage("/b"), OscTcpFramingStrategy.LENGTH_PREFIXED)
            )

            val received = withTimeout(2_000) { backend.receivedPackets.take(2).toList() }
            assertEquals(listOf("/a", "/b"), received.map { assertIs<OscMessage>(it.packet).address })
        } finally {
            client.close()
            backend.stop()
            scope.cancel()
        }
    }

    @Test
    fun `stop should close active idle clients and keep parent scope active`(): Unit = runBlocking {
        val parentJob = Job()
        val scope = CoroutineScope(parentJob + Dispatchers.Default)
        val address = localAddress()
        val backend = TcpOscServerBackend(
            scope = scope,
            bindAddress = address,
            framingStrategy = OscTcpFramingStrategy.LENGTH_PREFIXED
        )
        val client = Socket()

        try {
            backend.start()
            connect(client, address)

            withTimeout(2_000) { backend.stop() }

            assertTrue(parentJob.isActive)
        } finally {
            client.close()
            backend.stop()
            scope.cancel()
        }
    }

    @Test
    fun `decode error should not kill client read loop`(): Unit = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val address = localAddress()
        val decodeErrorCount = AtomicInteger()
        val backend = TcpOscServerBackend(
            scope = scope,
            bindAddress = address,
            transportHook = object : OscTransportHook {
                override fun onDecodeError(payload: ByteArray, error: OscCodecException) {
                    decodeErrorCount.incrementAndGet()
                }
            },
            framingStrategy = OscTcpFramingStrategy.LENGTH_PREFIXED
        )
        val client = Socket()

        try {
            backend.start()
            connect(client, address)

            write(client, OscFrameCodec(OscTcpFramingStrategy.LENGTH_PREFIXED).encodeFrame(badOscPayload()))
            client.writeFrame(OscMessage("/after-bad"), OscTcpFramingStrategy.LENGTH_PREFIXED)

            val received = withTimeout(2_000) { backend.receivedPackets.first() }
            val message = assertIs<OscMessage>(received.packet)
            assertEquals("/after-bad", message.address)
            assertEquals(1, decodeErrorCount.get())
        } finally {
            client.close()
            backend.stop()
            scope.cancel()
        }
    }

    @Test
    fun `framing error should report transport error and keep server accepting clients`(): Unit = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val address = localAddress()
        val transportErrorCount = AtomicInteger()
        val lastError = AtomicReference<Throwable?>()
        val backend = TcpOscServerBackend(
            scope = scope,
            bindAddress = address,
            transportHook = object : OscTransportHook {
                override fun onTransportError(error: Throwable) {
                    transportErrorCount.incrementAndGet()
                    lastError.set(error)
                }
            },
            framingStrategy = OscTcpFramingStrategy.SLIP
        )
        val badClient = Socket()
        val goodClient = Socket()

        try {
            backend.start()
            connect(badClient, address)
            write(badClient, byteArrayOf(SLIP_END, SLIP_END))
            badClient.close()

            connect(goodClient, address)
            goodClient.writeFrame(OscMessage("/after-frame-error"), OscTcpFramingStrategy.SLIP)

            val received = withTimeout(2_000) { backend.receivedPackets.first() }
            val message = assertIs<OscMessage>(received.packet)
            assertEquals("/after-frame-error", message.address)
            assertEquals(1, transportErrorCount.get())
            assertEquals("tcp framing failed", lastError.get()?.message)
        } finally {
            badClient.close()
            goodClient.close()
            backend.stop()
            scope.cancel()
        }
    }

    @Test
    fun `stop should not hang after many short-lived clients`(): Unit = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val address = localAddress()
        val backend = TcpOscServerBackend(
            scope = scope,
            bindAddress = address,
            framingStrategy = OscTcpFramingStrategy.LENGTH_PREFIXED
        )

        try {
            backend.start()

            repeat(32) {
                Socket().use { client ->
                    connect(client, address)
                }
            }

            withTimeout(2_000) { backend.stop() }
        } finally {
            backend.stop()
            scope.cancel()
        }
    }

    private suspend fun connect(socket: Socket, address: InetSocketAddress) {
        withContext(Dispatchers.IO) {
            socket.connect(address, 2_000)
        }
    }

    private suspend fun Socket.writeFrame(packet: OscPacket, framingStrategy: OscTcpFramingStrategy) {
        write(this, frameOf(packet, framingStrategy))
    }

    private suspend fun write(socket: Socket, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            socket.getOutputStream().write(bytes)
            socket.getOutputStream().flush()
        }
    }

    private fun frameOf(packet: OscPacket, framingStrategy: OscTcpFramingStrategy): ByteArray {
        val packetBytes = packetCodec.encode(packet, OscByteWriter())
        return OscFrameCodec(framingStrategy).encodeFrame(packetBytes)
    }

    private fun badOscPayload(): ByteArray {
        return OscByteWriter().apply {
            writeString("/bad")
            writeString("i")
            writeInt32(1)
        }.toByteArray()
    }

    private fun localAddress(): InetSocketAddress {
        return InetSocketAddress("127.0.0.1", ServerSocket(0).use { it.localPort })
    }
}
