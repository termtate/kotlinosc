package io.github.termtate.kotlinosc.transport

import io.github.termtate.kotlinosc.transport.tcp.codec.OscTcpFramingStrategy

/**
 * Transport protocol used by OSC clients and servers.
 */
public sealed interface OscTransportProtocol {
    /**
     * Datagram transport. Each OSC packet is sent as one UDP datagram.
     */
    public data object Udp : OscTransportProtocol

    /**
     * Stream transport. OSC packets are framed before being sent over TCP.
     *
     * @property framingStrategy Strategy used to delimit OSC packets on the TCP byte stream.
     */
    public data class Tcp(
        val framingStrategy: OscTcpFramingStrategy = OscTcpFramingStrategy.LENGTH_PREFIXED
    ) : OscTransportProtocol
}
