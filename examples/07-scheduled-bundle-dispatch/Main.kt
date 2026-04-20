package examples.scheduledbundledispatch

import io.github.termtate.kotlinosc.arg.toOscTimetag
import io.github.termtate.kotlinosc.transport.dsl.oscClient
import io.github.termtate.kotlinosc.transport.dsl.oscServer
import io.github.termtate.kotlinosc.type.oscBundle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.DatagramSocket
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

fun main(): Unit = runBlocking {
    val port = findFreeUdpPort()
    val receivedAt = CompletableDeferred<kotlin.time.Instant>()

    val server = oscServer("127.0.0.1", port) {
        route {
            on("/scheduled/demo") {
                if (!receivedAt.isCompleted) {
                    receivedAt.complete(Clock.System.now())
                }
            }
        }
    }
    val client = oscClient("127.0.0.1", port)

    try {
        server.start()

        val dueAt = Clock.System.now() + 1500.milliseconds
        val bundle = oscBundle(dueAt.toOscTimetag()) {
            message("/scheduled/demo", "hello at a later timetag")
        }

        println("Sending bundle scheduled for: $dueAt")
        client.send(bundle)

        val actualAt = withTimeout(3_000) {
            receivedAt.await()
        }
        val dispatchDelay = actualAt - dueAt

        println("Handler executed at: $actualAt")
        println("Observed dispatch delay relative to timetag: $dispatchDelay")
        println("Expected: close to zero or slightly positive depending on scheduler and OS timing.")
    } finally {
        client.closeAndJoin()
        server.stop()
    }
}

private fun findFreeUdpPort(): Int = DatagramSocket(0).use { it.localPort }
