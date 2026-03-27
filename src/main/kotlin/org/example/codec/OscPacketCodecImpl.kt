package org.example.codec

import org.example.config.OscSettings
import org.example.exception.OscCodecException
import org.example.type.OscBundle
import org.example.io.OscByteReader
import org.example.io.OscByteWriter
import org.example.type.OscMessage
import org.example.type.OscPacket
import org.example.util.OscLogger
import org.example.util.logger

/**
 * Dispatch OscPacket to correspond codec
 */
internal object OscPacketCodecImpl : OscPacketCodec<OscPacket>, OscLogger {
    override val logTag: String
        get() = "OscPacketCodecImpl"

    override fun encode(packet: OscPacket, writer: OscByteWriter): ByteArray {
        return when (packet) {
            is OscMessage -> OscMessageCodec.encode(packet, writer)
            is OscBundle -> OscBundleCodec.encode(packet, writer) { element ->
                encode(element, OscByteWriter())
            }
        }
    }

    override fun decode(reader: OscByteReader): OscPacket {
        val packet = if (reader.peekString() == OscBundleCodec.BUNDLE_TAG) {
            OscBundleCodec.decode(reader) { elementReader ->
                decode(elementReader)
            }
        } else {
            OscMessageCodec.decode(reader)
        }

        if (reader.hasRemaining()) {
            if (OscSettings.Codec.strict) {
                throw OscCodecException("osc packet bytes were not fully consumed")
            } else {
                logger.warn { "osc packet bytes were not fully consumed" }
            }
        }

        return packet
    }

}
