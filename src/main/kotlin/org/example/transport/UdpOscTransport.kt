package org.example.transport

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
import kotlinx.coroutines.withContext
import org.example.type.OscPacket
import org.example.codec.decodeFromByteArray
import org.example.codec.encodeToByteArray
import org.example.exception.OscCodecException
import org.example.exception.OscTransportException
import org.example.util.OscLogger
import org.example.util.logger
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.net.SocketException
import kotlin.time.Clock

internal class UdpOscTransport(
    private val scope: CoroutineScope,
    private val socket: DatagramSocket,
    receiveChannelCapacity: Int = DEFAULT_CHANNEL_CAPACITY,
    private val hook: OscTransportHook = OscTransportHook.NOOP
) : OscTransport, OscLogger {
    override val logTag: String
        get() = "UdpOscTransport"

    private var recvJob: Job? = null

    private val _receivedPackets = Channel<ReceivedPacket>(
        capacity = receiveChannelCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val receivedPackets: Flow<ReceivedPacket> get() = _receivedPackets.consumeAsFlow()

    override fun start() {
        if (recvJob != null) {
            return
        }
        recvJob = scope.launch(Dispatchers.IO) { receiveLoop() }
    }

    suspend fun receiveLoop() {
        val buf = ByteArray(MAX_PACKET_SIZE)

        try {
            while (currentCoroutineContext().isActive) {
                val datagram = DatagramPacket(buf, buf.size)
                socket.receive(datagram)
                val payload = datagram.data.copyOfRange(0, datagram.length)
                try {
                    val packet = OscPacket.decodeFromByteArray(payload)
                    _receivedPackets.send(ReceivedPacket(
                        packet,
                        datagram.socketAddress,
                        Clock.System.now()
                    ))
                } catch (e: OscCodecException) {
                    hook.onDecodeError(payload, e)
                    logger.error(e) { "payload decode failed: ${e.message}" }
                }
            }
        } catch (e: SocketException) {
            if (!socket.isClosed) {
                val wrapped = OscTransportException("udp receive failed", e)
                hook.onTransportError(wrapped)
                throw wrapped
            }
            // socket is closed, stop the loop
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val wrapped = t as? OscTransportException ?: OscTransportException("udp receive failed", t)
            hook.onTransportError(wrapped)
            throw wrapped
        }
    }

    override suspend fun send(packet: OscPacket, target: SocketAddress) {
        val bytes = packet.encodeToByteArray()
        try {
            withContext(Dispatchers.IO) {
                socket.send(DatagramPacket(bytes, bytes.size, target))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val wrapped = t as? OscTransportException ?: OscTransportException("udp send failed", t)
            hook.onTransportError(wrapped)
            throw wrapped
        }
    }

    override suspend fun stop() {
        socket.close()
        recvJob?.cancelAndJoin()
        recvJob = null
    }

    override fun isClosed() = socket.isClosed

    companion object {
        private const val MAX_PACKET_SIZE = 65507  // UDP max payload

    }
}

private const val DEFAULT_CHANNEL_CAPACITY = 256
