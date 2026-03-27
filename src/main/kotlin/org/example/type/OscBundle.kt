package org.example.type

import org.example.arg.OscTimetag

public data class OscBundle(
    val timeTag: OscTimetag,
    val elements: List<OscPacket> = emptyList()
) : OscPacket {
    public fun isImmediately(): Boolean = timeTag.isImmediately()
}