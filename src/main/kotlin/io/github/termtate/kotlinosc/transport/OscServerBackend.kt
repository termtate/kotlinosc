package io.github.termtate.kotlinosc.transport

import kotlinx.coroutines.flow.Flow

/**
 * One-shot packet receive backend; calling [start] after [stop] is invalid.
 */
internal interface OscServerBackend {
    /**
     * Received packets. This flow is closed by [stop].
     */
    val receivedPackets: Flow<ReceivedPacket>

    suspend fun start()

    suspend fun stop()
}
