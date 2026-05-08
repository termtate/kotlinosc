package io.github.termtate.kotlinosc.transport.udp

import io.github.termtate.kotlinosc.codec.OscPacketCodecImpl
import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.exception.OscCodecException
import io.github.termtate.kotlinosc.exception.OscTransportException
import io.github.termtate.kotlinosc.io.OscByteReader
import io.github.termtate.kotlinosc.io.OscByteWriter
import io.github.termtate.kotlinosc.transport.DEFAULT_CHANNEL_CAPACITY
import io.github.termtate.kotlinosc.transport.OscPeer
import io.github.termtate.kotlinosc.transport.OscServerBackend
import io.github.termtate.kotlinosc.transport.OscTransportHook
import io.github.termtate.kotlinosc.transport.ReceivedPacket
import io.github.termtate.kotlinosc.transport.UdpPeer
import io.github.termtate.kotlinosc.type.OscPacket
import io.github.termtate.kotlinosc.util.OscLogger
import io.github.termtate.kotlinosc.util.logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.net.SocketException
import kotlin.time.Clock

internal class UdpOscServerBackend(
    private val scope: CoroutineScope,
    bindAddress: SocketAddress,
    receiveChannelCapacity: Int = DEFAULT_CHANNEL_CAPACITY,
    private val transportHook: OscTransportHook = OscTransportHook.Companion.NOOP,
    codecConfig: OscConfig.Codec = OscConfig.Codec.default
) : OscServerBackend, OscLogger {
    override val logTag: String
        get() = "UdpOscServerBackend"

    private val socket = DatagramSocket(bindAddress)
    private val codec = OscPacketCodecImpl(codecConfig)
    private var recvJob: Job? = null

    private val bufferedPackets = Channel<ReceivedPacket>(
        capacity = receiveChannelCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val receivedPackets: Flow<ReceivedPacket>
        get() = bufferedPackets.consumeAsFlow()

    override suspend fun start() {
        if (recvJob != null) {
            return
        }
        recvJob = scope.launch(Dispatchers.IO) { receiveLoop() }
    }

    override suspend fun stop() {
        socket.close()
        recvJob?.cancelAndJoin()
        recvJob = null
    }

    private suspend fun receiveLoop() {
        val buffer = ByteArray(MAX_PACKET_SIZE)

        try {
            while (currentCoroutineContext().isActive) {
                val datagram = DatagramPacket(buffer, buffer.size)
                socket.receive(datagram)
                val payload = datagram.data.copyOfRange(0, datagram.length)

                try {
                    val packet = codec.decode(OscByteReader(payload))
                    bufferedPackets.send(
                        ReceivedPacket(
                            packet = packet,
                            peer = UdpPeer(datagram.socketAddress),
                            receivedAt = Clock.System.now()
                        )
                    )
                } catch (e: OscCodecException) {
                    transportHook.onDecodeError(payload, e)
                    logger.error(e) { "payload decode failed: ${e.message}" }
                }
            }
        } catch (e: SocketException) {
            if (!socket.isClosed) {
                val wrapped = OscTransportException("udp receive failed", e)
                transportHook.onTransportError(wrapped)
                throw wrapped
            }
            // socket is closed, stop the loop
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val wrapped = t as? OscTransportException ?: OscTransportException("udp receive failed", t)
            transportHook.onTransportError(wrapped)
            throw wrapped
        }
    }

    private companion object {
        private const val MAX_PACKET_SIZE = 65507  // UDP max payload
    }
}
