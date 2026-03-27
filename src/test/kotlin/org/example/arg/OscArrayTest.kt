package org.example.arg

import org.example.type.OscMessage
import org.example.codec.OscMessageCodec
import org.example.exception.OscArrayTagParseException
import org.example.exception.OscUnknownTypeTagException
import org.example.io.OscByteReader
import org.example.io.OscByteWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock

class OscArrayTest {
    @Test
    fun `osc array round-trip test`() {
        val message = OscMessage(
            address = "/x",
            args = listOf(OscInt32(1), OscArray(OscFloat32(2.0f), OscTrue), OscFalse, OscString("other thing"))
        )

        val bytes = OscMessageCodec.encode(message, OscByteWriter())
        val decoded = OscMessageCodec.decode(OscByteReader(bytes))

        assertEquals(message, decoded)
    }

    @Test
    fun `OscArray accepts empty array`() {
        val message = OscMessage(
            address = "/x",
            args = listOf(OscArray())
        )

        val bytes = OscMessageCodec.encode(message, OscByteWriter())
        val decoded = OscMessageCodec.decode(OscByteReader(bytes))

        assertEquals(message, decoded)
    }

    @Test
    fun `Parentheses should match and be closed`() {
        OscByteWriter().apply {
            writeString("/x")
            writeString(",T[F")
        }.run {
            assertFailsWith<OscArrayTagParseException> {
                OscMessageCodec.decode(OscByteReader(toByteArray()))
            }
        }

        OscByteWriter().apply {
            writeString("/x")
            writeString(",T][F")
        }.run {
            assertFailsWith<OscArrayTagParseException> {
                OscMessageCodec.decode(OscByteReader(toByteArray()))
            }
        }

        OscByteWriter().apply {
            writeString("/x")
            writeString(",TF]")
        }.run {
            assertFailsWith<OscArrayTagParseException> {
                OscMessageCodec.decode(OscByteReader(toByteArray()))
            }
        }

        OscByteWriter().apply {
            writeString("/x")
            writeString(",T[]]F")
        }.run {
            assertFailsWith<OscArrayTagParseException> {
                OscMessageCodec.decode(OscByteReader(toByteArray()))
            }
        }

        OscByteWriter().apply {
            writeString("/x")
            writeString(",T[[]F")
        }.run {
            assertFailsWith<OscArrayTagParseException> {
                OscMessageCodec.decode(OscByteReader(toByteArray()))
            }
        }
    }

    @Test
    fun `Nested osc arrays test`() {
        val message = OscMessage(
            address = "/x",
            args = listOf(
                OscInt32(1),
                OscArray(
                    OscFloat32(2.0f),
                    OscTrue,
                    OscArray(OscNil, OscMIDI(1u, 2u, 3u, 4u))
                ),
                OscFalse,
                OscArray(Clock.System.now().toOscTimetag()),
                OscString("other thing")
            )
        )

        val bytes = OscMessageCodec.encode(message, OscByteWriter())
        val decoded = OscMessageCodec.decode(OscByteReader(bytes))

        assertEquals(message, decoded)
    }

    @Test
    fun `array with unknown type tag should fail`() {
        OscByteWriter().apply {
            writeString("/x")
            writeString(",[z]")
        }.run {
            assertFailsWith<OscUnknownTypeTagException> {
                OscMessageCodec.decode(OscByteReader(toByteArray()))
            }
        }
    }
}
