package io.github.termtate.kotlinosc.transport.tcp.codec

import io.github.termtate.kotlinosc.io.OscByteWriter
import io.github.termtate.kotlinosc.transport.tcp.codec.decoder.LengthPrefixedOscFrameDecoder
import io.github.termtate.kotlinosc.transport.tcp.codec.decoder.OscFrameDecoder
import io.github.termtate.kotlinosc.transport.tcp.codec.decoder.SlipOscFrameDecoder

internal class OscFrameCodec(
    val framingStrategy: OscTcpFramingStrategy
) {
    fun encodeFrame(packetBytes: ByteArray): ByteArray {
        return when (framingStrategy) {
            OscTcpFramingStrategy.LENGTH_PREFIXED -> {
                OscByteWriter().apply {
                    writeInt32(packetBytes.size)
                    writeByteArray(packetBytes)
                }.toByteArray()
            }

            OscTcpFramingStrategy.SLIP -> encodeSlipFrame(packetBytes)
        }
    }

    fun decoder(): OscFrameDecoder = when (framingStrategy) {
        OscTcpFramingStrategy.LENGTH_PREFIXED -> LengthPrefixedOscFrameDecoder()
        OscTcpFramingStrategy.SLIP -> SlipOscFrameDecoder()
    }


    private fun encodeSlipFrame(packetBytes: ByteArray): ByteArray {
        val encoded = ArrayList<Byte>(packetBytes.size + 2)
        encoded += SLIP_END

        for (byte in packetBytes) {
            when (byte) {
                SLIP_END -> {
                    encoded += SLIP_ESC
                    encoded += SLIP_ESC_END
                }

                SLIP_ESC -> {
                    encoded += SLIP_ESC
                    encoded += SLIP_ESC_ESC
                }

                else -> encoded += byte
            }
        }

        encoded += SLIP_END
        return encoded.toByteArray()
    }

    companion object {
        internal const val SLIP_END: Byte = 0xC0.toByte()
        internal const val SLIP_ESC: Byte = 0xDB.toByte()
        internal const val SLIP_ESC_END: Byte = 0xDC.toByte()
        internal const val SLIP_ESC_ESC: Byte = 0xDD.toByte()
    }
}

