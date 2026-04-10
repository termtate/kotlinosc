package io.github.termtate.kotlinosc.transport

import io.github.termtate.kotlinosc.exception.OscCodecException

/**
 * Hook callbacks for transport and decode errors.
 *
 * Callbacks run on internal runtime coroutines.
 * Keep implementations non-blocking and exception-safe.
 */
public interface OscTransportHook {
    /**
     * Called when payload decoding fails.
     *
     * @param payload raw UDP payload bytes
     * @param error decode failure
     */
    public fun onDecodeError(payload: ByteArray, error: OscCodecException): Unit = Unit

    /**
     * Called when transport-level send/receive loop reports an error.
     */
    public fun onTransportError(error: Throwable): Unit = Unit

    public companion object {
        /** No-op hook implementation. */
        public val NOOP: OscTransportHook = object : OscTransportHook {}
    }
}

