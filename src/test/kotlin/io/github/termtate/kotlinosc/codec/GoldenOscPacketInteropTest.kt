package io.github.termtate.kotlinosc.codec

import io.github.termtate.kotlinosc.arg.OscInt32
import io.github.termtate.kotlinosc.arg.OscString
import io.github.termtate.kotlinosc.arg.OscTimetag
import io.github.termtate.kotlinosc.type.OscBundle
import io.github.termtate.kotlinosc.type.OscMessage
import io.github.termtate.kotlinosc.type.OscPacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class GoldenOscPacketInteropTest {
    private data class GoldenCase(
        val name: String,
        val packet: OscPacket
    )

    private val cases = listOf(
        GoldenCase(
            name = "message_ping_no_args",
            packet = OscMessage("/ping")
        ),
        GoldenCase(
            name = "message_sum_args",
            packet = OscMessage(
                address = "/sum",
                args = listOf(OscInt32(7), OscString("ok"))
            )
        ),
        GoldenCase(
            name = "bundle_immediate_two_messages",
            packet = OscBundle(
                timeTag = OscTimetag.IMMEDIATELY,
                elements = listOf(
                    OscMessage("/a"),
                    OscMessage("/b")
                )
            )
        )
    )

    @Test
    fun `encode should match golden hex packets`() {
        for (case in cases) {
            val expected = loadHexGolden(case.name)
            val actual = case.packet.encodeToByteArray()
            assertContentEquals(
                expected = expected,
                actual = actual,
                message = "encode mismatch for golden case '${case.name}'"
            )
        }
    }

    @Test
    fun `decode should match expected packet semantics from golden hex`() {
        for (case in cases) {
            val input = loadHexGolden(case.name)
            val decoded = OscPacket.decodeFromByteArray(input)
            assertEquals(
                expected = case.packet,
                actual = decoded,
                message = "decode mismatch for golden case '${case.name}'"
            )
        }
    }

    private fun loadHexGolden(name: String): ByteArray {
        val resource = "/golden/osc/$name.hex"
        val text = checkNotNull(javaClass.getResource(resource)) {
            "golden resource not found: $resource"
        }.readText()

        val hex = buildString {
            for (ch in text) {
                if (ch.isWhitespace()) continue
                append(ch)
            }
        }

        require(hex.length % 2 == 0) {
            "golden hex must be even-length, resource=$resource, length=${hex.length}"
        }

        val out = ByteArray(hex.length / 2)
        var i = 0
        while (i < hex.length) {
            val byte = hex.substring(i, i + 2).toInt(16)
            out[i / 2] = byte.toByte()
            i += 2
        }
        return out
    }
}

