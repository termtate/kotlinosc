package io.github.termtate.kotlinosc.transport.tcp.codec.decoder

import io.github.termtate.kotlinosc.exception.OscFrameException
import io.github.termtate.kotlinosc.transport.tcp.codec.GrowableByteBuffer
import io.github.termtate.kotlinosc.transport.tcp.codec.OscFrameCodec.Companion.SLIP_END
import io.github.termtate.kotlinosc.transport.tcp.codec.OscFrameCodec.Companion.SLIP_ESC
import io.github.termtate.kotlinosc.transport.tcp.codec.OscFrameCodec.Companion.SLIP_ESC_END
import io.github.termtate.kotlinosc.transport.tcp.codec.OscFrameCodec.Companion.SLIP_ESC_ESC
import io.github.termtate.kotlinosc.util.OscLogger
import io.github.termtate.kotlinosc.util.logger
import java.io.ByteArrayOutputStream

internal class SlipOscFrameDecoder(
    private val maxFrameSize: Int = GrowableByteBuffer.DEFAULT_MAX_FRAME_SIZE
) : OscFrameDecoder(), OscLogger {
    override val logTag: String
        get() = "SlipOscFrameDecoder"

    private val inputBuffer = GrowableByteBuffer(maxFrameSize = maxFrameSize)

    private val frameBuffer = LimitedByteArrayOutputStream(maxFrameSize)

    private var inFrame = false
    private var escaping = false

    override fun feed(bytes: ByteArray, length: Int): List<ByteArray> {
        inputBuffer.append(bytes, length)

        val frames = mutableListOf<ByteArray>()
        while (true) {
            val frame = tryDecodeFrame() ?: break
            frames += frame
        }

        return frames
    }

    private fun tryDecodeFrame(): ByteArray? {
        for ((i, b) in inputBuffer.withIndex()) {
            when {
                !inFrame && b == SLIP_END -> {
                    inFrame = true
                    frameBuffer.reset()
                }

                inFrame && escaping -> {
                    when (b) {
                        SLIP_ESC_END -> frameBuffer.write(SLIP_END.toInt())
                        SLIP_ESC_ESC -> frameBuffer.write(SLIP_ESC.toInt())
                        else -> throw OscFrameException("invalid escape")
                    }
                    escaping = false
                }

                inFrame && b == SLIP_ESC -> {
                    escaping = true
                }

                inFrame && b == SLIP_END -> {
                    // TODO Consider making empty SLIP frames configurable: ignore or reject.
                    if (frameBuffer.size() == 0) {
                        throw OscFrameException("Empty slip frame")
                    }
                    return frameBuffer.toByteArray().also {
                        frameBuffer.reset()
                        inFrame = false
                        escaping = false
                        inputBuffer.drop(i + 1)
                    }
                }

                inFrame -> {
                    frameBuffer.write(b.toInt())
                }

                else -> {
                    // TODO Decide whether to ignore bytes outside a frame or treat them as an error.
                    if (logger.isTraceEnabled()) {
                        logger.trace { "byte out of frame: $b" }
                    }
                }
            }
        }

        return null.also { inputBuffer.drop(inputBuffer.size) }
    }

    override fun reset() {
        inputBuffer.reset()
        frameBuffer.reset()
        inFrame = false
        escaping = false
    }

    private class LimitedByteArrayOutputStream(
        private val maxFrameSize: Int
    ) : ByteArrayOutputStream() {
        override fun write(b: Int) {
            ensureCapacity(1)
            super.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            ensureCapacity(len)
            super.write(b, off, len)
        }

        private fun ensureCapacity(extra: Int) {
            if (size() + extra > maxFrameSize) {
                throw OscFrameException("SLIP frame exceeded maxFrameSize: $maxFrameSize")
            }
        }
    }
}
