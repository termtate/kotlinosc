package io.github.termtate.kotlinosc.transport.tcp.codec.decoder

import io.github.termtate.kotlinosc.io.OscByteWriter
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class LengthPrefixedOscFrameDecoderTest {
    private val Char.b: Byte get() = code.toByte()

    @Test
    fun `length prefixed decode basic test`() {
        val decoder = LengthPrefixedOscFrameDecoder()

        assertContentEquals(
            byteArrayOf('a'.b),
            decoder.feed(frameOf('a'.b)).first()
        )
    }

    @Test
    fun `length prefixed decoder waits for partial header and payload`() {
        val decoder = LengthPrefixedOscFrameDecoder()
        val frame = frameOf('a'.b, 'b'.b)

        assertEquals(emptyList(), decoder.feed(frame.copyOfRange(0, 2)))
        assertEquals(emptyList(), decoder.feed(frame.copyOfRange(2, 5)))

        assertContentEquals(
            byteArrayOf('a'.b, 'b'.b),
            decoder.feed(frame.copyOfRange(5, frame.size)).first()
        )
    }

    @Test
    fun `length prefixed decoder decodes multiple frames in one feed`() {
        val decoder = LengthPrefixedOscFrameDecoder()

        val frames = decoder.feed(frameOf('a'.b) + frameOf('b'.b, 'c'.b))

        assertEquals(2, frames.size)
        assertContentEquals(byteArrayOf('a'.b), frames[0])
        assertContentEquals(byteArrayOf('b'.b, 'c'.b), frames[1])
    }

    @Test
    fun `length prefixed decoder reset clears partial frame state`() {
        val decoder = LengthPrefixedOscFrameDecoder()
        val frame = frameOf('a'.b, 'b'.b)

        assertEquals(emptyList(), decoder.feed(frame.copyOfRange(0, 5)))

        decoder.reset()

        assertContentEquals(
            byteArrayOf('c'.b),
            decoder.feed(frameOf('c'.b)).first()
        )
    }

    private fun frameOf(vararg payload: Byte): ByteArray {
        return OscByteWriter().apply {
            writeInt32(payload.size)
            writeByteArray(payload)
        }.toByteArray()
    }
}
