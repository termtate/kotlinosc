package io.github.termtate.kotlinosc.arg

public data class OscArray(val elements: List<OscArg> = emptyList()) : OscArg {
    public constructor(vararg elements: OscArg) : this(elements.toList())

    override val tag: String
        get() = elements.joinToString(
            separator = "",
            prefix = LEFT_BRACKET.toString(),
            postfix = RIGHT_BRACKET.toString()
        ) { it.tag }

    internal companion object {
        const val LEFT_BRACKET: Char = '['
        const val RIGHT_BRACKET: Char = ']'
    }
}

