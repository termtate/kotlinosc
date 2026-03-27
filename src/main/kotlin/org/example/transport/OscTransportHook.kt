package org.example.transport

import org.example.exception.OscCodecException

public interface OscTransportHook {
    public fun onDecodeError(payload: ByteArray, error: OscCodecException): Unit = Unit

    public fun onTransportError(error: Throwable): Unit = Unit

    public companion object {
        public val NOOP: OscTransportHook = object : OscTransportHook {}
    }
}
