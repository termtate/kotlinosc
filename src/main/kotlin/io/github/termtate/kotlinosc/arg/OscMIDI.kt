package io.github.termtate.kotlinosc.arg

import io.github.termtate.kotlinosc.type.MIDI

public data class OscMIDI(val value: MIDI) : OscArg {
    public constructor(portId: UByte, status: UByte, data1: UByte, data2: UByte) : this(MIDI(portId, status, data1, data2))

    override val tag: String
        get() = TYPE_TAG.toString()

    internal companion object {
        internal const val TYPE_TAG: Char = 'm'
    }
}

