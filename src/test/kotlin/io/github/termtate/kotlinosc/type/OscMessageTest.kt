package io.github.termtate.kotlinosc.type

import io.github.termtate.kotlinosc.arg.OscArray
import io.github.termtate.kotlinosc.arg.OscBlob
import io.github.termtate.kotlinosc.arg.OscInt32
import io.github.termtate.kotlinosc.arg.OscNil
import io.github.termtate.kotlinosc.arg.OscString
import io.github.termtate.kotlinosc.arg.OscTrue
import kotlin.test.Test
import kotlin.test.assertEquals

class OscMessageTest {
    @Test
    fun `oscMessageOf should box vararg args`() {
        val message = oscMessageOf("/x", 1, "s", true, null, byteArrayOf(1, 2))

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
    fun `oscMessageOf should support nested list and array`() {
        val message = oscMessageOf(
            "/x",
            listOf(1, true),
            arrayOf("a", null)
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

