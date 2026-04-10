package io.github.termtate.kotlinosc.arg

import io.github.termtate.kotlinosc.type.MIDI
import io.github.termtate.kotlinosc.type.RGBA
import kotlin.time.Instant

/** Converts [Int] to [OscInt32]. */
public fun Int.toOscInt32(): OscInt32 = OscInt32(this)

/** Converts [Long] to [OscInt64]. */
public fun Long.toOscInt64(): OscInt64 = OscInt64(this)

/** Converts [Float] to [OscFloat32]. */
public fun Float.toOscFloat32(): OscFloat32 = OscFloat32(this)

/** Converts [Double] to [OscFloat64]. */
public fun Double.toOscFloat64(): OscFloat64 = OscFloat64(this)

/** Converts [Char] to [OscChar]. */
public fun Char.toOscChar(): OscChar = OscChar(this)

/** Converts [String] to [OscString]. */
public fun String.toOscString(): OscString = OscString(this)

/** Converts [String] to [OscSymbol]. */
public fun String.toOscSymbol(): OscSymbol = OscSymbol(this)

/** Converts [Boolean] to [OscTrue] or [OscFalse]. */
public fun Boolean.toOscBoolean(): OscArg = if (this) OscTrue else OscFalse

/** Converts [ByteArray] to [OscBlob]. */
public fun ByteArray.toOscBlob(): OscBlob = OscBlob(this)

/** Converts [MIDI] to [OscMIDI]. */
public fun MIDI.toOscMIDI(): OscMIDI = OscMIDI(this)

/** Converts [RGBA] to [OscRGBA]. */
public fun RGBA.toOscRGBA(): OscRGBA = OscRGBA(this)

/**
 * Converts OSC NTP timetag to [Instant].
 *
 * @throws IllegalArgumentException if this timetag is [OscTimetag.IMMEDIATELY],
 * because it is a sentinel rather than an absolute timestamp.
 */
public fun OscTimetag.toInstant(): Instant {
    require(!isImmediately()) {
        "OscTimetag.IMMEDIATELY cannot be converted to Instant. Handle IMMEDIATELY separately."
    }
    val secondNtp = value shr 32
    val secondUnix = secondNtp.toLong() - NTP_UNIX_EPOCH_DIFF_SECONDS
    val fraction = value and UINT32_MAX
    val nanosecond = (fraction * NANOS_PER_SECOND.toULong()) shr 32

    return Instant.Companion.fromEpochSeconds(secondUnix, nanosecond.toLong())
}

private const val NTP_UNIX_EPOCH_DIFF_SECONDS = 2_208_988_800L
private const val NANOS_PER_SECOND = 1_000_000_000
private const val UINT32_MAX = 0xFFFF_FFFFuL

/**
 * Converts [Instant] to OSC NTP timetag.
 *
 * @throws IllegalArgumentException when epoch seconds are out of OSC NTP range.
 */
public fun Instant.toOscTimetag(): OscTimetag {
    val nanosecond = nanosecondsOfSecond.toULong()
    val secondNtp = epochSeconds + NTP_UNIX_EPOCH_DIFF_SECONDS
    require(secondNtp in 0..UINT32_MAX.toLong()) {
        "Instant out of OSC timetag range: epochSeconds=$epochSeconds"
    }
    val fraction = ((nanosecond shl 32) / NANOS_PER_SECOND.toULong()) and UINT32_MAX
    val ntp64b = (secondNtp.toULong() shl 32) + fraction

    return OscTimetag(ntp64b)
}

/** Wraps raw NTP timetag value as [OscTimetag]. */
public fun ULong.toOscTimetag(): OscTimetag = OscTimetag(this)

/**
 * Wraps non-negative raw timetag value as [OscTimetag].
 *
 * @throws IllegalArgumentException when value is negative.
 */
public fun Long.toOscTimetag(): OscTimetag {
    require(this >= 0) { "OSC timetag cannot be negative: $this" }
    return OscTimetag(this.toULong())
}

/** Converts argument list to [OscArray]. */
public fun List<OscArg>.toOscArray(): OscArray = OscArray(this)

/** Converts argument array to [OscArray]. */
public fun Array<OscArg>.toOscArray(): OscArray = OscArray(this.toList())

internal fun Any?.toOscArg(): OscArg {
    return when (this) {
        is Int -> OscInt32(this)
        is Long -> OscInt64(this)
        is Float -> OscFloat32(this)
        is Double -> OscFloat64(this)
        is String -> OscString(this)
        is Char -> OscChar(this)
        is Boolean -> this.toOscBoolean()
        is ByteArray -> OscBlob(this)
        null -> OscNil
        is MIDI -> OscMIDI(this)
        is RGBA -> OscRGBA(this)
        is Instant -> this.toOscTimetag()
        is List<*> -> this.map { it.toOscArg() }.toOscArray()
        is Array<*> -> this.map { it.toOscArg() }.toOscArray()
        is OscArg -> this
        else -> throw IllegalArgumentException("Unsupported type conversion: $this(${this::class}) -> OscArg")
    }
}

