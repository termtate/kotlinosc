package io.github.termtate.kotlinosc.codec

import io.github.termtate.kotlinosc.arg.OscBlob
import io.github.termtate.kotlinosc.arg.OscFalse
import io.github.termtate.kotlinosc.arg.OscFloat32
import io.github.termtate.kotlinosc.arg.OscFloat64
import io.github.termtate.kotlinosc.arg.OscInfinitum
import io.github.termtate.kotlinosc.arg.OscInt32
import io.github.termtate.kotlinosc.arg.OscInt64
import io.github.termtate.kotlinosc.arg.OscNil
import io.github.termtate.kotlinosc.arg.OscChar
import io.github.termtate.kotlinosc.arg.OscMIDI
import io.github.termtate.kotlinosc.arg.OscRGBA
import io.github.termtate.kotlinosc.arg.OscString
import io.github.termtate.kotlinosc.arg.OscSymbol
import io.github.termtate.kotlinosc.arg.OscTimetag
import io.github.termtate.kotlinosc.arg.OscTrue
import io.github.termtate.kotlinosc.arg.toOscTimetag
import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.exception.OscBufferUnderflowException
import io.github.termtate.kotlinosc.exception.OscCodecException
import io.github.termtate.kotlinosc.exception.OscTypeTagParseException
import io.github.termtate.kotlinosc.io.OscByteReader
import io.github.termtate.kotlinosc.io.OscByteWriter
import io.github.termtate.kotlinosc.type.MIDI
import io.github.termtate.kotlinosc.type.OscBundle
import io.github.termtate.kotlinosc.type.OscMessage
import io.github.termtate.kotlinosc.type.RGBA
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
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

        val bytes = OscPacketCodecImpl.default.encode(packet, OscByteWriter())
        val decoded = OscPacketCodecImpl.default.decode(OscByteReader(bytes))

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

        val bytes = OscPacketCodecImpl.default.encode(packet, OscByteWriter())
        val decoded = OscPacketCodecImpl.default.decode(OscByteReader(bytes))

        assertEquals(packet, decoded)
    }

    @Test
    fun `bundle round trip should preserve modern future timetag`() {
        val future = Instant.fromEpochSeconds(1_776_680_000, 123_000_000).toOscTimetag()
        val packet = OscBundle(
            timeTag = future,
            elements = listOf(
                OscMessage("/scheduled", listOf(OscString("future")))
            )
        )

        val bytes = OscPacketCodecImpl.default.encode(packet, OscByteWriter())
        val decoded = OscPacketCodecImpl.default.decode(OscByteReader(bytes))

        assertEquals(packet, decoded)
    }

    @Test
    fun `bundle decode invalid element size should fail`() {
        val writer = OscByteWriter()
        writer.writeString("#bundle")
        writer.writeInt64(1L)
        writer.writeInt32(1024)

        assertFailsWith<OscBufferUnderflowException> {
            OscPacketCodecImpl.default.decode(OscByteReader(writer.toByteArray()))
        }
    }

    @Test
    fun `TFNI tag round-trip`() {
        val message = OscMessage(
            "/x",
            listOf(OscTrue, OscFalse, OscNil, OscInfinitum)
        )

        val bytes = OscPacketCodecImpl.default.encode(message, OscByteWriter())
        val decoded = OscPacketCodecImpl.default.decode(OscByteReader(bytes))

        assertEquals(message, decoded)
    }

    @Test
    fun `int64 float64 tag round-trip`() {
        val message = OscMessage(
            "/x",
            listOf(OscInt64(1L), OscInt64(Long.MAX_VALUE), OscFloat64(1.0), OscFloat64(Double.MAX_VALUE))
        )

        val bytes = OscPacketCodecImpl.default.encode(message, OscByteWriter())
        val decoded = OscPacketCodecImpl.default.decode(OscByteReader(bytes))

        assertEquals(message, decoded)
    }

    @Test
    fun `char tag round-trip`() {
        val message = OscMessage(
            "/x",
            listOf(OscChar('a'), OscChar('b'), OscChar('c'), OscString("other thing"))
        )

        val bytes = OscPacketCodecImpl.default.encode(message, OscByteWriter())
        val decoded = OscPacketCodecImpl.default.decode(OscByteReader(bytes))

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

        val bytes = OscPacketCodecImpl.default.encode(message, OscByteWriter())
        val decoded = OscPacketCodecImpl.default.decode(OscByteReader(bytes))

        assertEquals(message, decoded)
    }

    @Test
    fun `Timetag boundary test`() {
        val message = OscMessage("/x", listOf(Clock.System.now().toOscTimetag(), OscTimetag.Companion.IMMEDIATELY))

        val bytes = OscPacketCodecImpl.default.encode(message, OscByteWriter())
        val decoded = OscPacketCodecImpl.default.decode(OscByteReader(bytes))

        assertEquals(message, decoded)
    }

    @Test
    fun `decode should fail on trailing bytes when codec strict is true`() {
        val message = OscMessage("/x", listOf(OscInt32(1)))
        val bytes = OscPacketCodecImpl.default.encode(message, OscByteWriter())
        val withTrailing = bytes + 0x00
        val config = OscConfig.Codec(strictCodecPayloadConsumption = true)

        assertFailsWith<OscCodecException> {
            OscPacketCodecImpl(config).decode(OscByteReader(withTrailing))
        }
    }

    @Test
    fun `decode should ignore trailing bytes when codec strict is false`() {
        val message = OscMessage("/x", listOf(OscInt32(1)))
        val bytes = OscPacketCodecImpl.default.encode(message, OscByteWriter())
        val withTrailing = bytes + 0x00
        val config = OscConfig.Codec(strictCodecPayloadConsumption = false)

        val decoded = OscPacketCodecImpl(config).decode(OscByteReader(withTrailing))
        assertEquals(message, decoded)

    }
}

