package org.example.arg

public data class OscBlob(val value: ByteArray) : OscArg {
    override val tag: String = TYPE_TAG.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OscBlob
        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int = value.contentHashCode()

    internal companion object {
        internal const val TYPE_TAG: Char = 'b'
    }
}
