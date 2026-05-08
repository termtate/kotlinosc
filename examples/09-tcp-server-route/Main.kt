package examples.tcpserverroute

import io.github.termtate.kotlinosc.transport.dsl.oscServer
import io.github.termtate.kotlinosc.transport.tcp.codec.OscTcpFramingStrategy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

fun main(): Unit = runBlocking {
    val received = CompletableDeferred<Unit>()
    val server = oscServer("127.0.0.1", 9000) {
        protocol {
            tcp {
                framingStrategy = OscTcpFramingStrategy.SLIP
            }
        }
        route {
            on("/demo/tcp/slip") { message ->
                println("Received ${message.address} with args=${message.args}")
                if (!received.isCompleted) {
                    received.complete(Unit)
                }
            }
        }
    }

    try {
        server.start()
        println("TCP SLIP server listening on 127.0.0.1:9000")
        println("Send /demo/tcp/slip from another process within 30 seconds.")
        withTimeout(30.seconds) {
            received.await()
        }
    } finally {
        server.stop()
    }
}
