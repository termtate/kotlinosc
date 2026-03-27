package org.example.arg

public data class OscInt64(val value: Long) : OscArg {
    override val tag: String = TYPE_TAG.toString()

    internal companion object {
        internal const val TYPE_TAG: Char = 'h'
    }
}
