package io.github.termtate.kotlinosc.transport.tcp.codec

/**
 * Framing strategy used to delimit OSC packets on a TCP byte stream.
 */
public enum class OscTcpFramingStrategy {
    /**
     * Prefixes each OSC packet with a 32-bit big-endian payload length.
     */
    LENGTH_PREFIXED,

    /**
     * Uses SLIP framing and byte escaping.
     */
    SLIP
}
