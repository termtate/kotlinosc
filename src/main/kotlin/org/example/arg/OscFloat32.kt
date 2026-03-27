package org.example.arg

public data class OscFloat32(val value: Float) : OscArg {
    override val tag: String = TYPE_TAG.toString()

    internal companion object {
        internal const val TYPE_TAG: Char = 'f'
    }
}
