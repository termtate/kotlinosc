package io.github.termtate.kotlinosc.transport.tcp

import io.github.termtate.kotlinosc.codec.OscPacketCodecImpl
import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.exception.OscCodecException
import io.github.termtate.kotlinosc.exception.OscFrameException
import io.github.termtate.kotlinosc.exception.OscTransportException
import io.github.termtate.kotlinosc.io.OscByteReader
import io.github.termtate.kotlinosc.transport.DEFAULT_CHANNEL_CAPACITY
import io.github.termtate.kotlinosc.transport.OscServerBackend
import io.github.termtate.kotlinosc.transport.OscTransportHook
import io.github.termtate.kotlinosc.transport.ReceivedPacket
import io.github.termtate.kotlinosc.transport.TcpPeer
import io.github.termtate.kotlinosc.transport.tcp.codec.OscTcpFramingStrategy
import io.github.termtate.kotlinosc.transport.tcp.codec.OscFrameCodec
import io.github.termtate.kotlinosc.util.OscLogger
import io.github.termtate.kotlinosc.util.logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
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
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Clock

internal class TcpOscServerBackend(
    private val scope: CoroutineScope,
    private val bindAddress: SocketAddress,
    receiveChannelCapacity: Int = DEFAULT_CHANNEL_CAPACITY,
    private val transportHook: OscTransportHook = OscTransportHook.Companion.NOOP,
    codecConfig: OscConfig.Codec = OscConfig.Codec.default,
    framingStrategy: OscTcpFramingStrategy
) : OscServerBackend, OscLogger {
    override val logTag: String
        get() = "TcpOscServerBackend"

    private val bufferedPackets = Channel<ReceivedPacket>(
        capacity = receiveChannelCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val receivedPackets: Flow<ReceivedPacket>
        get() = bufferedPackets.consumeAsFlow()

    private val socket: ServerSocket = ServerSocket().apply {
        reuseAddress = true
        bind(bindAddress)
    }

    private var recvJob: Job? = null

    private val codec = OscPacketCodecImpl(codecConfig)

    private val frameCodec = OscFrameCodec(framingStrategy)

    private val clientId = AtomicLong()

    private val clients: MutableMap<Long, ClientConnection> = ConcurrentHashMap()

    override suspend fun start() {
        if (recvJob != null) {
            return
        }
        recvJob = scope.launch(Dispatchers.IO) { acceptClients() }
    }

    private suspend fun acceptClients() {
        while (currentCoroutineContext().isActive) {
            try {
                val client = socket.accept()
                val id = clientId.getAndIncrement()

                val job = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
                    readForever(client, id)
                }

                clients[id] = ClientConnection(client, job)
                job.start()
            } catch (e: IOException) {
                if (socket.isClosed) {
                    // socket is closed, stop the loop
                    return
                }
                val wrapped = OscTransportException("tcp receive failed", e)
                transportHook.onTransportError(wrapped)
                throw wrapped
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                val wrapped = t as? OscTransportException ?: OscTransportException("tcp receive failed", t)
                transportHook.onTransportError(wrapped)
                throw wrapped
            }
        }
    }

    private suspend fun readForever(client: Socket, id: Long) {
        logger.debug { "new client accepted. Client: $client. Client id: $id" }

        try {
            val buffer = ByteArray(READ_BUFFER_SIZE)
            val input = client.getInputStream()
            val decoder = frameCodec.decoder()

            while (currentCoroutineContext().isActive) {
                val n = input.read(buffer)
                if (n == -1) {
                    logger.debug { "client read after EOF, break. Client: $client. Client id: $id" }
                    break
                }
                val frames = decoder.feed(buffer, n)

                for (frame in frames) {
                    try {
                        val packet = codec.decode(OscByteReader(frame))
                        bufferedPackets.send(
                            ReceivedPacket(
                                packet = packet,
                                peer = TcpPeer(id, remoteAddress = client.remoteSocketAddress),
                                receivedAt = Clock.System.now()
                            )
                        )
                    } catch (e: OscCodecException) {
                        transportHook.onDecodeError(frame, e)
                        logger.error(e) { "payload decode failed: ${e.message}" }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: OscFrameException) {
            val wrapped = OscTransportException("tcp framing failed", e)
            transportHook.onTransportError(wrapped)
        } catch (e: IOException) {
            if (client.isClosed) {
                return
            }
            val wrapped = OscTransportException("tcp receive failed", e)
            transportHook.onTransportError(wrapped)
        } catch (t: Throwable) {
            val wrapped = t as? OscTransportException ?: OscTransportException("tcp receive failed", t)
            transportHook.onTransportError(wrapped)
        } finally {
            clients.remove(id)
            closeQuietly(client, id)
        }
    }

    private fun closeQuietly(client: Socket, id: Long) {
        runCatching {
            client.close()
        }.onFailure { e ->
            logger.warn { "client close failed. id: $id; message: ${e.message}" }
        }
    }

    override suspend fun stop() {
        socket.close()
        recvJob?.cancelAndJoin()
        recvJob = null

        val snapshot = clients.toMap()

        snapshot.forEach { (id, conn) -> closeQuietly(conn.socket, id) }

        snapshot.values.forEach { it.job.cancelAndJoin() }
    }

    companion object {
        const val READ_BUFFER_SIZE = 8192
    }

    private data class ClientConnection(
        val socket: Socket,
        val job: Job
    )
}