package io.github.termtate.kotlinosc.transport

import io.github.termtate.kotlinosc.type.OscPacket

internal interface OscClientBackend {
    suspend fun send(packet: OscPacket)

    suspend fun close()
}
