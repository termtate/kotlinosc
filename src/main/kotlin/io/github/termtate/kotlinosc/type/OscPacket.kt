package io.github.termtate.kotlinosc.type

/**
 * Root OSC packet type.
 *
 * Concrete packet types:
 * - [OscMessage]
 * - [OscBundle]
 */
public sealed interface OscPacket {
    public companion object
}
