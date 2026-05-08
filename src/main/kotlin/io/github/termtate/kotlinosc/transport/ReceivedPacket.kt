package io.github.termtate.kotlinosc.transport

import io.github.termtate.kotlinosc.type.OscPacket
import java.net.SocketAddress
import kotlin.time.Instant

internal data class ReceivedPacket(
    val packet: OscPacket,
    val peer: OscPeer,
    val receivedAt: Instant,
)
