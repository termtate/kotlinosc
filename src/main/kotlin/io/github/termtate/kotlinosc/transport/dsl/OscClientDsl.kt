package io.github.termtate.kotlinosc.transport.dsl

import io.github.termtate.kotlinosc.transport.OscClient
import io.github.termtate.kotlinosc.transport.OscTransportHook
import io.github.termtate.kotlinosc.util.OscDsl
import java.net.InetSocketAddress
import java.net.SocketAddress

/**
 * Builder for [oscClient] options.
 */
@OscDsl
public class OscClientOptionsBuilder {
    /**
     * Hook callbacks for transport-level send errors.
     */
    public var transportHook: OscTransportHook = OscTransportHook.NOOP

    private val codecDsl = OscCodecDsl()

    private val protocolDsl = OscProtocolDsl()

    /**
     * Configures codec behavior.
     */
    public fun codec(builder: OscCodecDsl.() -> Unit): OscCodecDsl =
        codecDsl.apply(builder)

    /**
     * Configures UDP/TCP transport protocol options.
     */
    public fun protocol(builder: OscProtocolDsl.() -> Unit): OscProtocolDsl =
        protocolDsl.apply(builder)

    internal fun build(targetAddress: SocketAddress): OscClient {
        return OscClient(
            targetAddress = targetAddress,
            transportHook = transportHook,
            codecConfig = codecDsl.build(),
            protocol = protocolDsl.protocol
        )
    }
}

/**
 * Creates an [OscClient] from a full socket address.
 *
 * Example:
 * ```kotlin
 * val client = oscClient(InetSocketAddress("127.0.0.1", 9000)) {
 *     protocol { udp() }
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
 *
 * Example:
 * ```kotlin
 * val client = oscClient("127.0.0.1", 9000) {
 *     protocol { udp() }
 *     codec { strictCodecPayloadConsumption = true }
 * }
 * ```
 */
public fun oscClient(
    ipAddress: String,
    port: Int,
    builder: (OscClientOptionsBuilder.() -> Unit)? = null
): OscClient = oscClient(InetSocketAddress(ipAddress, port), builder)
