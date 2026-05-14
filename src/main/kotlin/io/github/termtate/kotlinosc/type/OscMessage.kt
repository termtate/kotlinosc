package io.github.termtate.kotlinosc.type

import io.github.termtate.kotlinosc.arg.OscArg
import io.github.termtate.kotlinosc.arg.toOscArg

/**
 * OSC message conforming to OSC Specification 1.0.
 *
 * Use this constructor when arguments are already boxed as [OscArg] values.
 * Use [oscMessageOf] when building a message from regular Kotlin values.
 *
 * @property address OSC address string (for example `/foo/bar`).
 * @property args Message arguments encoded as [OscArg].
 */
public data class OscMessage(
    val address: String,
    val args: List<OscArg> = emptyList()
) : OscPacket


/**
 * Builds an [OscMessage] from Kotlin values by boxing each item to [OscArg].
 *
 * Supported mappings include:
 * - [Int] -> [OscInt32][io.github.termtate.kotlinosc.arg.OscInt32]
 * - [Long] -> [OscInt64][io.github.termtate.kotlinosc.arg.OscInt64]
 * - [Float] -> [OscFloat32][io.github.termtate.kotlinosc.arg.OscFloat32]
 * - [Double] -> [OscFloat64][io.github.termtate.kotlinosc.arg.OscFloat64]
 * - [String] -> [OscString][io.github.termtate.kotlinosc.arg.OscString]
 * - [Char] -> [OscChar][io.github.termtate.kotlinosc.arg.OscChar]
 * - [Boolean] -> [OscTrue][io.github.termtate.kotlinosc.arg.OscTrue] / [OscFalse][io.github.termtate.kotlinosc.arg.OscFalse]
 * - [ByteArray] -> [OscBlob][io.github.termtate.kotlinosc.arg.OscBlob]
 * - `null` -> [OscNil][io.github.termtate.kotlinosc.arg.OscNil]
 * - [MIDI][io.github.termtate.kotlinosc.type.MIDI] -> [OscMIDI][io.github.termtate.kotlinosc.arg.OscMIDI]
 * - [RGBA][io.github.termtate.kotlinosc.type.RGBA] -> [OscRGBA][io.github.termtate.kotlinosc.arg.OscRGBA]
 * - [kotlin.time.Instant] -> [OscTimetag][io.github.termtate.kotlinosc.arg.OscTimetag]
 * - [List] / [Array] -> [OscArray][io.github.termtate.kotlinosc.arg.OscArray] (recursive boxing)
 * - [OscArg][io.github.termtate.kotlinosc.arg.OscArg] -> unchanged
 *
 * A `List` or `Array` value is treated as one OSC array argument. To pass a
 * collection as multiple message arguments, spread it explicitly.
 *
 * @throws IllegalArgumentException when any element type is unsupported.
 */
public fun oscMessageOf(address: String, vararg args: Any?): OscMessage =
    OscMessage(address, args.map { it.toOscArg() })
