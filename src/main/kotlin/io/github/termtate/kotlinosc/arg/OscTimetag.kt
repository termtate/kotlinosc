package io.github.termtate.kotlinosc.arg

/**
 * OSC timetag value.
 *
 * Backed by 64-bit NTP fixed-point format:
 * - high 32 bits: seconds since 1900-01-01
 * - low 32 bits: fractional second
 *
 * Special value [IMMEDIATELY] indicates "execute as soon as possible".
 */
public data class OscTimetag(val value: ULong) : OscArg, Comparable<OscTimetag> {
    override val tag: String
        get() = TYPE_TAG.toString()

    /**
     * Returns `true` if this timetag is the OSC "immediately" sentinel.
     */
    public fun isImmediately(): Boolean = value == IMMEDIATELY_VALUE

    /**
     * Raw 64-bit NTP timetag value.
     */
    public fun toULong(): ULong = value

    /**
     * Raw 64-bit NTP timetag value converted to [Long].
     */
    public fun toLong(): Long = value.toLong()

    override fun compareTo(other: OscTimetag): Int {
        return value.compareTo(other.value)
    }

    public companion object {
        internal const val TYPE_TAG: Char = 't'
        /**
         * OSC special timetag value for immediate execution.
         */
        public val IMMEDIATELY: OscTimetag = OscTimetag(IMMEDIATELY_VALUE)

        private const val IMMEDIATELY_VALUE = 1uL
    }
}

