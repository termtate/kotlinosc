package org.example.arg

public data object OscFalse : OscArg {
    override val tag: String
        get() = TYPE_TAG.toString()

    internal const val TYPE_TAG: Char = 'F'
}
