package org.example.codec

import org.example.type.OscPacket
import org.example.io.OscByteReader
import org.example.io.OscByteWriter

/**
 * [OscPacket]编码为[ByteArray]的便捷方法
 */
public fun OscPacket.encodeToByteArray(): ByteArray = OscPacketCodecImpl.encode(this, OscByteWriter())

/**
 * [ByteArray]解码为[OscPacket]的便捷方法
 */
public fun OscPacket.Companion.decodeFromByteArray(
    data: ByteArray,
    offset: Int = 0,
    length: Int = data.size
): OscPacket = OscPacketCodecImpl.decode(OscByteReader(data, offset, length))
