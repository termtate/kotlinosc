package io.github.termtate.kotlinosc.arg

/**
 * Base OSC argument type.
 *
 * Each implementation maps to one OSC type tag.
 */
public sealed interface OscArg {
    /**
     * OSC type tag represented as one-character string.
     */
    public val tag: String

    public companion object {
        internal const val START_SEP: Char = ','
    }
}

