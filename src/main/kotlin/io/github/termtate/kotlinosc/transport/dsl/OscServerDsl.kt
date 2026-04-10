package io.github.termtate.kotlinosc.transport.dsl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.route.DispatchMode
import io.github.termtate.kotlinosc.route.OscRouter
import io.github.termtate.kotlinosc.transport.OscServer
import io.github.termtate.kotlinosc.transport.OscServerOptions
import io.github.termtate.kotlinosc.transport.OscTransportHook
import io.github.termtate.kotlinosc.util.OscDsl
import java.net.InetSocketAddress
import java.net.SocketAddress

@OscDsl
public class OscCodecDsl {
    /**
     * Mirrors [OscConfig.Codec.strictCodecPayloadConsumption].
     */
    public var strictCodecPayloadConsumption: Boolean = true

    internal fun build() = OscConfig.Codec(strictCodecPayloadConsumption)

}


@OscDsl
public class OscAddressPatternDsl {
    /**
     * Mirrors [OscConfig.AddressPattern.strictAddressPattern].
     */
    public var strictAddressPattern: Boolean = true

    internal fun build(): OscConfig.AddressPattern = OscConfig.AddressPattern(strictAddressPattern)

}


@OscDsl
public class OscServerOptionsBuilder {
    @OscDsl
    public class OscServerDispatchDsl {
        public var dispatchMode: DispatchMode = DispatchMode.ALL_MATCH

        public var continueOnDispatchError: Boolean = true

        public var dispatchDispatcher: CoroutineDispatcher = Dispatchers.IO

        public var maxConcurrentDispatches: Int = 1

    }

    private val dispatchDsl = OscServerDispatchDsl()

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


    public var router: OscRouter = OscRouter()

    public fun route(builder: OscRouter.() -> Unit): OscRouter = router.apply(builder)



    public var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)


    public var transportHook: OscTransportHook = OscTransportHook.Companion.NOOP


    internal fun build(bindAddress: SocketAddress): OscServerOptions = OscServerOptions(
        bindAddress,
        router,
        dispatchDsl.dispatchMode,
        scope,
        dispatchDsl.continueOnDispatchError,
        dispatchDsl.maxConcurrentDispatches,
        dispatchDsl.dispatchDispatcher,
        transportHook,
        codecDsl.build(),
        patternDsl.build(),
    )
}

/**
 * Creates an [OscServer] from full socket address.
 *
 * Example:
 * ```kotlin
 * val server = oscServer(InetSocketAddress("127.0.0.1", 9000)) {
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

