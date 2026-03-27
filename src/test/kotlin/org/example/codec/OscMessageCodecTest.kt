package org.example.codec

import org.example.arg.OscBlob
import org.example.arg.OscFalse
import org.example.arg.OscFloat32
import org.example.arg.OscFloat64
import org.example.arg.OscInfinitum
import org.example.arg.OscInt32
import org.example.arg.OscInt64
import org.example.arg.OscNil
import org.example.arg.OscChar
import org.example.arg.OscMIDI
import org.example.arg.OscRGBA
import org.example.arg.OscString
import org.example.arg.OscSymbol
import org.example.arg.OscTimetag
import org.example.arg.OscTrue
import org.example.arg.toOscTimetag
import org.example.config.OscSettings
import org.example.exception.OscBufferUnderflowException
import org.example.exception.OscCodecException
import org.example.exception.OscTypeTagParseException
import org.example.io.OscByteReader
import org.example.io.OscByteWriter
import org.example.type.MIDI
import org.example.type.OscBundle
import org.example.type.OscMessage
import org.example.type.RGBA
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock

class OscMessageCodecTest {
    @Test
    fun `osc message round trip basic types`() {
        val msg = OscMessage(
            address = "/volume",
            args = listOf(
                OscInt32(17),
                OscFloat32(0.4f),
                OscString("path"),
                OscBlob(byteArrayOf(1, 2, 3))
            )
        )

        val bytes = OscMessageCodec.encode(msg, OscByteWriter())
        val decoded = OscMessageCodec.decode(OscByteReader(bytes))

        assertEquals(msg, decoded)
    }

    @Test
    fun `decode invalid type tag should fail`() {
        val writer = OscByteWriter()
        writer.writeString("/bad")
        writer.writeString("i")
        writer.writeInt32(1)

        assertFailsWith<OscTypeTagParseException> {
            OscMessageCodec.decode(OscByteReader(writer.toByteArray()))
        }
    }

    @Test
    fun `bundle round trip`() {
        val packet = OscBundle(
            timeTag = 1L.toOscTimetag(),
            elements = listOf(
                OscMessage("/a", listOf(OscInt32(7))),
                OscMessage("/b", listOf(OscString("ok")))
            )
        )

        val bytes = OscPacketCodecImpl.encode(packet, OscByteWriter())
        val decoded = OscPacketCodecImpl.decode(OscByteReader(bytes))

        assertEquals(packet, decoded)
    }

    @Test
    fun `nested bundle round trip`() {
        val packet = OscBundle(
            timeTag = 10L.toOscTimetag(),
            elements = listOf(
                OscMessage("/root", listOf(OscFloat32(1.5f))),
                OscBundle(
                    timeTag = 20L.toOscTimetag(),
                    elements = listOf(
                        OscMessage("/child", listOf(OscBlob(byteArrayOf(1, 2, 3))))
                    )
                )
            )
        )

        val bytes = OscPacketCodecImpl.encode(packet, OscByteWriter())
        val decoded = OscPacketCodecImpl.decode(OscByteReader(bytes))

        assertEquals(packet, decoded)
    }

    @Test
    fun `bundle decode invalid element size should fail`() {
        val writer = OscByteWriter()
        writer.writeString("#bundle")
        writer.writeInt64(1L)
        writer.writeInt32(1024)

        assertFailsWith<OscBufferUnderflowException> {
            OscPacketCodecImpl.decode(OscByteReader(writer.toByteArray()))
        }
    }

    @Test
    fun `TFNI tag round-trip`() {
        val message = OscMessage(
            "/x",
            listOf(OscTrue, OscFalse, OscNil, OscInfinitum)
        )

        val bytes = OscPacketCodecImpl.encode(message, OscByteWriter())
        val decoded = OscPacketCodecImpl.decode(OscByteReader(bytes))

        assertEquals(message, decoded)
    }

    @Test
    fun `int64 float64 tag round-trip`() {
        val message = OscMessage(
            "/x",
            listOf(OscInt64(1L), OscInt64(Long.MAX_VALUE), OscFloat64(1.0), OscFloat64(Double.MAX_VALUE))
        )

        val bytes = OscPacketCodecImpl.encode(message, OscByteWriter())
        val decoded = OscPacketCodecImpl.decode(OscByteReader(bytes))

        assertEquals(message, decoded)
    }

    @Test
    fun `char tag round-trip`() {
        val message = OscMessage(
            "/x",
            listOf(OscChar('a'), OscChar('b'), OscChar('c'), OscString("other thing"))
        )

        val bytes = OscPacketCodecImpl.encode(message, OscByteWriter())
        val decoded = OscPacketCodecImpl.decode(OscByteReader(bytes))

        assertEquals(message, decoded)
    }

    @Test
    fun `r, m, t, S tag round-trip`() {
        val message = OscMessage(
            "/x",
            listOf(
                OscRGBA(RGBA(1u, 2u, 3u, 4u)),
                OscMIDI(MIDI(5u, 6u, 7u, 8u)),
                Clock.System.now().toOscTimetag(),
                OscSymbol("hello")
            )
        )

        val bytes = OscPacketCodecImpl.encode(message, OscByteWriter())
        val decoded = OscPacketCodecImpl.decode(OscByteReader(bytes))

        assertEquals(message, decoded)
    }

    @Test
    fun `Timetag boundary test`() {
        val message = OscMessage("/x", listOf(Clock.System.now().toOscTimetag(), OscTimetag.Companion.IMMEDIATELY))

        val bytes = OscPacketCodecImpl.encode(message, OscByteWriter())
        val decoded = OscPacketCodecImpl.decode(OscByteReader(bytes))

        assertEquals(message, decoded)
    }

    @Test
    fun `decode should fail on trailing bytes when codec strict is true`() {
        val message = OscMessage("/x", listOf(OscInt32(1)))
        val bytes = OscPacketCodecImpl.encode(message, OscByteWriter())
        val withTrailing = bytes + 0x00

        val previous = OscSettings.Codec.strict
        OscSettings.Codec.strict = true
        try {
            assertFailsWith<OscCodecException> {
                OscPacketCodecImpl.decode(OscByteReader(withTrailing))
            }
        } finally {
            OscSettings.Codec.strict = previous
        }
    }

    @Test
    fun `decode should ignore trailing bytes when codec strict is false`() {
        val message = OscMessage("/x", listOf(OscInt32(1)))
        val bytes = OscPacketCodecImpl.encode(message, OscByteWriter())
        val withTrailing = bytes + 0x00

        val previous = OscSettings.Codec.strict
        OscSettings.Codec.strict = false
        try {
            val decoded = OscPacketCodecImpl.decode(OscByteReader(withTrailing))
            assertEquals(message, decoded)
        } finally {
            OscSettings.Codec.strict = previous
        }
    }
}
