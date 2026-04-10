package io.github.termtate.kotlinosc.type

import io.github.termtate.kotlinosc.arg.OscArray
import io.github.termtate.kotlinosc.arg.OscBlob
import io.github.termtate.kotlinosc.arg.OscFalse
import io.github.termtate.kotlinosc.arg.OscInt32
import io.github.termtate.kotlinosc.arg.OscNil
import io.github.termtate.kotlinosc.arg.OscString
import io.github.termtate.kotlinosc.arg.OscTrue
import kotlin.test.Test
import kotlin.test.assertEquals

class OscMessageExtTest {
    @Test
    fun `OscMessage companion invoke should box list args`() {
        val message = OscMessage(
            "/x",
            listOf(1, "s", true, null, byteArrayOf(1, 2))
        )

        assertEquals(
            OscMessage(
                "/x",
                listOf(
                    OscInt32(1),
                    OscString("s"),
                    OscTrue,
                    OscNil,
                    OscBlob(byteArrayOf(1, 2))
                )
            ),
            message
        )
    }

    @Test
    fun `OscMessage companion invoke should box vararg args`() {
        val message = OscMessage("/x", 1, 2.0f, "s", false)

        assertEquals(
            OscMessage(
                "/x",
                listOf(
                    OscInt32(1),
                    io.github.termtate.kotlinosc.arg.OscFloat32(2.0f),
                    OscString("s"),
                    OscFalse
                )
            ),
            message
        )
    }

    @Test
    fun `OscMessage companion invoke should support nested list and array`() {
        val message = OscMessage(
            "/x",
            listOf(
                listOf(1, true),
                arrayOf("a", null)
            )
        )

        assertEquals(
            OscMessage(
                "/x",
                listOf(
                    OscArray(OscInt32(1), OscTrue),
                    OscArray(OscString("a"), OscNil)
                )
            ),
            message
        )
    }
}

