package io.github.termtate.kotlinosc.transport

import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.exception.OscTransportException
import io.github.termtate.kotlinosc.transport.tcp.TcpOscClientBackend
import io.github.termtate.kotlinosc.transport.udp.UdpOscClientBackend
import io.github.termtate.kotlinosc.type.OscPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.io.Closeable
import java.net.SocketAddress
import java.util.concurrent.atomic.AtomicReference

/**
 * OSC client runtime for sending packets over UDP or TCP.
 *
 * Lifecycle model:
 * - [closeAsync] is concurrency-safe and idempotent
 * - [closeAndJoin] awaits full shutdown
 * - [close] triggers fire-and-forget shutdown
 *
 * Recommended usage:
 * - structured coroutine shutdown: [closeAndJoin]
 * - [Closeable] integration / best-effort shutdown: [close]
 *
 * UDP is used by default. Use [OscTransportProtocol.Tcp] to send framed OSC
 * packets over a persistent TCP connection.
 */
public class OscClient(
    private val targetAddress: SocketAddress,
    transportHook: OscTransportHook = OscTransportHook.NOOP,
    codecConfig: OscConfig.Codec = OscConfig.Codec.default,
    protocol: OscTransportProtocol = OscTransportProtocol.Udp
) : Closeable {
    private val closeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val closeTaskRef = AtomicReference<Deferred<Unit>?>(null)

    private val backend: OscClientBackend = when (protocol) {
        OscTransportProtocol.Udp -> UdpOscClientBackend(
            targetAddress = targetAddress,
            transportHook = transportHook,
            codecConfig = codecConfig
        )
        is OscTransportProtocol.Tcp -> TcpOscClientBackend(
            targetAddress = targetAddress,
            transportHook = transportHook,
            codecConfig = codecConfig,
            framingStrategy = protocol.framingStrategy
        )
    }

    /**
     * Sends one OSC packet to configured target address.
     *
     * Send after [close] throws [OscTransportException].
     */
    public suspend fun send(packet: OscPacket) {
        backend.send(packet)
    }

    /**
     * Non-blocking shutdown trigger.
     *
     * Concurrency-safe and idempotent:
     * concurrent calls share one shutdown [Deferred].
     *
     * This is the primary shutdown primitive used by [closeAndJoin] and [close].
     */
    public fun closeAsync(): Deferred<Unit> {
        closeTaskRef.get()?.let { return it }

        val created = closeScope.async(start = CoroutineStart.LAZY) { backend.close() }
        if (closeTaskRef.compareAndSet(null, created)) {
            created.start()
            return created
        }

        created.cancel()
        return closeTaskRef.get()!!
    }

    /**
     * Suspends until client shutdown fully completes.
     */
    public suspend fun closeAndJoin() {
        closeAsync().await()
    }

    /**
     * [Closeable] integration: triggers non-blocking shutdown.
     */
    override fun close() {
        closeAsync()
    }
}
