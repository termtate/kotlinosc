package io.github.termtate.kotlinosc.transport.udp

import io.github.termtate.kotlinosc.exception.OscTransportException
import io.github.termtate.kotlinosc.transport.OscTransportHook
import io.github.termtate.kotlinosc.type.OscMessage
import kotlinx.coroutines.runBlocking
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UdpOscClientBackendTest {
    private fun getAvailablePort() = DatagramSocket(0).use { it.localPort }

    @Test
    fun `transport error hook should be called when send fails`() = runBlocking {
        var transportErrorCount = 0
        val client = UdpOscClientBackend(
            targetAddress = InetSocketAddress("127.0.0.1", getAvailablePort()),
            transportHook = object : OscTransportHook {
                override fun onTransportError(error: Throwable) {
                    transportErrorCount++
                }
            }
        )
        client.close()

        try {
            assertFailsWith<OscTransportException> {
                client.send(OscMessage("/x"))
            }
            assertEquals(1, transportErrorCount)
        } finally {
            client.close()
        }
    }
}
