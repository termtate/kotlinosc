package io.github.termtate.kotlinosc.codec

import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.type.OscPacket
import io.github.termtate.kotlinosc.io.OscByteReader
import io.github.termtate.kotlinosc.io.OscByteWriter

/**
 * Encodes an [OscPacket] into a [ByteArray].
 */
public fun OscPacket.encodeToByteArray(
    config: OscConfig.Codec? = null
): ByteArray {
    val codec = config?.let { OscPacketCodecImpl(config) } ?: OscPacketCodecImpl.default

    return codec.encode(this, OscByteWriter())
}

/**
 * Decodes an [OscPacket] from a [ByteArray].
 */
public fun OscPacket.Companion.decodeFromByteArray(
    data: ByteArray,
    offset: Int = 0,
    length: Int = data.size,
    config: OscConfig.Codec? = null
): OscPacket {
    val codec = config?.let { OscPacketCodecImpl(config) } ?: OscPacketCodecImpl.default

    return codec.decode(OscByteReader(data, offset, length))
}
