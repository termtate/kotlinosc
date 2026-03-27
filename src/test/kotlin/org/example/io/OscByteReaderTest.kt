package org.example.io

import org.example.exception.OscBufferUnderflowException
import org.example.exception.OscPacketIllegalBytesException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class OscByteReaderTest {
    @Test
    fun `readBlob supports zero length blob`() {
        val writer = OscByteWriter().apply {
            writeInt32(0)
        }

        val blob = OscByteReader(writer.toByteArray()).readBlob()

        assertContentEquals(byteArrayOf(), blob)
    }

    @Test
    fun `readBlob throws when blob size is negative`() {
        val writer = OscByteWriter().apply {
            writeInt32(-1)
        }

        assertFailsWith<OscPacketIllegalBytesException> {
            OscByteReader(writer.toByteArray()).readBlob()
        }
    }

    @Test
    fun `readBlob throws when declared payload exceeds remaining bytes`() {
        val writer = OscByteWriter().apply {
            writeInt32(4)
            writeInt32(1)
        }
        val packet = writer.toByteArray().copyOf(7)

        assertFailsWith<OscBufferUnderflowException> {
            OscByteReader(packet).readBlob()
        }
    }

    @Test
    fun `readBlob throws on padded size int overflow case`() {
        val writer = OscByteWriter().apply {
            writeInt32(Int.MAX_VALUE)
        }

        assertFailsWith<OscBufferUnderflowException> {
            OscByteReader(writer.toByteArray()).readBlob()
        }
    }
}
