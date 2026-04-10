package io.github.termtate.kotlinosc.type

import io.github.termtate.kotlinosc.arg.OscArg
import io.github.termtate.kotlinosc.arg.OscTimetag
import io.github.termtate.kotlinosc.arg.toOscArg
import io.github.termtate.kotlinosc.util.OscDsl
import kotlin.jvm.JvmName

/**
 * Builder for [OscBundle] DSL.
 */
@OscDsl
public class OscBundleBuilder(public val timetag: OscTimetag) {
    private val packets = mutableListOf<OscPacket>()

    /**
     * Adds one [OscMessage] using pre-boxed [OscArg] list.
     */
    @JvmName("messageOscArgList")
    public fun message(address: String, args: List<OscArg>) {
        packets += OscMessage(address, args)
    }

    /**
     * Adds one [OscMessage] using pre-boxed [OscArg] vararg.
     */
    public fun message(address: String, vararg args: OscArg) {
        packets += OscMessage(address, args.toList())
    }

    /**
     * Adds one [OscMessage] and boxes Kotlin values to [OscArg].
     *
     * Supported mappings are the same as [OscMessage.Companion.invoke] with `List<Any?>`.
     */
    public fun message(address: String, args: List<Any?>) {
        packets += OscMessage(address, args.map { it.toOscArg() })
    }

    /**
     * Adds one [OscMessage] and boxes Kotlin values to [OscArg].
     */
    public fun message(address: String, vararg args: Any?) {
        packets += OscMessage(address, args.map { it.toOscArg() })
    }

    /**
     * Adds one nested [OscBundle].
     */
    public fun bundle(timetag: OscTimetag = OscTimetag.IMMEDIATELY, builder: OscBundleBuilder.() -> Unit) {
        packets += oscBundle(timetag, builder)
    }

    internal fun build(): OscBundle = OscBundle(timetag, packets.toList())
}

/**
 * Builds an [OscBundle] with DSL.
 *
 * Example:
 * ```kotlin
 * val packet = oscBundle {
 *     message("/a", 1, "x")
 *     bundle {
 *         message("/b", true)
 *     }
 * }
 * ```
 */
public fun oscBundle(
    timetag: OscTimetag = OscTimetag.IMMEDIATELY,
    builder: OscBundleBuilder.() -> Unit
): OscBundle = OscBundleBuilder(timetag).apply(builder).build()
