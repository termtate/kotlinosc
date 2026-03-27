package org.example.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.example.type.OscMessage
import org.example.codec.encodeToByteArray
import org.example.exception.OscCodecException
import org.example.exception.OscTransportException
import org.example.io.OscByteWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertIs

class UdpOscTransportTest {
    private fun getAvailablePort() = DatagramSocket(0).use { it.localPort }

    @Test
    fun `stop should exit receive loop without cancelling parent scope`() = runBlocking {
        val parentJob = Job()
        val scope = CoroutineScope(parentJob + Dispatchers.Default)
        val transport = UdpOscTransport(
            scope = scope,
            socket = DatagramSocket(InetSocketAddress("127.0.0.1", getAvailablePort()))
        )

        try {
            transport.start()
            withTimeout(2_000) {
                transport.stop()
            }
            assertTrue(parentJob.isActive)
        } finally {
            transport.stop()
            scope.cancel()
        }
    }

    @Test
    fun `channel overflow should keep latest packet when capacity is one`() = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val target = InetSocketAddress("127.0.0.1", getAvailablePort())
        val transport = UdpOscTransport(
            scope = scope,
            socket = DatagramSocket(target),
            receiveChannelCapacity = 1
        )
        val client = DatagramSocket()

        try {
            transport.start()

            repeat(20) { i ->
                val payload = OscMessage("/m$i").encodeToByteArray()
                client.send(DatagramPacket(payload, payload.size, target))
            }

            // Let receive loop drain socket and apply overflow policy.
            delay(120)

            val first = withTimeout(2_000) { transport.receivedPackets.first() }
            assertIs<OscMessage>(first.packet)
            assertEquals("/m19", first.packet.address)
        } finally {
            client.close()
            transport.stop()
            scope.cancel()
        }
    }

    @Test
    fun `decode error hook should be called when bad packet received`() = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val target = InetSocketAddress("127.0.0.1", getAvailablePort())
        var decodeErrorCount = 0
        val transport = UdpOscTransport(
            scope = scope,
            socket = DatagramSocket(target),
            hook = object : OscTransportHook {
                override fun onDecodeError(payload: ByteArray, error: OscCodecException) {
                    decodeErrorCount++
                }
            }
        )
        val client = DatagramSocket()

        try {
            transport.start()
            // Bad packet: typetag string does not start with ','.
            val writer = OscByteWriter()
            writer.writeString("/bad")
            writer.writeString("i")
            writer.writeInt32(1)
            val bad = writer.toByteArray()
            client.send(DatagramPacket(bad, bad.size, target))

            withTimeout(2_000) {
                while (decodeErrorCount == 0) {
                    delay(10)
                }
            }
            assertEquals(1, decodeErrorCount)
        } finally {
            client.close()
            transport.stop()
            scope.cancel()
        }
    }

    @Test
    fun `transport error hook should be called when send fails`() = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        var transportErrorCount = 0
        val transport = UdpOscTransport(
            scope = scope,
            socket = DatagramSocket(),
            hook = object : OscTransportHook {
                override fun onTransportError(error: Throwable) {
                    transportErrorCount++
                }
            }
        )
        transport.stop()

        try {
            assertFailsWith<OscTransportException> {
                transport.send(OscMessage("/x"), InetSocketAddress("127.0.0.1", getAvailablePort()))
            }
            assertEquals(1, transportErrorCount)
        } finally {
            scope.cancel()
        }
    }
}
