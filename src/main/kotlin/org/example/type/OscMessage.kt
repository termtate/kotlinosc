package org.example.type

import org.example.arg.OscArg

/**
 * 符合OSC Spec 1.0的OSC message
 */
public data class OscMessage(
    val address: String,
    val args: List<OscArg> = emptyList()
) : OscPacket
