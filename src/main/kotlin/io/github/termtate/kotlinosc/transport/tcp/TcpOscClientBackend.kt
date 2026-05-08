package io.github.termtate.kotlinosc.transport.tcp

import io.github.termtate.kotlinosc.codec.OscPacketCodecImpl
import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.exception.OscTransportException
import io.github.termtate.kotlinosc.io.OscByteWriter
import io.github.termtate.kotlinosc.transport.OscClientBackend
import io.github.termtate.kotlinosc.transport.OscTransportHook
import io.github.termtate.kotlinosc.transport.tcp.codec.OscTcpFramingStrategy
import io.github.termtate.kotlinosc.transport.tcp.codec.OscFrameCodec
import io.github.termtate.kotlinosc.type.OscPacket
import io.github.termtate.kotlinosc.util.OscLogger
import io.github.termtate.kotlinosc.util.logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.Socket
import java.net.SocketAddress

internal class TcpOscClientBackend(
    private val targetAddress: SocketAddress,
    private val transportHook: OscTransportHook = OscTransportHook.Companion.NOOP,
    codecConfig: OscConfig.Codec = OscConfig.Codec.default,
    framingStrategy: OscTcpFramingStrategy
) : OscClientBackend, OscLogger {
    override val logTag: String
        get() = "TcpOscClientBackend"

    private var socket: Socket? = null

    private var output: OutputStream? = null

    private val frameCodec = OscFrameCodec(framingStrategy)

    private val codec = OscPacketCodecImpl(codecConfig)

    private val mutex = Mutex()

    private var closed = false

    override suspend fun send(packet: OscPacket) {
        val bytes = codec.encode(packet, OscByteWriter())
        val framed = frameCodec.encodeFrame(bytes)
        try {
            mutex.withLock {
                withContext(Dispatchers.IO) {
                    if (closed) {
                        throw OscTransportException("cannot send message after client is closed")
                    }
                    val out = connectedOutput()
                    try {
                        out.write(framed)
                        out.flush()
                    } catch (t: Throwable) {
                        resetConnection()
                        throw t
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val wrapped = t as? OscTransportException ?: OscTransportException("tcp send failed", t)
            transportHook.onTransportError(wrapped)
            logger.error(wrapped) { wrapped.message ?: "tcp send failed" }
            throw wrapped
        }
    }

    private fun connectedOutput(): OutputStream {
        output?.let { return it }

        val newSocket = Socket()
        try {
            newSocket.tcpNoDelay = true
            newSocket.connect(targetAddress, CONNECT_TIMEOUT_MS)

            socket = newSocket
            return newSocket.getOutputStream().also { output = it }
        } catch (t: Throwable) {
            runCatching { newSocket.close() }
            throw OscTransportException("tcp client connect failed", t)
        }
    }

    private fun resetConnection() {
        runCatching { socket?.close() }
        socket = null
        output = null
    }

    override suspend fun close() {
        mutex.withLock {
            closed = true
            withContext(Dispatchers.IO) {
                resetConnection()
            }
        }
    }

    private companion object {
        private const val CONNECT_TIMEOUT_MS = 5_000
    }
}
