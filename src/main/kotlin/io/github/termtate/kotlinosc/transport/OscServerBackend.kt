package io.github.termtate.kotlinosc.transport

import io.github.termtate.kotlinosc.type.OscPacket
import kotlinx.coroutines.flow.Flow

internal interface OscServerBackend {
    val receivedPackets: Flow<ReceivedPacket>

    suspend fun start()

    suspend fun stop()
}
