package io.github.termtate.kotlinosc.type

import io.github.termtate.kotlinosc.arg.OscTimetag

/**
 * OSC bundle packet.
 *
 * A bundle contains a timetag and a sequence of nested [OscPacket] elements.
 */
public data class OscBundle(
    val timeTag: OscTimetag,
    val elements: List<OscPacket> = emptyList()
) : OscPacket {
    /**
     * Returns `true` when [timeTag] means immediate dispatch.
     */
    public fun isImmediately(): Boolean = timeTag.isImmediately()
}
