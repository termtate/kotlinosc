package examples.tcpclientsend

import io.github.termtate.kotlinosc.transport.dsl.oscClient
import io.github.termtate.kotlinosc.type.oscMessageOf
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val client = oscClient("127.0.0.1", 9000) {
        protocol { tcp() }
    }

    try {
        val message = oscMessageOf("/demo/tcp/ping", "hello from kotlinosc over TCP")
        client.send(message)
        println("Sent TCP message to 127.0.0.1:9000 -> $message")
    } finally {
        client.closeAndJoin()
    }
}
