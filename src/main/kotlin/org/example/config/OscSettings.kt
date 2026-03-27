package org.example.config

public sealed class OscSettings {
    public data object Codec {
        /**
         * 决定以下情况发生时的行为（log记录或者报错）：
         * - 在decode [org.example.type.OscPacket] 时，decode完之后有多余bytes
         *
         */
        var strict: Boolean = true
    }

    public data object AddressPattern {
        /**
         * - 匹配address时，如果address是非法格式（e.g. "a" or "//a"），是否报错
         */
        var strict: Boolean = true
    }
}