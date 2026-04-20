package examples.udpserverroute

import io.github.termtate.kotlinosc.transport.dsl.oscServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

fun main(): Unit = runBlocking {
    val received = CompletableDeferred<Unit>()
    val server = oscServer("127.0.0.1", 9000) {
        route {
            on("/demo/ping") { message ->
                println("Received ${message.address} with args=${message.args}")
                if (!received.isCompleted) {
                    received.complete(Unit)
                }
            }
        }
    }

    try {
        server.start()
        println("Server listening on 127.0.0.1:9000")
        println("Send /demo/ping from another process within 30 seconds.")
        withTimeout(30.seconds) {
            received.await()
        }
    } finally {
        server.stop()
    }
}
