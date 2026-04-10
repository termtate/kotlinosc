package io.github.termtate.kotlinosc.type

import io.github.termtate.kotlinosc.arg.OscArg

/**
 * OSC message conforming to OSC Specification 1.0.
 *
 * @property address OSC address string (for example `/foo/bar`).
 * @property args Message arguments encoded as [OscArg].
 */
public data class OscMessage(
    val address: String,
    val args: List<OscArg> = emptyList()
) : OscPacket {
    public companion object
}
