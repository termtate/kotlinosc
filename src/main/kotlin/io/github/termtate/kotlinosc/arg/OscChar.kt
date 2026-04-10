package io.github.termtate.kotlinosc.arg

public data class OscChar(val value: Int) : OscArg {  // wrap Int instead of Char because osc-char takes 4 bytes
    public constructor(value: Char) : this(value.code)

    override val tag: String
        get() = TYPE_TAG.toString()

    internal companion object {
        internal const val TYPE_TAG: Char = 'c'
    }
}

