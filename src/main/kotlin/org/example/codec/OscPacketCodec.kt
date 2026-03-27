package org.example.codec

import org.example.io.OscByteReader
import org.example.io.OscByteWriter
import org.example.type.OscPacket

/**
 * OscMessage <-> ByteArray encode and decode
 */
internal interface OscPacketCodec<T : OscPacket> {
    /** Encode into an OSC packet (UDP datagram payload).
     *
     * @throws org.example.exception.OscCodecException
     */
    fun encode(packet: T, writer: OscByteWriter): ByteArray

    /**
     * Decode from an OSC packet
     *
     * @throws org.example.exception.OscCodecException
     */
    fun decode(reader: OscByteReader): T
}

