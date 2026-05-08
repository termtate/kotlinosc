package io.github.termtate.kotlinosc.transport.tcp.codec

import io.github.termtate.kotlinosc.exception.OscFrameException

internal class GrowableByteBuffer(
    val initialCapacity: Int = DEFAULT_INITIAL_CAPACITY,
    val maxFrameSize: Int = DEFAULT_MAX_FRAME_SIZE
) : Iterable<Byte> {
    private var buffer = ByteArray(initialCapacity)
    private var start = 0
    private var end = 0

    val size: Int get() = end - start

    val indices: IntRange = start until end

    fun append(array: ByteArray, length: Int = array.size) {
        require(length >= 0) { "length must not be negative: $length" }
        require(length <= array.size) { "length must not exceed array size: $length > ${array.size}" }

        ensureCapacity(length)

        for (i in end until end + length) {
            buffer[i] = array[i - end]
        }

        end += length
    }

    fun take(count: Int): ByteArray {
        require(count <= size) { "count is great than buffer's size" }
        return buffer.copyOfRange(start, start + count)
    }

    fun take(startIndex: Int, endIndex: Int): ByteArray {
        require(startIndex <= size) { "startIndex is great than buffer's size" }
        require(endIndex <= size) { "startIndex is great than buffer's size" }
        require(startIndex <= endIndex) { "startIndex is great than endIndex" }

        return buffer.copyOfRange(start + startIndex, start + endIndex)
    }

    fun drop(count: Int) {
        require(count <= size) { "count is great than buffer's size" }
        start += count
    }

    fun peek(index: Int): Byte {
        require(index >= 0) { "index must not be negative: $index" }
        require(index < size) { "index is greater than or equal to buffer size" }
        return buffer[index + start]
    }

    fun peekInt32(startIndex: Int = 0): Int {
        require(startIndex >= 0) { "startIndex must not be negative: $startIndex" }
        require(startIndex + Int.SIZE_BYTES <= size) { "not enough bytes to read Int32 from buffer" }

        return ((peek(startIndex).toInt() and 0xFF) shl 24) or
            ((peek(startIndex + 1).toInt() and 0xFF) shl 16) or
            ((peek(startIndex + 2).toInt() and 0xFF) shl 8) or
            (peek(startIndex + 3).toInt() and 0xFF)
    }

    operator fun get(index: Int): Byte = peek(index)

    fun getOrNull(index: Int): Byte? = if (index in 0..<size) buffer[index + start] else null

    operator fun set(index: Int, value: Byte) {
        require(index >= 0) { "index must not be negative: $index" }
        require(index < size) { "index is greater than or equal to buffer size" }
        buffer[index + start] = value
    }

    private fun ensureCapacity(extra: Int) {
        // We don't need to extend if remaining space is large enough.
        if (buffer.size - end >= extra) {
            return
        }

        // Reuse free space at the head before growing the backing array.
        if (size + extra <= buffer.size) {
            compact()
        } else {
            var newCapacity = maxOf(buffer.size * 2, size + extra)

            if (newCapacity > maxFrameSize) {
                if (size + extra <= maxFrameSize) {
                    newCapacity = size + extra
                } else {
                    throw OscFrameException("GrowableByteBuffer overflowed. maxFrameSize: $maxFrameSize, current frame size: $newCapacity")
                }
            }

            val newBuffer = ByteArray(newCapacity)

            for (i in 0 until size) {
                newBuffer[i] = buffer[start + i]
            }
            buffer = newBuffer
            end = size
            start = 0
        }
    }

    private fun compact() {
        for (i in start until end) {
            buffer[i - start] = buffer[i]
        }

        end = size
        start = 0
    }

    fun reset() {
        start = 0
        end = 0
        buffer = ByteArray(initialCapacity)
    }

    override fun iterator(): Iterator<Byte> = object : Iterator<Byte> {
        private var i = start

        override fun next(): Byte = buffer[i++]

        override fun hasNext(): Boolean = i < end

    }

    companion object {
        internal const val DEFAULT_INITIAL_CAPACITY: Int = 1024
        internal const val DEFAULT_MAX_FRAME_SIZE: Int = 65507
    }
}