package io.github.termtate.kotlinosc.transport

import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.exception.OscLifecycleException
import io.github.termtate.kotlinosc.route.DispatchMode
import io.github.termtate.kotlinosc.route.OscDispatcher
import io.github.termtate.kotlinosc.route.OscPacketScheduler
import io.github.termtate.kotlinosc.route.OscRouter
import io.github.termtate.kotlinosc.util.OscLogger
import io.github.termtate.kotlinosc.util.logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import java.io.Closeable
import java.net.DatagramSocket
import java.net.SocketAddress
import java.util.concurrent.atomic.AtomicReference

/**
 * UDP OSC server runtime.
 *
 * Lifecycle model:
 * - [start] / [startAsync] are startup entry points (not concurrency-safe)
 * - [stopAsync] is concurrency-safe and idempotent
 * - [stop] awaits full shutdown
 * - [close] triggers fire-and-forget shutdown via [stopAsync]
 *
 * This server is one-shot: calling [start] after a completed [stop] throws [OscLifecycleException].
 *
 * Example:
 * ```
 * val server = oscServer("127.0.0.1", 9000) {
 *     route {
 *         on("/ping") { message ->
 *             println("received: ${message.address}")
 *         }
 *     }
 * }.startAsync()
 * // ...
 * server.stop()
 * ```
 */
public class OscServer internal constructor(
    private val options: OscServerOptions
) : OscLogger, Closeable {
    internal constructor(
        bindAddress: SocketAddress,
        router: OscRouter,
        dispatchMode: DispatchMode = DispatchMode.ALL_MATCH,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        continueOnDispatchError: Boolean = true,
        maxConcurrentDispatches: Int = 1,
        dispatchDispatcher: CoroutineDispatcher = Dispatchers.IO,
        transportHook: OscTransportHook = OscTransportHook.NOOP,
        strictAddressPattern: Boolean = true,
        strictCodecPayloadConsumption: Boolean = true
    ) : this(
        OscServerOptions(
            bindAddress = bindAddress,
            router = router,
            dispatchMode = dispatchMode,
            scope = scope,
            continueOnDispatchError = continueOnDispatchError,
            maxConcurrentDispatches = maxConcurrentDispatches,
            dispatchDispatcher = dispatchDispatcher,
            transportHook = transportHook,
            codecConfig = OscConfig.Codec(strictCodecPayloadConsumption),
            addressPatternConfig = OscConfig.AddressPattern(strictAddressPattern)
        )
    )

    override val logTag: String
        get() = "OscServer"

    private val dispatcher = OscDispatcher(options.router, options.addressPatternConfig)
    private val scheduler = OscPacketScheduler.factory(
        dispatcher::dispatch,
        options.scope,
        continueOnDispatchError = options.continueOnDispatchError,
        maxConcurrentDispatches = options.maxConcurrentDispatches,
        dispatchDispatcher = options.dispatchDispatcher
    )

    private val transport = UdpOscTransport(
        options.scope,
        DatagramSocket(options.bindAddress),
        hook = options.transportHook,
        codecConfig = options.codecConfig
    )

    private var collectorJob: Job? = null
    private val lifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val closeTaskRef = AtomicReference<Deferred<Unit>?>(null)

    public val bindAddress: SocketAddress = options.bindAddress
    public val router: OscRouter = options.router

    /**
     * Starts receive and dispatch loops.
     *
     * Not concurrency-safe. Repeated calls while active are ignored.
     * Calling this method after shutdown throws [OscLifecycleException].
     *
     * Recommended startup API in structured coroutine code.
     */
    public suspend fun start() {
        if (collectorJob?.isActive == true) return
        if (transport.isClosed()) {
            throw OscLifecycleException("OscServer.start() cannot be called after stop()")
        }

        try {
            collectorJob = options.scope.launch {
                transport.receivedPackets.collect { receivedPacket ->
                    logger.debug { "received packet $receivedPacket" }
                    scheduler.schedule(receivedPacket.packet, options.dispatchMode)
                }
            }
            scheduler.start()
            transport.start()
        } catch (t: Throwable) {
            collectorJob?.cancelAndJoin()
            collectorJob = null
            throw t
        }

        logger.info { "osc server started" }
    }

    /**
     * Non-blocking startup trigger.
     *
     * Not concurrency-safe.
     *
     * Use [start] when you need completion or failure in the current coroutine.
     */
    public fun startAsync(): Deferred<Unit> = lifecycleScope.async(start = CoroutineStart.DEFAULT) { start() }

    private suspend fun stopUnsafe() {
        collectorJob?.cancelAndJoin()
        collectorJob = null
        transport.stop()
        scheduler.stop()
        logger.info { "osc server stopped" }
    }

    /**
     * Non-blocking shutdown trigger.
     *
     * Concurrency-safe and idempotent:
     * concurrent calls share one shutdown [Deferred].
     *
     * This is the primary shutdown primitive used by [stop] and [close].
     */
    public fun stopAsync(): Deferred<Unit> {
        closeTaskRef.get()?.let { return it }

        val created = lifecycleScope.async(start = CoroutineStart.LAZY) { stopUnsafe() }
        if (closeTaskRef.compareAndSet(null, created)) {
            created.start()
            return created
        }

        created.cancel()
        return closeTaskRef.get()!!
    }

    /**
     * Suspends until shutdown fully completes.
     *
     * Recommended shutdown API in structured coroutine code.
     */
    public suspend fun stop() {
        stopAsync().await()
    }

    /**
     * [Closeable] integration: triggers non-blocking shutdown.
     */
    override fun close() {
        stopAsync()
    }
}
