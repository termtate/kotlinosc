package examples.messagecodec

import io.github.termtate.kotlinosc.codec.decodeFromByteArray
import io.github.termtate.kotlinosc.codec.encodeToByteArray
import io.github.termtate.kotlinosc.type.OscPacket
import io.github.termtate.kotlinosc.type.oscMessageOf

fun main() {
    val message = oscMessageOf("/synth/freq", 440, 0.75f, "lead")
    val payload = message.encodeToByteArray()
    val decoded = OscPacket.decodeFromByteArray(payload)

    println("Original message: $message")
    println("Payload length: ${payload.size} bytes")
    println("Payload hex: ${payload.joinToString(" ") { "%02x".format(it) }}")
    println("Decoded packet: $decoded")
}
