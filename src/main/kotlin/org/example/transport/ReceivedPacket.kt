package org.example.transport

import org.example.type.OscPacket
import java.net.SocketAddress
import kotlin.time.Instant

internal data class ReceivedPacket(
    val packet: OscPacket,
    val remote: SocketAddress,
    val receivedAt: Instant,
)