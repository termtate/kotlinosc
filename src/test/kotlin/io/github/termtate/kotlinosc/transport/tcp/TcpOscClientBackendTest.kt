package io.github.termtate.kotlinosc.transport.tcp

import io.github.termtate.kotlinosc.codec.OscPacketCodecImpl
import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.exception.OscTransportException
import io.github.termtate.kotlinosc.io.OscByteReader
import io.github.termtate.kotlinosc.transport.OscTransportHook
import io.github.termtate.kotlinosc.transport.tcp.codec.OscTcpFramingStrategy
import io.github.termtate.kotlinosc.transport.tcp.codec.OscFrameCodec
import io.github.termtate.kotlinosc.type.OscMessage
import io.github.termtate.kotlinosc.type.OscPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class TcpOscClientBackendTest {
    private val packetCodec = OscPacketCodecImpl(OscConfig.Codec.default)

    @Test
    fun `send should write one length-prefixed framed packet`() = runBlocking {
        val serverSocket = ServerSocket(0)
        val client = TcpOscClientBackend(
            targetAddress = targetAddress(serverSocket),
            framingStrategy = OscTcpFramingStrategy.LENGTH_PREFIXED
        )
        val received = async(Dispatchers.IO) {
            readPackets(serverSocket, OscTcpFramingStrategy.LENGTH_PREFIXED, count = 1)
        }

        try {
            client.send(OscMessage("/ping"))

            val packet = withTimeout(2_000) { received.await().single() }
            val message = assertIs<OscMessage>(packet)
            assertEquals("/ping", message.address)
        } finally {
            client.close()
            serverSocket.close()
        }
    }

    @Test
    fun `send should write one slip framed packet`() = runBlocking {
        val serverSocket = ServerSocket(0)
        val client = TcpOscClientBackend(
            targetAddress = targetAddress(serverSocket),
            framingStrategy = OscTcpFramingStrategy.SLIP
        )
        val received = async(Dispatchers.IO) {
            readPackets(serverSocket, OscTcpFramingStrategy.SLIP, count = 1)
        }

        try {
            client.send(OscMessage("/slip"))

            val packet = withTimeout(2_000) { received.await().single() }
            val message = assertIs<OscMessage>(packet)
            assertEquals("/slip", message.address)
        } finally {
            client.close()
            serverSocket.close()
        }
    }

    @Test
    fun `transport error hook should be called when send after close fails`() = runBlocking {
        var transportErrorCount = 0
        var lastError: Throwable? = null
        val client = TcpOscClientBackend(
            targetAddress = InetSocketAddress("127.0.0.1", getAvailablePort()),
            transportHook = object : OscTransportHook {
                override fun onTransportError(error: Throwable) {
                    transportErrorCount++
                    lastError = error
                }
            },
            framingStrategy = OscTcpFramingStrategy.LENGTH_PREFIXED
        )

        client.close()

        assertFailsWith<OscTransportException> {
            client.send(OscMessage("/closed"))
        }
        assertEquals(1, transportErrorCount)
        assertIs<OscTransportException>(lastError)
        assertEquals("cannot send message after client is closed", lastError?.message)
    }

    @Test
    fun `transport error hook should be called when lazy connect fails`() = runBlocking {
        var transportErrorCount = 0
        var lastError: Throwable? = null
        val client = TcpOscClientBackend(
            targetAddress = InetSocketAddress("127.0.0.1", getAvailablePort()),
            transportHook = object : OscTransportHook {
                override fun onTransportError(error: Throwable) {
                    transportErrorCount++
                    lastError = error
                }
            },
            framingStrategy = OscTcpFramingStrategy.LENGTH_PREFIXED
        )

        try {
            assertFailsWith<OscTransportException> {
                client.send(OscMessage("/connect-fail"))
            }
            assertEquals(1, transportErrorCount)
            assertIs<OscTransportException>(lastError)
            assertEquals("tcp client connect failed", lastError?.message)
        } finally {
            client.close()
        }
    }

    @Test
    fun `concurrent sends should write complete length-prefixed frames`() = runBlocking {
        val serverSocket = ServerSocket(0)
        val count = 32
        val client = TcpOscClientBackend(
            targetAddress = targetAddress(serverSocket),
            framingStrategy = OscTcpFramingStrategy.LENGTH_PREFIXED
        )
        val received = async(Dispatchers.IO) {
            readPackets(serverSocket, OscTcpFramingStrategy.LENGTH_PREFIXED, count)
        }

        try {
            (0 until count).map { i ->
                async(Dispatchers.Default) {
                    client.send(OscMessage("/m$i"))
                }
            }.awaitAll()

            val addresses = withTimeout(2_000) {
                received.await()
                    .map { assertIs<OscMessage>(it).address }
                    .toSet()
            }
            assertEquals((0 until count).map { "/m$it" }.toSet(), addresses)
        } finally {
            client.close()
            serverSocket.close()
        }
    }

    private suspend fun readPackets(
        serverSocket: ServerSocket,
        framingStrategy: OscTcpFramingStrategy,
        count: Int
    ): List<OscPacket> = withContext(Dispatchers.IO) {
        val decoder = OscFrameCodec(framingStrategy).decoder()
        serverSocket.accept().use { socket ->
            val input = socket.getInputStream()
            val buffer = ByteArray(1024)
            val frames = mutableListOf<ByteArray>()

            while (frames.size < count) {
                val n = input.read(buffer)
                if (n == -1) {
                    error("client closed before $count frames were received")
                }
                frames += decoder.feed(buffer, n)
            }

            frames.take(count).map { frame ->
                packetCodec.decode(OscByteReader(frame))
            }
        }
    }

    private fun targetAddress(serverSocket: ServerSocket): InetSocketAddress {
        return InetSocketAddress("127.0.0.1", serverSocket.localPort)
    }

    private fun getAvailablePort(): Int = ServerSocket(0).use { it.localPort }
}
