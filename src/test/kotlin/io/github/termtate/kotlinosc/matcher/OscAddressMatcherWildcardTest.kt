package io.github.termtate.kotlinosc.matcher

import io.github.termtate.kotlinosc.pattern.compile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OscAddressMatcherWildcardTest {
    @Test
    fun `wildcard segment matching test`() {
        assertTrue {
            strictMatcher.matches(
                compiled = "/a/*".compile(),
                address = "/a/abc"
            )
        }

        assertTrue {
            strictMatcher.matches(
                compiled = "/a*c".compile(),
                address = "/axc"
            )
        }

        assertTrue {
            strictMatcher.matches(
                compiled = "/a*bc".compile(),
                address = "/abbbbbbc"
            )
        }

        assertFalse {
            strictMatcher.matches(
                compiled = "/a*c".compile(),
                address = "/ab"
            )
        }

        assertFalse {
            strictMatcher.matches(
                compiled = "/a/*/c".compile(),
                address = "/a/x/y/c"
            )
        }
    }

    @Test
    fun `multiple stars in one segment`() {
        assertTrue { strictMatcher.matches("/a**c".compile(), "/abc") }
        assertTrue { strictMatcher.matches("/a**c".compile(), "/abbbbbc") }
        assertTrue { strictMatcher.matches("/a**c".compile(), "/ac") }
    }

    @Test
    fun `trailing star test`() {
        assertTrue { strictMatcher.matches("/a*".compile(), "/a") }
        assertTrue { strictMatcher.matches("/a*".compile(), "/abc") }
    }

    @Test
    fun `single char matching test`() {
        assertTrue { strictMatcher.matches("/a?c".compile(), "/abc") }
        assertFalse { strictMatcher.matches("/a?c".compile(), "/ac") }
        assertTrue { strictMatcher.matches("/?".compile(), "/a") }
    }

    @Test
    fun `combination matching test`() {
        assertTrue { strictMatcher.matches("/a*?c".compile(), "/abbbc") }
        assertFalse { strictMatcher.matches("/a*?c".compile(), "/ac") }
        assertTrue { strictMatcher.matches("/a?*c".compile(), "/abbbc") }
    }
}
