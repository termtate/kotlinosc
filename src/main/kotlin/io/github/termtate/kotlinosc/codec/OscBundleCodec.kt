package io.github.termtate.kotlinosc.codec

import io.github.termtate.kotlinosc.arg.OscTimetag
import io.github.termtate.kotlinosc.type.OscBundle
import io.github.termtate.kotlinosc.io.OscByteReader
import io.github.termtate.kotlinosc.io.OscByteWriter
import io.github.termtate.kotlinosc.type.OscPacket
import io.github.termtate.kotlinosc.arg.toOscTimetag
import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.exception.OscBundleParseException
import io.github.termtate.kotlinosc.util.OscLogger
import io.github.termtate.kotlinosc.util.logger


internal class OscBundleCodec(private val config: OscConfig.Codec) : OscLogger {
    override val logTag: String
        get() = "OscBundleCodec"

    inline fun encode(
        message: OscBundle,
        writer: OscByteWriter,
        encodeElement: (OscPacket) -> ByteArray
    ): ByteArray {
        writer.writeString(BUNDLE_TAG)
        writer.writeInt64(message.timeTag.toULong())
        for (element in message.elements) {
            writer.writeSizedPacket(encodeElement(element))
        }
        return writer.toByteArray()
    }

    inline fun decode(
        reader: OscByteReader,
        decodeElement: (OscByteReader) -> OscPacket,
    ): OscBundle {
        val tag = reader.readString()
        if (tag != BUNDLE_TAG) {
            throw OscBundleParseException("Invalid bundle tag: $tag")
        }
        val timeTag = reader.readInt64().toULong()
        val elements = buildList {
            while (reader.hasRemaining()) {
                val elementReader = reader.readSizedPacketReader()
                val element = decodeElement(elementReader)
                if (elementReader.hasRemaining()) {
                    if (config.strictCodecPayloadConsumption) {
                        throw OscBundleParseException("Bundle element was not fully consumed")
                    } else {
                        logger.warn { "Bundle element was not fully consumed" }
                    }
                }
                add(element)
            }
        }
        return OscBundle(timeTag = OscTimetag(timeTag), elements = elements)
    }

    internal companion object {
        const val BUNDLE_TAG = "#bundle"
    }
}

