package io.github.termtate.kotlinosc.arg

public data class OscInt32(val value: Int) : OscArg {
    override val tag: String = TYPE_TAG.toString()

    internal companion object {
        internal const val TYPE_TAG: Char = 'i'
    }
}

