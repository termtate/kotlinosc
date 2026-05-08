package io.github.termtate.kotlinosc.transport.tcp.codec.decoder

internal sealed class OscFrameDecoder {
    abstract fun feed(bytes: ByteArray, length: Int = bytes.size): List<ByteArray>

    abstract fun reset()
}
