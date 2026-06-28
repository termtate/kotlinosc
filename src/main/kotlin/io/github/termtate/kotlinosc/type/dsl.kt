package io.github.termtate.kotlinosc.type

import io.github.termtate.kotlinosc.arg.OscArg
import io.github.termtate.kotlinosc.arg.OscTimetag
import io.github.termtate.kotlinosc.arg.toOscArg
import io.github.termtate.kotlinosc.util.OscDsl

/**
 * Builder for [OscBundle] DSL.
 */
@OscDsl
public class OscBundleBuilder(public val timetag: OscTimetag) {
    private val packets = mutableListOf<OscPacket>()

    /**
     * Adds one [OscMessage] and boxes Kotlin values to [OscArg].
     *
     * Supported mappings are the same as [oscMessageOf].
     * A `List` or `Array` value is treated as one OSC array argument.
     */
    public fun message(address: String, vararg args: Any?) {
        packets += OscMessage(address, args.map { it.toOscArg() })
    }

    /**
     * Adds one nested [OscBundle].
     */
    public fun bundle(timetag: OscTimetag = OscTimetag.IMMEDIATELY, builder: OscBundleBuilder.() -> Unit) {
        packets += buildOscBundle(timetag, builder)
    }

    /**
     * Adds a prebuilt [OscPacket] to this bundle.
     *
     * Use this when a message or nested bundle has already been constructed.
     */
    public fun packet(packet: OscPacket) {
        packets += packet
    }

    internal fun build(): OscBundle = OscBundle(timetag, packets.toList())
}
