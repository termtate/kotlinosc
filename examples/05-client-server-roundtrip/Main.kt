package examples.clientserverroundtrip

import io.github.termtate.kotlinosc.transport.dsl.oscClient
import io.github.termtate.kotlinosc.transport.dsl.oscServer
import io.github.termtate.kotlinosc.type.OscMessage
import io.github.termtate.kotlinosc.type.invoke
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.DatagramSocket

fun main(): Unit = runBlocking {
    val port = findFreeUdpPort()
    val received = CompletableDeferred<OscMessage>()

    val server = oscServer("127.0.0.1", port) {
        route {
            on("/roundtrip") { message ->
                if (!received.isCompleted) {
                    received.complete(message)
                }
            }
        }
    }
    val client = oscClient("127.0.0.1", port)

    try {
        server.start()

        val outbound = OscMessage.invoke("/roundtrip", 42, "hello")
        client.send(outbound)

        withTimeout(3_000) {
            val inbound = received.await()
            println("Server port: $port")
            println("Sent: $outbound")
            println("Received: $inbound")
        }
    } finally {
        client.closeAndJoin()
        server.stop()
    }
}

private fun findFreeUdpPort(): Int = DatagramSocket(0).use { it.localPort }
