package io.github.termtate.kotlinosc.transport.udp

import io.github.termtate.kotlinosc.codec.OscPacketCodecImpl
import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.exception.OscTransportException
import io.github.termtate.kotlinosc.io.OscByteWriter
import io.github.termtate.kotlinosc.transport.OscClientBackend
import io.github.termtate.kotlinosc.transport.OscTransportHook
import io.github.termtate.kotlinosc.transport.UdpPeer
import io.github.termtate.kotlinosc.type.OscPacket
import io.github.termtate.kotlinosc.util.OscLogger
import io.github.termtate.kotlinosc.util.logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress

internal class UdpOscClientBackend(
    targetAddress: SocketAddress,
    private val transportHook: OscTransportHook = OscTransportHook.Companion.NOOP,
    codecConfig: OscConfig.Codec = OscConfig.Codec.default
) : OscClientBackend, OscLogger {
    override val logTag: String
        get() = "UdpOscClientBackend"

    private val targetPeer = UdpPeer(targetAddress)
    private val socket = DatagramSocket()
    private val codec = OscPacketCodecImpl(codecConfig)

    override suspend fun send(packet: OscPacket) {
        val bytes = codec.encode(packet, OscByteWriter())
        try {
            withContext(Dispatchers.IO) {
                socket.send(DatagramPacket(bytes, bytes.size, targetPeer.address))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val wrapped = t as? OscTransportException ?: OscTransportException("udp send failed", t)
            transportHook.onTransportError(wrapped)
            logger.error(wrapped) { wrapped.message ?: "udp send failed" }
            throw wrapped
        }
    }

    override suspend fun close() {
        socket.close()
    }
}
