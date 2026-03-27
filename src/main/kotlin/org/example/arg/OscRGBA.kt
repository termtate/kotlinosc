package org.example.arg

import org.example.type.RGBA

public data class OscRGBA(val value: RGBA) : OscArg {
    public constructor(r: UByte, g: UByte, b: UByte, a: UByte) : this(RGBA(r, g, b, a))

    override val tag: String
        get() = TYPE_TAG.toString()

    internal companion object {
        internal const val TYPE_TAG: Char = 'r'
    }
}
