package io.github.termtate.kotlinosc.transport.tcp.codec.decoder

import io.github.termtate.kotlinosc.transport.tcp.codec.GrowableByteBuffer

internal class LengthPrefixedOscFrameDecoder : OscFrameDecoder() {
    private val buffer = GrowableByteBuffer()

    override fun feed(bytes: ByteArray, length: Int): List<ByteArray> {
        buffer.append(bytes, length)

        val result = mutableListOf<ByteArray>()
        while (true) {
            if (buffer.size < LENGTH_PREFIXED_BYTE_LENGTH) {
                break
            }

            val packetLength = buffer.peekInt32()
            if (buffer.size < LENGTH_PREFIXED_BYTE_LENGTH + packetLength) {
                break
            }

            buffer.drop(LENGTH_PREFIXED_BYTE_LENGTH)

            val payload = buffer.take(packetLength)
            buffer.drop(packetLength)

            result += payload
        }

        return result
    }

    override fun reset() {
        buffer.reset()
    }

    private companion object {
        private const val LENGTH_PREFIXED_BYTE_LENGTH = 4
    }
}
