package io.github.termtate.kotlinosc.transport

import java.net.SocketAddress

internal sealed interface OscPeer

internal data class UdpPeer(
    val address: SocketAddress
) : OscPeer

internal data class TcpPeer(
    val connectionId: Long,
    val remoteAddress: SocketAddress
) : OscPeer