package io.github.termtate.kotlinosc.arg

public data class OscString(val value: String) : OscArg {
    override val tag: String = TYPE_TAG.toString()

    internal companion object {
        internal const val TYPE_TAG: Char = 's'
    }
}

