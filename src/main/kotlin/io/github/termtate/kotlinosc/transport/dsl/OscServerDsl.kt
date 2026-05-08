package io.github.termtate.kotlinosc.transport.dsl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import io.github.termtate.kotlinosc.route.DispatchMode
import io.github.termtate.kotlinosc.route.OscRouter
import io.github.termtate.kotlinosc.transport.OscServer
import io.github.termtate.kotlinosc.transport.OscServerOptions
import io.github.termtate.kotlinosc.transport.OscTransportHook
import io.github.termtate.kotlinosc.util.OscDsl
import java.net.InetSocketAddress
import java.net.SocketAddress

/**
 * Builder for [oscServer] options.
 */
@OscDsl
public class OscServerOptionsBuilder {
    /**
     * DSL block for route dispatch behavior.
     */
    @OscDsl
    public class OscServerDispatchDsl {
        /**
         * Route dispatch strategy used when multiple routes match a packet address.
         */
        public var dispatchMode: DispatchMode = DispatchMode.ALL_MATCH

        /**
         * Whether route dispatch errors are logged and ignored instead of stopping dispatch.
         */
        public var continueOnDispatchError: Boolean = true

        /**
         * Dispatcher used for route dispatch work.
         */
        public var dispatchDispatcher: CoroutineDispatcher = Dispatchers.IO

        /**
         * Maximum number of concurrent dispatch jobs.
         */
        public var maxConcurrentDispatches: Int = 1

    }

    private val dispatchDsl = OscServerDispatchDsl()

    /**
     * Configures route dispatch behavior.
     */
    public fun dispatch(builder: OscServerDispatchDsl.() -> Unit): OscServerDispatchDsl {
        return dispatchDsl.apply(builder)
    }

    private val codecDsl = OscCodecDsl()

    /**
     * Configures codec behavior.
     */
    public fun codec(builder: OscCodecDsl.() -> Unit): OscCodecDsl =
        codecDsl.apply(builder)

    private val patternDsl = OscAddressPatternDsl()

    /**
     * Configures address pattern behavior.
     */
    public fun addressPattern(builder: OscAddressPatternDsl.() -> Unit): OscAddressPatternDsl =
        patternDsl.apply(builder)


    /**
     * Route registry used by the server.
     */
    public var router: OscRouter = OscRouter()

    /**
     * Configures routes on the server router.
     */
    public fun route(builder: OscRouter.() -> Unit): OscRouter = router.apply(builder)



    /**
     * Parent scope used by server receive and dispatch jobs.
     */
    public var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)


    /**
     * Hook callbacks for transport and decode errors.
     */
    public var transportHook: OscTransportHook = OscTransportHook.Companion.NOOP

    private val protocolDsl = OscProtocolDsl()

    /**
     * Configures UDP/TCP transport protocol options.
     */
    public fun protocol(builder: OscProtocolDsl.() -> Unit): OscProtocolDsl =
        protocolDsl.apply(builder)


    internal fun build(bindAddress: SocketAddress): OscServerOptions = OscServerOptions(
        bindAddress = bindAddress,
        router = router,
        dispatchMode = dispatchDsl.dispatchMode,
        scope = scope,
        continueOnDispatchError = dispatchDsl.continueOnDispatchError,
        maxConcurrentDispatches = dispatchDsl.maxConcurrentDispatches,
        dispatchDispatcher = dispatchDsl.dispatchDispatcher,
        transportHook = transportHook,
        codecConfig = codecDsl.build(),
        addressPatternConfig = patternDsl.build(),
        protocol = protocolDsl.protocol
    )
}

/**
 * Creates an [OscServer] from a full socket address.
 *
 * Example:
 * ```kotlin
 * val server = oscServer(InetSocketAddress("127.0.0.1", 9000)) {
 *     protocol {
 *         tcp {
 *             framingStrategy = OscTcpFramingStrategy.SLIP
 *         }
 *     }
 *     codec { strictCodecPayloadConsumption = true }
 *     addressPattern { strictAddressPattern = true }
 * }
 * ```
 */
public fun oscServer(bindAddress: SocketAddress, builder: (OscServerOptionsBuilder.() -> Unit)? = null): OscServer {
    val options = OscServerOptionsBuilder().apply {
        if (builder != null) {
            builder()
        }
    }.build(bindAddress)

    return OscServer(options)
}

/**
 * Creates an [OscServer] from ip/port.
 */
public fun oscServer(ipAddress: String, port: Int, builder: (OscServerOptionsBuilder.() -> Unit)? = null): OscServer {
    return oscServer(InetSocketAddress(ipAddress, port), builder)
}

