package io.github.termtate.kotlinosc.matcher

import io.github.termtate.kotlinosc.exception.OscAddressParseException
import io.github.termtate.kotlinosc.pattern.compile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OscAddressMatcherCharClassTest {
    @Test
    fun `char class basic test`() {
        val pattern = "/[abc]".compile()
        assertTrue { strictMatcher.matches(pattern, "/a") }
        assertTrue { strictMatcher.matches(pattern, "/b") }
        assertTrue { strictMatcher.matches(pattern, "/c") }
        assertFalse { strictMatcher.matches(pattern, "/d") }

        val pattern2 = "/[abc]d".compile()
        assertTrue { strictMatcher.matches(pattern2, "/ad") }
        assertTrue { strictMatcher.matches(pattern2, "/bd") }
        assertFalse { strictMatcher.matches(pattern2, "/c") }
        assertFalse { strictMatcher.matches(pattern2, "/dd") }
    }

    @Test
    fun `char class range test`() {
        val pattern = "/[a-c]".compile()
        assertTrue { strictMatcher.matches(pattern, "/a") }
        assertTrue { strictMatcher.matches(pattern, "/b") }
        assertTrue { strictMatcher.matches(pattern, "/c") }
        assertFalse { strictMatcher.matches(pattern, "/d") }

        val upper = "/[A-C]".compile()
        assertTrue { strictMatcher.matches(upper, "/A") }
        assertTrue { strictMatcher.matches(upper, "/B") }
        assertFalse { strictMatcher.matches(upper, "/D") }

        val digits = "/[0-2]".compile()
        assertTrue { strictMatcher.matches(digits, "/0") }
        assertTrue { strictMatcher.matches(digits, "/2") }
        assertFalse { strictMatcher.matches(digits, "/3") }
    }

    @Test
    fun `char class dash literal test`() {
        val pattern = "/[-ac]".compile()
        assertTrue { strictMatcher.matches(pattern, "/a") }
        assertFalse { strictMatcher.matches(pattern, "/b") }
        assertTrue { strictMatcher.matches(pattern, "/c") }
        assertFalse { strictMatcher.matches(pattern, "/d") }
        assertTrue { strictMatcher.matches(pattern, "/-") }

        val pattern2 = "/[ac-]".compile()
        assertTrue { strictMatcher.matches(pattern2, "/a") }
        assertFalse { strictMatcher.matches(pattern2, "/b") }
        assertTrue { strictMatcher.matches(pattern2, "/c") }
        assertFalse { strictMatcher.matches(pattern2, "/d") }
        assertTrue { strictMatcher.matches(pattern2, "/-") }
    }

    @Test
    fun `char class negation test`() {
        val pattern = "/[!a-c]".compile()
        assertTrue { strictMatcher.matches(pattern, "/z") }
        assertFalse { strictMatcher.matches(pattern, "/b") }

        val pattern2 = "/[^a-c]".compile()
        assertTrue { strictMatcher.matches(pattern2, "/z") }
        assertFalse { strictMatcher.matches(pattern2, "/b") }
    }

    @Test
    fun `multiple char class range`() {
        val pattern = "/[a-c0-5]".compile()
        assertTrue { strictMatcher.matches(pattern, "/b") }
        assertTrue { strictMatcher.matches(pattern, "/2") }
        assertFalse { strictMatcher.matches(pattern, "/d") }

        val pattern2 = "/[a-cxyz0-5]".compile()
        assertTrue { strictMatcher.matches(pattern2, "/b") }
        assertTrue { strictMatcher.matches(pattern2, "/2") }
        assertTrue { strictMatcher.matches(pattern2, "/y") }
        assertFalse { strictMatcher.matches(pattern2, "/d") }
    }

    @Test
    fun `char class should support backtrack with star`() {
        val pattern = "/a*[bc]d".compile()
        assertTrue { strictMatcher.matches(pattern, "/axxbd") }
        assertTrue { strictMatcher.matches(pattern, "/abbd") }
        assertFalse { strictMatcher.matches(pattern, "/axxdd") }
    }

    @Test
    fun `char class illegal pattern test`() {
        assertFailsWith<OscAddressParseException> { "/[]".compile() }
        assertFailsWith<OscAddressParseException> { "/[ab".compile() }
        assertFailsWith<OscAddressParseException> { "/ab]".compile() }
        assertFailsWith<OscAddressParseException> { "/[b-a]".compile() }
    }

    @Test
    fun `char class boundary test`() {
        "/[a-z]".compile().let { pattern ->
            assertTrue(strictMatcher.matches(pattern, "/a"))
            assertTrue(strictMatcher.matches(pattern, "/z"))
            assertFalse(strictMatcher.matches(pattern, "/0"))
        }

        "/[-a]".compile().let { pattern ->
            assertTrue(strictMatcher.matches(pattern, "/a"))
            assertTrue(strictMatcher.matches(pattern, "/-"))
            assertFalse(strictMatcher.matches(pattern, "/b"))
        }

        "/[a-]".compile().let { pattern ->
            assertTrue(strictMatcher.matches(pattern, "/a"))
            assertTrue(strictMatcher.matches(pattern, "/-"))
            assertFalse(strictMatcher.matches(pattern, "/b"))
        }

        "/[!-]".compile().let { pattern ->
            assertTrue(strictMatcher.matches(pattern, "/a"))
            assertFalse(strictMatcher.matches(pattern, "/-"))
            assertTrue(strictMatcher.matches(pattern, "/b"))
        }
    }
}
