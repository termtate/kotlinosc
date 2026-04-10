package io.github.termtate.kotlinosc.config

/**
 * Core configuration model for KotlinOSC runtime behavior.
 */
public sealed class OscConfig {
    /**
     * Codec behavior for packet encoding/decoding.
     *
     * @property strictCodecPayloadConsumption
     * `true` enforces strict payload consumption checks while decoding.
     * `false` allows permissive tail/consumption behavior.
     */
    public data class Codec(
        val strictCodecPayloadConsumption: Boolean
    ) {
        /**
         * Default codec config.
         *
         * `strictCodecPayloadConsumption = true`
         */
        public companion object {
            public val default: Codec = Codec(
                strictCodecPayloadConsumption = true
            )
        }
    }

    /**
     * Address pattern matching behavior.
     *
     * @property strictAddressPattern
     * Whether to throw when matching an invalid address format
     * (for example: `"a"` or `"//a"`).
     */
    public data class AddressPattern(
        val strictAddressPattern: Boolean
    ) {
        /**
         * Default address pattern config.
         *
         * `strictAddressPattern = true`
         */
        public companion object {
            public val default: AddressPattern = AddressPattern(
                strictAddressPattern = true
            )
        }
    }
}
