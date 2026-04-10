package io.github.termtate.kotlinosc.codec

import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.exception.OscCodecException
import io.github.termtate.kotlinosc.type.OscBundle
import io.github.termtate.kotlinosc.io.OscByteReader
import io.github.termtate.kotlinosc.io.OscByteWriter
import io.github.termtate.kotlinosc.type.OscMessage
import io.github.termtate.kotlinosc.type.OscPacket
import io.github.termtate.kotlinosc.util.OscLogger
import io.github.termtate.kotlinosc.util.logger

/**
 * Dispatch OscPacket to correspond codec
 */
internal class OscPacketCodecImpl(private val config: OscConfig.Codec) : OscPacketCodec<OscPacket>, OscLogger {
    override val logTag: String
        get() = "OscPacketCodecImpl"

    private val bundleCodec = OscBundleCodec(config)

    override fun encode(packet: OscPacket, writer: OscByteWriter): ByteArray {
        return when (packet) {
            is OscMessage -> OscMessageCodec.encode(packet, writer)
            is OscBundle -> bundleCodec.encode(packet, writer) { element ->
                encode(element, OscByteWriter())
            }
        }
    }

    override fun decode(reader: OscByteReader): OscPacket {
        val packet = if (reader.peekString() == OscBundleCodec.BUNDLE_TAG) {
            bundleCodec.decode(reader) { elementReader ->
                decode(elementReader)
            }
        } else {
            OscMessageCodec.decode(reader)
        }

        if (reader.hasRemaining()) {
            if (config.strictCodecPayloadConsumption) {
                throw OscCodecException("osc packet bytes were not fully consumed")
            } else {
                logger.warn { "osc packet bytes were not fully consumed" }
            }
        }

        return packet
    }
    internal companion object {
        val default: OscPacketCodec<OscPacket> = OscPacketCodecImpl(OscConfig.Codec.default)
    }
}

