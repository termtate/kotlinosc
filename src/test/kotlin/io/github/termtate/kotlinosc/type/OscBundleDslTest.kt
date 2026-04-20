package io.github.termtate.kotlinosc.type

import io.github.termtate.kotlinosc.arg.OscArg
import io.github.termtate.kotlinosc.arg.OscInfinitum
import io.github.termtate.kotlinosc.arg.OscInt32
import io.github.termtate.kotlinosc.arg.OscString
import io.github.termtate.kotlinosc.arg.OscTimetag
import io.github.termtate.kotlinosc.arg.toOscTimetag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class OscBundleDslTest {
    @Test
    fun `oscBundle should build OscBundle correctly`() {
        val now = Clock.System.now().toOscTimetag()
        val bundle = oscBundle(now) {
            message("/a", 1, 0.2f, "3")

            bundle(now) {
                message("/b", 4L, true, OscInfinitum)

                bundle(Instant.fromEpochSeconds(1_000).toOscTimetag()) {  }
            }

            message("/c", MIDI(5u, 6u, 7u, 8u))

        }

        val excepted = OscBundle(
            timeTag = now,
            listOf(
                OscMessage("/a", 1, 0.2f, "3"),
                OscBundle(
                    timeTag = now,
                    listOf(
                        OscMessage("/b", 4L, true, OscInfinitum),
                        OscBundle(Instant.fromEpochSeconds(1_000).toOscTimetag())
                    )
                ),
                OscMessage("/c", MIDI(5u, 6u, 7u, 8u))
            )
        )

        assertEquals(bundle, excepted)
    }

    @Test
    fun `message list overloads should both work`() {
        val argList: List<OscArg> = listOf(OscInt32(1), OscString("x"))
        val anyList: List<Any?> = listOf(2, "y")

        val bundle = oscBundle {
            message("/typed", argList)
            message("/boxed", anyList)
        }

        val expected = OscBundle(
            timeTag = OscTimetag.IMMEDIATELY,
            elements = listOf(
                OscMessage("/typed", listOf(OscInt32(1), OscString("x"))),
                OscMessage("/boxed", listOf(OscInt32(2), OscString("y")))
            )
        )
        assertEquals(expected, bundle)
    }

    @Test
    fun `oscBundle and nested bundle should use immediately timetag by default`() {
        val bundle = oscBundle {
            bundle {
                message("/a", emptyList<Any?>())
            }
        }

        assertEquals(OscTimetag.IMMEDIATELY, bundle.timeTag)
        val nested = bundle.elements.single() as OscBundle
        assertEquals(OscTimetag.IMMEDIATELY, nested.timeTag)
    }

    @Test
    fun `empty oscBundle should keep timetag and have no elements`() {
        val t = Instant.fromEpochSeconds(42).toOscTimetag()
        val bundle = oscBundle(t) { }

        assertEquals(t, bundle.timeTag)
        assertTrue(bundle.elements.isEmpty())
    }

    @Test
    fun `OscBundle toString should format nested bundles readably`() {
        val bundle = oscBundle {
            message("/a", 1, "x")
            bundle {
                message("/b", true)
            }
        }

        val rendered = bundle.toString()

        println(rendered)

        assertEquals(
            rendered,
            "OscBundle(timeTag=IMMEDIATELY, elements=[\n" +
            "  OscMessage(address=/a, args=[OscInt32(value=1), OscString(value=x)]),\n" +
            "  OscBundle(timeTag=IMMEDIATELY, elements=[\n" +
            "    OscMessage(address=/b, args=[OscTrue])\n" +
            "  ])\n" +
            "])"
        )
    }
}

