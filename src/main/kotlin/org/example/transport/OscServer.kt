package org.example.transport

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.example.exception.OscLifecycleException
import org.example.route.DispatchMode
import org.example.route.OscDispatcher
import org.example.route.OscPacketScheduler
import org.example.route.OscRouter
import org.example.type.OscMessage
import org.example.util.OscLogger
import org.example.util.logger
import java.io.Closeable
import java.net.DatagramSocket
import java.net.SocketAddress

public class OscServer(
    bindAddress: SocketAddress,
    router: OscRouter,
    public val dispatchMode: DispatchMode = DispatchMode.ALL_MATCH,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    continueOnDispatchError: Boolean = true,
    maxConcurrentDispatches: Int = 1,
    dispatchDispatcher: CoroutineDispatcher = Dispatchers.IO,
    transportHook: OscTransportHook = OscTransportHook.NOOP
) : OscLogger {
    override val logTag: String
        get() = "OscServer"

    private val dispatcher = OscDispatcher(router)
    private val scheduler = OscPacketScheduler.factory(
        dispatcher::dispatch,
        scope,
        continueOnDispatchError = continueOnDispatchError,
        maxConcurrentDispatches = maxConcurrentDispatches,
        dispatchDispatcher = dispatchDispatcher
    )

    private val transport = UdpOscTransport(
        scope,
        DatagramSocket(bindAddress),
        hook = transportHook
    )

    private var collectorJob: Job? = null

    /**
     * 开始监听并分发[org.example.type.OscPacket]消息
     *
     * Calling start() after stop() throws.
     */
    public suspend fun start() {
        if (collectorJob != null && collectorJob?.isActive == true) {
            return
        }


        if (transport.isClosed()) {
            throw OscLifecycleException("OscServer.start() cannot be called after stop()")
        }

        collectorJob = scope.launch {
            transport.receivedPackets.collect { receivedPacket ->
                logger.debug { "received packet $receivedPacket" }
                scheduler.schedule(receivedPacket.packet, dispatchMode)
            }
        }
        scheduler.start()
        transport.start()

        logger.info { "osc server started" }
    }

    public suspend fun stop() {
        collectorJob?.cancelAndJoin()
        collectorJob = null

        transport.stop()
        scheduler.stop()

        logger.info { "osc server stopped" }
    }
}
