package org.example.arg

import org.example.type.MIDI
import org.example.type.RGBA
import kotlin.time.Instant

public fun Int.toOscInt32(): OscInt32 = OscInt32(this)

public fun Long.toOscInt64(): OscInt64 = OscInt64(this)

public fun Float.toOscFloat32(): OscFloat32 = OscFloat32(this)

public fun Double.toOscFloat64(): OscFloat64 = OscFloat64(this)

public fun Char.toOscChar(): OscChar = OscChar(this)

public fun String.toOscString(): OscString = OscString(this)

public fun String.toOscSymbol(): OscSymbol = OscSymbol(this)

public fun Boolean.toOscBoolean(): OscArg = if (this) OscTrue else OscFalse

public fun ByteArray.toOscBlob(): OscBlob = OscBlob(this)

public fun MIDI.toOscMIDI(): OscMIDI = OscMIDI(this)

public fun RGBA.toOscRGBA(): OscRGBA = OscRGBA(this)

public fun OscTimetag.toInstant(): Instant {
    val secondNtp = value shr 32
    val secondUnix = secondNtp.toLong() - NTP_UNIX_EPOCH_DIFF_SECONDS
    val fraction = value and UINT32_MAX
    val nanosecond = (fraction * NANOS_PER_SECOND.toULong()) shr 32

    return Instant.Companion.fromEpochSeconds(secondUnix, nanosecond.toLong())
}

private const val NTP_UNIX_EPOCH_DIFF_SECONDS = 2_208_988_800L
private const val NANOS_PER_SECOND = 1_000_000_000
private const val UINT32_MAX = 0xFFFF_FFFFuL

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

public fun ULong.toOscTimetag(): OscTimetag = OscTimetag(this)

public fun Long.toOscTimetag(): OscTimetag {
    require(this >= 0) { "OSC timetag cannot be negative: $this" }
    return OscTimetag(this.toULong())
}

public fun List<OscArg>.toOscArray(): OscArray = OscArray(this)
