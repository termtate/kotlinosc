package org.example.arg

import org.example.arg.OscTimetag.Companion.IMMEDIATELY_VALUE

public data class OscTimetag(val value: ULong) : OscArg, Comparable<OscTimetag> {
    override val tag: String
        get() = TYPE_TAG.toString()

    public fun isImmediately(): Boolean = value == IMMEDIATELY_VALUE

    public fun toULong(): ULong = value

    public fun toLong(): Long = value.toLong()

    override fun compareTo(other: OscTimetag): Int {
        return value.compareTo(other.value)
    }

    public companion object {
        internal const val TYPE_TAG: Char = 't'
        public val IMMEDIATELY: OscTimetag = OscTimetag(IMMEDIATELY_VALUE)

        private const val IMMEDIATELY_VALUE = 1uL
    }
}
