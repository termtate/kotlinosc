package io.github.termtate.kotlinosc.matcher

import io.github.termtate.kotlinosc.exception.OscAddressParseException
import io.github.termtate.kotlinosc.pattern.compile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OscAddressMatcherBasicTest {
    @Test
    fun `strict false should return false for invalid address format`() {
        assertFalse {
            looseMatcher.matches(
                compiled = "/a/*".compile(),
                address = "a/x"
            )
        }
        assertFalse {
            looseMatcher.matches(
                compiled = "/a/*".compile(),
                address = "/a//x"
            )
        }
    }

    @Test
    fun `leading slash is required`() {
        assertFailsWith<IllegalArgumentException> {
            strictMatcher.matches(
                compiled = "/a/*".compile(),
                address = "a/x"
            )
        }

        assertFailsWith<OscAddressParseException> {
            strictMatcher.matches(
                compiled = "a/*".compile(),
                address = "/a/x"
            )
        }
    }

    @Test
    fun `empty segment should not match`() {
        assertFailsWith<IllegalArgumentException> {
            strictMatcher.matches(
                compiled = "/a/*/c".compile(),
                address = "/a//c"
            )
        }

        assertFailsWith<OscAddressParseException> {
            strictMatcher.matches(
                compiled = "/a//c".compile(),
                address = "/a/x/c"
            )
        }
    }

    @Test
    fun `empty segment matching test`() {
        assertFailsWith<IllegalArgumentException> {
            strictMatcher.matches(
                compiled = "/?".compile(),
                address = "/"
            )
        }

        assertFailsWith<IllegalArgumentException> {
            strictMatcher.matches(
                compiled = "/?/*".compile(),
                address = "/a/"
            )
        }
    }

    @Test
    fun `segment matching boundary test`() {
        assertTrue {
            strictMatcher.matches(
                compiled = "/a?/*".compile(),
                address = "/ab/c"
            )
        }

        assertFalse {
            strictMatcher.matches(
                compiled = "/a?/*".compile(),
                address = "/a/x"
            )
        }
    }
}
