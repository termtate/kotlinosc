package examples.tcpclientsend

import io.github.termtate.kotlinosc.transport.dsl.oscClient
import io.github.termtate.kotlinosc.type.OscMessage
import io.github.termtate.kotlinosc.type.invoke
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val client = oscClient("127.0.0.1", 9000) {
        protocol { tcp() }
    }

    try {
        val message = OscMessage.invoke("/demo/tcp/ping", "hello from kotlinosc over TCP")
        client.send(message)
        println("Sent TCP message to 127.0.0.1:9000 -> $message")
    } finally {
        client.closeAndJoin()
    }
}
