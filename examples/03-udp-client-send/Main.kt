package examples.udpclientsend

import io.github.termtate.kotlinosc.transport.dsl.oscClient
import io.github.termtate.kotlinosc.type.OscMessage
import io.github.termtate.kotlinosc.type.invoke
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val client = oscClient("127.0.0.1", 9000)

    try {
        val message = OscMessage.invoke("/demo/ping", "hello from kotlinosc")
        client.send(message)
        println("Sent message to 127.0.0.1:9000 -> $message")
    } finally {
        client.closeAndJoin()
    }
}
