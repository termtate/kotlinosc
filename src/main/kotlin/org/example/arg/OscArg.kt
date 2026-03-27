package org.example.arg

public sealed interface OscArg {
    public val tag: String

    public companion object {
        internal const val START_SEP: Char = ','
    }
}
