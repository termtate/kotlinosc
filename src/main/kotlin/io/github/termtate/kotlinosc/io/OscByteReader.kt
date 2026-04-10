package io.github.termtate.kotlinosc.io

import io.github.termtate.kotlinosc.codec.CHAR_BYTES
import io.github.termtate.kotlinosc.codec.FLOAT32_BYTES
import io.github.termtate.kotlinosc.codec.FLOAT64_BYTES
import io.github.termtate.kotlinosc.codec.INT32_BYTES
import io.github.termtate.kotlinosc.codec.INT64_BYTES
import io.github.termtate.kotlinosc.type.MIDI
import io.github.termtate.kotlinosc.codec.MIDI_BYTES
import io.github.termtate.kotlinosc.type.RGBA
import io.github.termtate.kotlinosc.codec.RGBA_BYTES
import io.github.termtate.kotlinosc.exception.OscBufferUnderflowException
import io.github.termtate.kotlinosc.exception.OscPacketIllegalBytesException
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class OscByteReader(private val data: ByteArray, offset: Int = 0, length: Int = data.size) {
    private val byteBuffer: ByteBuffer =
        ByteBuffer.wrap(data)
            .order(ByteOrder.BIG_ENDIAN)
            .position(offset)
            .limit(offset + length)

    init {
        require(offset >= 0 && length >= 0) { "offset and length should be positive" }
        require(offset + length <= data.size) { "offset + length cannot be greater than packet.size" }
    }

    fun readInt32(): Int {
        if (byteBuffer.remaining() < INT32_BYTES) {
            throw OscBufferUnderflowException("OSC-int padding overflow at ${byteBuffer.position()}")
        }
        return byteBuffer.int
    }

    fun readFloat32(): Float {
        if (byteBuffer.remaining() < FLOAT32_BYTES) {
            throw OscBufferUnderflowException("OSC-float padding overflow at ${byteBuffer.position()}")
        }
        return byteBuffer.float
    }

    fun readInt64(): Long {
        if (byteBuffer.remaining() < INT64_BYTES) {
            throw OscBufferUnderflowException("OSC-int64 overflow at ${byteBuffer.position()}")
        }
        return byteBuffer.long
    }

    fun readFloat64(): Double {
        if (byteBuffer.remaining() < FLOAT64_BYTES) {
            throw OscBufferUnderflowException("OSC-int64 overflow at ${byteBuffer.position()}")
        }
        return byteBuffer.double
    }

    fun readString(): String {
        val start = byteBuffer.position()

        while (byteBuffer.hasRemaining() && byteBuffer.get(byteBuffer.position()) != 0.toByte()) {
            byteBuffer.position(byteBuffer.position() + 1)
        }

        if (!byteBuffer.hasRemaining()) {
            throw OscBufferUnderflowException("Unterminated OSC-string at $start")
        }

        val strBytes = ByteArray(byteBuffer.position() - start)
        byteBuffer.position(start)
        byteBuffer.get(strBytes)
        byteBuffer.get() // consume 0

        // align to 4
        val consumed = byteBuffer.position() - start
        val padded = (consumed + 3) and -4
        val paddingToSkip = padded - consumed

        if (byteBuffer.remaining() < paddingToSkip) {
            throw OscBufferUnderflowException("Unterminated OSC-string at $start")
        }
        byteBuffer.position(start + padded)

        return String(strBytes, Charsets.UTF_8)
    }

    fun readBlob(): ByteArray {
        val size = readInt32()
        if (size < 0) {
            throw OscPacketIllegalBytesException("osc-blob size cannot be less than zero")
        }

        val padded = (size.toLong() + 3L) and -4L

        if (padded > byteBuffer.remaining().toLong()) {
            throw OscBufferUnderflowException("OSC-blob padding overflow at ${byteBuffer.position()}")
        }
        val blobBytes = ByteArray(size)
        byteBuffer.get(blobBytes)
        byteBuffer.position(byteBuffer.position() + (padded - size.toLong()).toInt())
        return blobBytes
    }


    fun readChar(): Int {
        if (byteBuffer.remaining() < CHAR_BYTES) {
            throw OscBufferUnderflowException("OSC-char overflow at ${byteBuffer.position()}")
        }
        return byteBuffer.int
    }

    fun readRGBA(): RGBA {
        if (byteBuffer.remaining() < RGBA_BYTES) {
            throw OscBufferUnderflowException("OSC-rgba overflow at ${byteBuffer.position()}")
        }
        return RGBA(
            byteBuffer.get().toUByte(),
            byteBuffer.get().toUByte(),
            byteBuffer.get().toUByte(),
            byteBuffer.get().toUByte(),
        )
    }

    fun readMIDI(): MIDI {
        if (byteBuffer.remaining() < MIDI_BYTES) {
            throw OscBufferUnderflowException("OSC-midi overflow at ${byteBuffer.position()}")
        }
        return MIDI(
            byteBuffer.get().toUByte(),
            byteBuffer.get().toUByte(),
            byteBuffer.get().toUByte(),
            byteBuffer.get().toUByte(),
        )
    }


    fun readSizedPacketReader(): OscByteReader {
        val size = readInt32()
        if (size <= 0) {
            throw OscPacketIllegalBytesException("OSC-packet size must be greater than zero: $size")
        }

        if (byteBuffer.remaining() < size) {
            throw OscBufferUnderflowException("OSC-packet overflow at ${byteBuffer.position()} with size $size")
        }
        val start = byteBuffer.position()
        val child = OscByteReader(data, offset = start, length = size)
        byteBuffer.position(start + size)
        return child
    }

    fun hasRemaining(): Boolean = byteBuffer.hasRemaining()

    fun remaining(): Int = byteBuffer.remaining()

    fun peekString(): String {
        val pos = byteBuffer.position()
        val result = readString()
        byteBuffer.position(pos)
        return result
    }
}

