package org.example.transport

import kotlinx.coroutines.CoroutineScope
import org.example.type.OscPacket
import java.net.DatagramSocket
import java.net.SocketAddress

public class OscClient(
    private val targetAddress: SocketAddress,
    scope: CoroutineScope,
    transportHook: OscTransportHook = OscTransportHook.NOOP
) {
    private val transport = UdpOscTransport(
        scope,
        DatagramSocket(),
        hook = transportHook
    )

    public suspend fun send(packet: OscPacket) {
        transport.send(packet, targetAddress)
    }

    public suspend fun stop(): Unit = transport.stop()
}
