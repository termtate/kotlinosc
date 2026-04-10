package io.github.termtate.kotlinosc.codec

import io.github.termtate.kotlinosc.io.OscByteReader
import io.github.termtate.kotlinosc.io.OscByteWriter
import io.github.termtate.kotlinosc.type.OscPacket

/**
 * OscMessage <-> ByteArray encode and decode
 */
internal interface OscPacketCodec<T : OscPacket> {
    /** Encode into an OSC packet (UDP datagram payload).
     *
     * @throws io.github.termtate.kotlinosc.exception.OscCodecException
     */
    fun encode(packet: T, writer: OscByteWriter): ByteArray

    /**
     * Decode from an OSC packet
     *
     * @throws io.github.termtate.kotlinosc.exception.OscCodecException
     */
    fun decode(reader: OscByteReader): T
}


