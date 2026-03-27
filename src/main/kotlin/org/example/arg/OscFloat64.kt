package org.example.arg

public data class OscFloat64(val value: Double) : OscArg {
    override val tag: String = TYPE_TAG.toString()

    internal companion object {
        internal const val TYPE_TAG: Char = 'd'
    }
}
