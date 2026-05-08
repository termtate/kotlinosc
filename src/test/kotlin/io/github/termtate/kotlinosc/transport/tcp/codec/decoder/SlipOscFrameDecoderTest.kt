package io.github.termtate.kotlinosc.transport.tcp.codec.decoder

import io.github.termtate.kotlinosc.exception.OscFrameException
import io.github.termtate.kotlinosc.transport.tcp.codec.OscFrameCodec.Companion.SLIP_END
import io.github.termtate.kotlinosc.transport.tcp.codec.OscFrameCodec.Companion.SLIP_ESC
import io.github.termtate.kotlinosc.transport.tcp.codec.OscFrameCodec.Companion.SLIP_ESC_END
import io.github.termtate.kotlinosc.transport.tcp.codec.OscFrameCodec.Companion.SLIP_ESC_ESC
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SlipOscFrameDecoderTest {
    private val Char.b: Byte get() = code.toByte()

    @Test
    fun `slip decode basic test`() {
        val decoder = SlipOscFrameDecoder()

        assertContentEquals(
            decoder.feed(byteArrayOf(SLIP_END, 'a'.b, SLIP_END)).first(),
            byteArrayOf('a'.code.toByte())
        )

        assertEquals(
            decoder.feed(byteArrayOf(SLIP_END, 'a'.b)),
            emptyList()
        )

        assertContentEquals(
            decoder.feed(byteArrayOf(SLIP_END)).first(),
            byteArrayOf('a'.code.toByte())
        )

        assertContentEquals(
            decoder.feed(byteArrayOf(SLIP_END, SLIP_ESC, SLIP_ESC_END, SLIP_END)).first(),
            byteArrayOf(SLIP_END)
        )

        assertEquals(
            decoder.feed(byteArrayOf(SLIP_END, SLIP_ESC)),
            emptyList()
        )

        assertContentEquals(
            decoder.feed(byteArrayOf(SLIP_ESC_ESC, SLIP_END)).first(),
            byteArrayOf(SLIP_ESC)
        )

        assertFailsWith<OscFrameException> {
            decoder.feed(byteArrayOf(SLIP_END, SLIP_ESC, 0x01, SLIP_END))
        }
    }

    @Test
    fun `slip decode multiple frames in one feed`() {
        val decoder = SlipOscFrameDecoder()

        val frames = decoder.feed(byteArrayOf(SLIP_END, 'a'.b, SLIP_END, SLIP_END, 'b'.b, SLIP_END))

        assertEquals(2, frames.size)
        assertContentEquals(byteArrayOf('a'.b), frames[0])
        assertContentEquals(byteArrayOf('b'.b), frames[1])
    }

    @Test
    fun `slip decoder reset clears partial frame state`() {
        val decoder = SlipOscFrameDecoder()

        assertEquals(emptyList(), decoder.feed(byteArrayOf(SLIP_END, 'a'.b)))

        decoder.reset()

        assertContentEquals(
            byteArrayOf('b'.b),
            decoder.feed(byteArrayOf(SLIP_END, 'b'.b, SLIP_END)).first()
        )
    }

    @Test
    fun `slip decoder rejects empty frame`() {
        val decoder = SlipOscFrameDecoder()

        assertFailsWith<OscFrameException> {
            decoder.feed(byteArrayOf(SLIP_END, SLIP_END))
        }
    }

    @Test
    fun `slip decoder ignores bytes outside frame`() {
        val decoder = SlipOscFrameDecoder()

        val frames = decoder.feed(byteArrayOf('x'.b, 'y'.b, SLIP_END, 'a'.b, SLIP_END, 'z'.b))

        assertEquals(1, frames.size)
        assertContentEquals(byteArrayOf('a'.b), frames.first())
    }

    @Test
    fun `slip decoder fails when frame exceeds max frame size`() {
        val decoder = SlipOscFrameDecoder(maxFrameSize = 3)

        assertFailsWith<OscFrameException> {
            decoder.feed(byteArrayOf(SLIP_END, 'a'.b, 'b'.b, 'c'.b, 'd'.b))
        }
    }
}
