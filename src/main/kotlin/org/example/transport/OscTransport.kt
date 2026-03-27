package org.example.transport

import kotlinx.coroutines.flow.Flow
import org.example.type.OscPacket
import java.net.SocketAddress

internal interface OscTransport {
    val receivedPackets: Flow<ReceivedPacket>

    suspend fun send(packet: OscPacket, target: SocketAddress)

    fun start()

    suspend fun stop()

    fun isClosed(): Boolean

}