package examples.addresspatternrouting

import io.github.termtate.kotlinosc.transport.dsl.oscClient
import io.github.termtate.kotlinosc.transport.dsl.oscServer
import io.github.termtate.kotlinosc.type.OscMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.DatagramSocket

fun main(): Unit = runBlocking {
    val port = findFreeUdpPort()
    val seen = mutableListOf<String>()
    val finished = CompletableDeferred<Unit>()

    val server = oscServer("127.0.0.1", port) {
        route {
            on("/drum/*") { message ->
                seen += "wildcard -> ${message.address}"
                maybeFinish(seen, finished)
            }
            on("/drum/{kick,snare}") { message ->
                seen += "alternation -> ${message.address}"
                maybeFinish(seen, finished)
            }
            on("/clip/[0-9]") { message ->
                seen += "char-class -> ${message.address}"
                maybeFinish(seen, finished)
            }
        }
    }
    val client = oscClient("127.0.0.1", port)

    try {
        server.start()

        client.send(OscMessage("/drum/kick"))
        client.send(OscMessage("/drum/hihat"))
        client.send(OscMessage("/clip/7"))

        withTimeout(3_000) {
            finished.await()
        }

        println("Matched handlers:")
        seen.forEach(::println)
    } finally {
        client.closeAndJoin()
        server.stop()
    }
}

private fun maybeFinish(seen: List<String>, finished: CompletableDeferred<Unit>) {
    if (seen.size >= 4 && !finished.isCompleted) {
        finished.complete(Unit)
    }
}

private fun findFreeUdpPort(): Int = DatagramSocket(0).use { it.localPort }
