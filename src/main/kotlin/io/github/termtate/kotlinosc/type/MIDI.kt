package io.github.termtate.kotlinosc.type

public data class MIDI(
    val portId: UByte,
    val status: UByte,
    val data1: UByte,
    val data2: UByte,
)
