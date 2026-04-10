package io.github.termtate.kotlinosc.transport.dsl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import io.github.termtate.kotlinosc.transport.OscClient
import io.github.termtate.kotlinosc.transport.OscTransportHook
import io.github.termtate.kotlinosc.util.OscDsl
import java.net.InetSocketAddress
import java.net.SocketAddress

@OscDsl
public class OscClientOptionsBuilder {
    public var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    public var transportHook: OscTransportHook = OscTransportHook.NOOP

    private val codecDsl = OscCodecDsl()

    /**
     * Configures codec behavior.
     */
    public fun codec(builder: OscCodecDsl.() -> Unit): OscCodecDsl =
        codecDsl.apply(builder)

    internal fun build(targetAddress: SocketAddress): OscClient {
        return OscClient(
            targetAddress = targetAddress,
            scope = scope,
            transportHook = transportHook,
            codecConfig = codecDsl.build()
        )
    }
}

/**
 * Creates an [OscClient] from full socket address.
 *
 * Example:
 * ```kotlin
 * val client = oscClient(InetSocketAddress("127.0.0.1", 9000)) {
 *     codec { strictCodecPayloadConsumption = true }
 * }
 * ```
 */
public fun oscClient(
    targetAddress: SocketAddress,
    builder: (OscClientOptionsBuilder.() -> Unit)? = null
): OscClient {
    val options = OscClientOptionsBuilder().apply {
        if (builder != null) {
            builder()
        }
    }
    return options.build(targetAddress)
}

/**
 * Creates an [OscClient] from ip/port.
 */
public fun oscClient(
    ipAddress: String,
    port: Int,
    builder: (OscClientOptionsBuilder.() -> Unit)? = null
): OscClient = oscClient(InetSocketAddress(ipAddress, port), builder)


