package io.github.termtate.kotlinosc.arg

public data object OscNil : OscArg {
    override val tag: String
        get() = TYPE_TAG.toString()

    internal const val TYPE_TAG: Char = 'N'
}

