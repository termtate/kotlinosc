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

    override fun toString(): String = buildString {
        appendBundle(this@OscBundle, indentLevel = 0)
    }
}

private fun StringBuilder.appendBundle(bundle: OscBundle, indentLevel: Int) {
    val indent = "  ".repeat(indentLevel)
    append(indent)
    append("OscBundle(timeTag=")
    append(if (bundle.isImmediately()) "IMMEDIATELY" else bundle.timeTag)

    if (bundle.elements.isEmpty()) {
        append(", elements=[])")
        return
    }

    append(", elements=[\n")
    bundle.elements.forEachIndexed { index, element ->
        when (element) {
            is OscMessage -> append("  ".repeat(indentLevel + 1)).append(element)
            is OscBundle -> appendBundle(element, indentLevel + 1)
        }
        if (index != bundle.elements.lastIndex) {
            append(",")
        }
        append("\n")
    }
    append(indent)
    append("])")
}
