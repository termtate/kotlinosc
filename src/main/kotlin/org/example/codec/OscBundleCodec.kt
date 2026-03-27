package org.example.codec

import org.example.type.OscBundle
import org.example.io.OscByteReader
import org.example.io.OscByteWriter
import org.example.type.OscPacket
import org.example.arg.toOscTimetag
import org.example.config.OscSettings
import org.example.exception.OscBundleParseException
import org.example.util.OscLogger
import org.example.util.logger

/**
 * [OscBundleCodec] 对 [OscPacketCodecImpl] 有强依赖，如果实现[OscPacketCodec]接口会有循环引用，后续优化
 */
internal object OscBundleCodec : OscLogger {
    override val logTag: String
        get() = "OscBundleCodec"

    fun encode(
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

    fun decode(
        reader: OscByteReader,
        decodeElement: (OscByteReader) -> OscPacket
    ): OscBundle {
        val tag = reader.readString()
        if (tag != BUNDLE_TAG) {
            throw OscBundleParseException("Invalid bundle tag: $tag")
        }
        val timeTag = reader.readInt64()
        val elements = buildList {
            while (reader.hasRemaining()) {
                val elementReader = reader.readSizedPacketReader()
                val element = decodeElement(elementReader)
                if (elementReader.hasRemaining()) {
                    if (OscSettings.Codec.strict) {
                        throw OscBundleParseException("Bundle element was not fully consumed")
                    } else {
                        logger.warn { "Bundle element was not fully consumed" }
                    }
                }
                add(element)
            }
        }
        return OscBundle(timeTag = timeTag.toOscTimetag(), elements = elements)
    }

    const val BUNDLE_TAG = "#bundle"
}
