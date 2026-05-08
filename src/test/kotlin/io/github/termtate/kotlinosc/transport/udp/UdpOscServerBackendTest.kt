package io.github.termtate.kotlinosc.transport.udp

import io.github.termtate.kotlinosc.codec.encodeToByteArray
import io.github.termtate.kotlinosc.exception.OscCodecException
import io.github.termtate.kotlinosc.type.OscMessage
import io.github.termtate.kotlinosc.io.OscByteWriter
import io.github.termtate.kotlinosc.transport.OscTransportHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UdpOscServerBackendTest {
    private fun getAvailablePort() = DatagramSocket(0).use { it.localPort }

    @Test
    fun `stop should exit receive loop without cancelling parent scope`() = runBlocking {
        val parentJob = Job()
        val scope = CoroutineScope(parentJob + Dispatchers.Default)
        val backend = UdpOscServerBackend(
            scope = scope,
            bindAddress = InetSocketAddress("127.0.0.1", getAvailablePort())
        )

        try {
            backend.start()
            withTimeout(2_000) {
                backend.stop()
            }
            assertTrue(parentJob.isActive)
        } finally {
            backend.stop()
            scope.cancel()
        }
    }

    @Test
    fun `channel overflow should keep latest packet when capacity is one`() = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val target = InetSocketAddress("127.0.0.1", getAvailablePort())
        val backend = UdpOscServerBackend(
            scope = scope,
            bindAddress = target,
            receiveChannelCapacity = 1
        )
        val client = DatagramSocket()

        try {
            backend.start()

            repeat(20) { i ->
                val payload = OscMessage("/m$i").encodeToByteArray()
                client.send(DatagramPacket(payload, payload.size, target))
            }

            // Let receive loop drain socket and apply overflow policy.
            delay(120)

            val first = withTimeout(2_000) { backend.receivedPackets.first() }
            assertIs<OscMessage>(first.packet)
            assertEquals("/m19", first.packet.address)
        } finally {
            client.close()
            backend.stop()
            scope.cancel()
        }
    }

    @Test
    fun `decode error hook should be called when bad packet received`() = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val target = InetSocketAddress("127.0.0.1", getAvailablePort())
        var decodeErrorCount = 0
        val backend = UdpOscServerBackend(
            scope = scope,
            bindAddress = target,
            transportHook = object : OscTransportHook {
                override fun onDecodeError(payload: ByteArray, error: OscCodecException) {
                    decodeErrorCount++
                }
            }
        )
        val client = DatagramSocket()

        try {
            backend.start()
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
            backend.stop()
            scope.cancel()
        }
    }
}
