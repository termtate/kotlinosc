package io.github.termtate.kotlinosc.type

import io.github.termtate.kotlinosc.arg.OscArg
import io.github.termtate.kotlinosc.arg.toOscArg

/**
 * Builds an [OscMessage] from pre-boxed [OscArg] values.
 */
public operator fun OscMessage.Companion.invoke(address: String, vararg args: OscArg): OscMessage =
    OscMessage(address, args.toList())

/**
 * Builds an [OscMessage] from Kotlin values by boxing each item to [OscArg].
 *
 * Supported mappings include:
 * - `Int` -> `OscInt32`
 * - `Long` -> `OscInt64`
 * - `Float` -> `OscFloat32`
 * - `Double` -> `OscFloat64`
 * - `String` -> `OscString`
 * - `Char` -> `OscChar`
 * - `Boolean` -> `OscTrue` / `OscFalse`
 * - `ByteArray` -> `OscBlob`
 * - `null` -> `OscNil`
 * - `MIDI` -> `OscMIDI`
 * - `RGBA` -> `OscRGBA`
 * - `Instant` -> `OscTimetag`
 * - `List<*>` / `Array<*>` -> `OscArray` (recursive boxing)
 * - `OscArg` -> unchanged
 *
 * @throws IllegalArgumentException when any element type is unsupported.
 */
public operator fun OscMessage.Companion.invoke(address: String, args: List<Any?>): OscMessage =
    OscMessage(address, args.map { it.toOscArg() })

/**
 * Builds an [OscMessage] from Kotlin values by boxing each item to [OscArg].
 *
 * See [invoke] with `List<Any?>` for supported mappings.
 */
public operator fun OscMessage.Companion.invoke(address: String, vararg args: Any?): OscMessage =
    OscMessage(address, args.toList())
