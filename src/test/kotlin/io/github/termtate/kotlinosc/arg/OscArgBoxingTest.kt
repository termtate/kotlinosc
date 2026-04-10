package io.github.termtate.kotlinosc.arg

import io.github.termtate.kotlinosc.type.MIDI
import io.github.termtate.kotlinosc.type.RGBA
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class OscArgBoxingTest {
    @Test
    fun `toOscArg should map common kotlin primitives to OscArg`() {
        assertEquals(OscInt32(1), 1.toOscArg())
        assertEquals(OscInt64(2L), 2L.toOscArg())
        assertEquals(OscFloat32(3.0f), 3.0f.toOscArg())
        assertEquals(OscFloat64(4.0), 4.0.toOscArg())
        assertEquals(OscString("str"), "str".toOscArg())
        assertEquals(OscChar('a'), 'a'.toOscArg())
        assertEquals(OscTrue, true.toOscArg())
        assertEquals(OscFalse, false.toOscArg())
        assertEquals(OscBlob(byteArrayOf(1, 2, 3)), byteArrayOf(1, 2, 3).toOscArg())
        assertEquals(OscNil, null.toOscArg())
    }

    @Test
    fun `toOscArg should map domain types and preserve OscArg`() {
        val midi = MIDI(1u, 2u, 3u, 4u)
        val rgba = RGBA(5u, 6u, 7u, 8u)
        val instant = Instant.fromEpochSeconds(1_700_000_000, 123_456_789)
        val arg = OscInt32(9)

        assertEquals(OscMIDI(midi), midi.toOscArg())
        assertEquals(OscRGBA(rgba), rgba.toOscArg())
        assertEquals(instant.toOscTimetag(), instant.toOscArg())
        assertEquals(arg, arg.toOscArg())
    }

    @Test
    fun `toOscArg should map list and array recursively to OscArray`() {
        val listArg = listOf(
            1,
            "x",
            listOf(true, null)
        ).toOscArg()
        val arrayArg = arrayOf(
            2L,
            3.0f,
            arrayOf<Any?>("y", false)
        ).toOscArg()

        assertEquals(
            OscArray(
                OscInt32(1),
                OscString("x"),
                OscArray(OscTrue, OscNil)
            ),
            listArg
        )
        assertEquals(
            OscArray(
                OscInt64(2L),
                OscFloat32(3.0f),
                OscArray(OscString("y"), OscFalse)
            ),
            arrayArg
        )
    }

    @Test
    fun `toOscArg should throw for unsupported type`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            object {}.toOscArg()
        }
        assertEquals(ex.message?.contains("Unsupported type conversion"), true)
    }
}

