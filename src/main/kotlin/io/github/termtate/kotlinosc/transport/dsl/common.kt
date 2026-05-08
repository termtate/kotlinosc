package io.github.termtate.kotlinosc.transport.dsl

import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.transport.OscTransportProtocol
import io.github.termtate.kotlinosc.transport.tcp.codec.OscTcpFramingStrategy
import io.github.termtate.kotlinosc.util.OscDsl

/**
 * DSL block for codec options shared by clients and servers.
 */
@OscDsl
public class OscCodecDsl {
    /**
     * Mirrors [io.github.termtate.kotlinosc.config.OscConfig.Codec.strictCodecPayloadConsumption].
     */
    public var strictCodecPayloadConsumption: Boolean = true

    internal fun build() = OscConfig.Codec(strictCodecPayloadConsumption)

}

/**
 * DSL block for selecting the OSC transport protocol.
 *
 * UDP is selected by default. Call [tcp] to use TCP with packet framing.
 */
@OscDsl
public class OscProtocolDsl {
    internal var protocol: OscTransportProtocol = OscTransportProtocol.Udp

    /**
     * Selects UDP transport.
     */
    public fun udp() {
        protocol = OscTransportProtocol.Udp
    }

    /**
     * Selects TCP transport and configures TCP framing options.
     */
    public fun tcp(builder: (OscTcpDsl.() -> Unit)? = null) {
        protocol = OscTcpDsl().apply {
            if (builder != null) {
                builder()
            }
        }.build()
    }

    /**
     * DSL block for TCP-specific transport options.
     */
    public class OscTcpDsl {
        /**
         * Strategy used to delimit OSC packets on the TCP byte stream.
         */
        public var framingStrategy: OscTcpFramingStrategy = OscTcpFramingStrategy.LENGTH_PREFIXED

        internal fun build(): OscTransportProtocol.Tcp = OscTransportProtocol.Tcp(framingStrategy)
    }


}


/**
 * DSL block for OSC address pattern matching options.
 */
@OscDsl
public class OscAddressPatternDsl {
    /**
     * Mirrors [OscConfig.AddressPattern.strictAddressPattern].
     */
    public var strictAddressPattern: Boolean = true

    internal fun build(): OscConfig.AddressPattern = OscConfig.AddressPattern(strictAddressPattern)

}
