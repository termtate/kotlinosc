package io.github.termtate.kotlinosc.matcher

import io.github.termtate.kotlinosc.exception.OscAddressParseException
import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.pattern.OscAddressMatcher
import io.github.termtate.kotlinosc.pattern.compile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OscAddressMatcherTest {
    private val strictMatcher = OscAddressMatcher(OscConfig.AddressPattern.default)
    private val looseMatcher = OscAddressMatcher(OscConfig.AddressPattern(strictAddressPattern = false))

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
    fun `multiple stars in one segment`() {
        assertTrue {
            strictMatcher.matches(
                compiled = "/a**c".compile(),
                address = "/abc"
            )
        }

        assertTrue {
            strictMatcher.matches(
                compiled = "/a**c".compile(),
                address = "/abbbbbc"
            )
        }

        assertTrue {
            strictMatcher.matches(
                compiled = "/a**c".compile(),
                address = "/ac"
            )
        }
    }

    @Test
    fun `trailing star test`() {
        assertTrue {
            strictMatcher.matches(
                compiled = "/a*".compile(),
                address = "/a"
            )
        }

        assertTrue {
            strictMatcher.matches(
                compiled = "/a*".compile(),
                address = "/abc"
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
    fun `single char matching test`() {
        assertTrue {
            strictMatcher.matches(
                compiled = "/a?c".compile(),
                address = "/abc"
            )
        }

        assertFalse {
            strictMatcher.matches(
                compiled = "/a?c".compile(),
                address = "/ac"
            )
        }

        assertTrue {
            strictMatcher.matches(
                compiled = "/?".compile(),
                address = "/a"
            )
        }
    }

    @Test
    fun `combination matching test`() {
        assertTrue {
            strictMatcher.matches(
                compiled = "/a*?c".compile(),
                address = "/abbbc"
            )
        }

        assertFalse {
            strictMatcher.matches(
                compiled = "/a*?c".compile(),
                address = "/ac"
            )
        }

        assertTrue {
            strictMatcher.matches(
                compiled = "/a?*c".compile(),
                address = "/abbbc"
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

    @Test
    fun `alternation set basic test`() {
        val pattern1 = "/{foo,bar}".compile()
        assertTrue { strictMatcher.matches(pattern1, "/foo") }
        assertTrue { strictMatcher.matches(pattern1, "/bar") }
        assertFalse { strictMatcher.matches(pattern1, "/baz") }

        val pattern2 = "/{a,bbb}".compile()
        assertTrue { strictMatcher.matches(pattern2, "/a") }
        assertTrue { strictMatcher.matches(pattern2, "/bbb") }
        assertFalse { strictMatcher.matches(pattern2, "/baz") }

        val pattern3 = "/{bbb,a}".compile()
        assertTrue { strictMatcher.matches(pattern3, "/a") }
        assertTrue { strictMatcher.matches(pattern3, "/bbb") }
        assertFalse { strictMatcher.matches(pattern3, "/baz") }
    }

    @Test
    fun `alternation single branch test`() {
        val pattern1 = "/{abc}".compile()
        assertTrue { strictMatcher.matches(pattern1, "/abc") }
        assertFalse { strictMatcher.matches(pattern1, "/abd") }
    }

    @Test
    fun `alternation & literal test`() {
        val pattern1 = "/x{ab,cd}y".compile()
        assertTrue { strictMatcher.matches(pattern1, "/xaby") }
        assertTrue { strictMatcher.matches(pattern1, "/xcdy") }
        assertFalse { strictMatcher.matches(pattern1, "/xay") }
    }

    @Test
    fun `alternation & star & anyChar combination test`() {
        val pattern1 = "/{a?,b*}".compile()
        assertTrue { strictMatcher.matches(pattern1, "/aa") }
        assertTrue { strictMatcher.matches(pattern1, "/bbbbbbb") }
        assertFalse { strictMatcher.matches(pattern1, "/abc") }
    }

    @Test
    fun `alternation & char class combination test`() {
        val pattern1 = "/{[a-c]y,cd}".compile()
        assertTrue { strictMatcher.matches(pattern1, "/ay") }
        assertTrue { strictMatcher.matches(pattern1, "/cd") }
        assertFalse { strictMatcher.matches(pattern1, "/dy") }

        val pattern2 = "/{[a-c],cd}".compile()
        assertTrue { strictMatcher.matches(pattern2, "/a") }
        assertTrue { strictMatcher.matches(pattern2, "/cd") }
        assertFalse { strictMatcher.matches(pattern2, "/ab") }
    }


    @Test
    fun `alternation illegal pattern test`() {
        assertFailsWith<OscAddressParseException> { "/{}".compile() }
        assertFailsWith<OscAddressParseException> { "/{a,,b}".compile() }
        assertFailsWith<OscAddressParseException> { "/{ab".compile() }
        assertFailsWith<OscAddressParseException> { "/ab}".compile() }
    }
}

