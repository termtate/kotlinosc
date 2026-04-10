package io.github.termtate.kotlinosc.arg

public data class OscSymbol(val value: String) : OscArg {
    override val tag: String
        get() = TYPE_TAG.toString()

    internal companion object {
        internal const val TYPE_TAG: Char = 'S'
    }
}

