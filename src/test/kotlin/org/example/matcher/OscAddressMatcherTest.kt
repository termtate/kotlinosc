package org.example.matcher

import org.example.exception.OscAddressParseException
import org.example.config.OscSettings
import org.example.pattern.OscAddressMatcher
import org.example.pattern.compile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OscAddressMatcherTest {
    @Test
    fun `strict false should return false for invalid address format`() {
        val previous = OscSettings.AddressPattern.strict
        OscSettings.AddressPattern.strict = false
        try {
            assertFalse {
                OscAddressMatcher.matches(
                    compiled = "/a/*".compile(),
                    address = "a/x"
                )
            }
            assertFalse {
                OscAddressMatcher.matches(
                    compiled = "/a/*".compile(),
                    address = "/a//x"
                )
            }
        } finally {
            OscSettings.AddressPattern.strict = previous
        }
    }

    @Test
    fun `wildcard segment matching test`() {
        assertTrue {
            OscAddressMatcher.matches(
                compiled = "/a/*".compile(),
                address = "/a/abc"
            )
        }

        assertTrue {
            OscAddressMatcher.matches(
                compiled = "/a*c".compile(),
                address = "/axc"
            )
        }

        assertTrue {
            OscAddressMatcher.matches(
                compiled = "/a*bc".compile(),
                address = "/abbbbbbc"
            )
        }

        assertFalse {
            OscAddressMatcher.matches(
                compiled = "/a*c".compile(),
                address = "/ab"
            )
        }

        assertFalse {
            OscAddressMatcher.matches(
                compiled = "/a/*/c".compile(),
                address = "/a/x/y/c"
            )
        }
    }

    @Test
    fun `leading slash is required`() {
        assertFailsWith<IllegalArgumentException> {
            OscAddressMatcher.matches(
                compiled = "/a/*".compile(),
                address = "a/x"
            )
        }

        assertFailsWith<OscAddressParseException> {
            OscAddressMatcher.matches(
                compiled = "a/*".compile(),
                address = "/a/x"
            )
        }
    }

    @Test
    fun `multiple stars in one segment`() {
        assertTrue {
            OscAddressMatcher.matches(
                compiled = "/a**c".compile(),
                address = "/abc"
            )
        }

        assertTrue {
            OscAddressMatcher.matches(
                compiled = "/a**c".compile(),
                address = "/abbbbbc"
            )
        }
    }

    @Test
    fun `trailing star test`() {
        assertTrue {
            OscAddressMatcher.matches(
                compiled = "/a*".compile(),
                address = "/a"
            )
        }

        assertTrue {
            OscAddressMatcher.matches(
                compiled = "/a*".compile(),
                address = "/abc"
            )
        }
    }

    @Test
    fun `empty segment should not match`() {
        assertFailsWith<IllegalArgumentException> {
            OscAddressMatcher.matches(
                compiled = "/a/*/c".compile(),
                address = "/a//c"
            )
        }

        assertFailsWith<OscAddressParseException> {
            OscAddressMatcher.matches(
                compiled = "/a//c".compile(),
                address = "/a/x/c"
            )
        }
    }

    @Test
    fun `single char matching test`() {
        assertTrue {
            OscAddressMatcher.matches(
                compiled = "/a?c".compile(),
                address = "/abc"
            )
        }

        assertFalse {
            OscAddressMatcher.matches(
                compiled = "/a?c".compile(),
                address = "/ac"
            )
        }

        assertTrue {
            OscAddressMatcher.matches(
                compiled = "/?".compile(),
                address = "/a"
            )
        }
    }

    @Test
    fun `combination matching test`() {
        assertTrue {
            OscAddressMatcher.matches(
                compiled = "/a*?c".compile(),
                address = "/abbbc"
            )
        }

        assertFalse {
            OscAddressMatcher.matches(
                compiled = "/a*?c".compile(),
                address = "/ac"
            )
        }

        assertTrue {
            OscAddressMatcher.matches(
                compiled = "/a?*c".compile(),
                address = "/abbbc"
            )
        }
    }

    @Test
    fun `segment matching boundary test`() {
        assertTrue {
            OscAddressMatcher.matches(
                compiled = "/a?/*".compile(),
                address = "/ab/c"
            )
        }

        assertFalse {
            OscAddressMatcher.matches(
                compiled = "/a?/*".compile(),
                address = "/a/x"
            )
        }
    }

    @Test
    fun `empty segment matching test`() {
        assertFailsWith<IllegalArgumentException> {
            OscAddressMatcher.matches(
                compiled = "/?".compile(),
                address = "/"
            )
        }

        assertFailsWith<IllegalArgumentException> {
            OscAddressMatcher.matches(
                compiled = "/?/*".compile(),
                address = "/a/"
            )
        }
    }

    @Test
    fun `char class basic test`() {
        val pattern = "/[abc]".compile()
        assertTrue { OscAddressMatcher.matches(pattern, "/a") }
        assertTrue { OscAddressMatcher.matches(pattern, "/b") }
        assertTrue { OscAddressMatcher.matches(pattern, "/c") }
        assertFalse { OscAddressMatcher.matches(pattern, "/d") }


        val pattern2 = "/[abc]d".compile()
        assertTrue { OscAddressMatcher.matches(pattern2, "/ad") }
        assertTrue { OscAddressMatcher.matches(pattern2, "/bd") }
        assertFalse { OscAddressMatcher.matches(pattern2, "/c") }
        assertFalse { OscAddressMatcher.matches(pattern2, "/dd") }
    }

    @Test
    fun `char class range test`() {
        val pattern = "/[a-c]".compile()
        assertTrue { OscAddressMatcher.matches(pattern, "/a") }
        assertTrue { OscAddressMatcher.matches(pattern, "/b") }
        assertTrue { OscAddressMatcher.matches(pattern, "/c") }
        assertFalse { OscAddressMatcher.matches(pattern, "/d") }

        val upper = "/[A-C]".compile()
        assertTrue { OscAddressMatcher.matches(upper, "/A") }
        assertTrue { OscAddressMatcher.matches(upper, "/B") }
        assertFalse { OscAddressMatcher.matches(upper, "/D") }

        val digits = "/[0-2]".compile()
        assertTrue { OscAddressMatcher.matches(digits, "/0") }
        assertTrue { OscAddressMatcher.matches(digits, "/2") }
        assertFalse { OscAddressMatcher.matches(digits, "/3") }
    }

    @Test
    fun `char class dash literal test`() {
        val pattern = "/[-ac]".compile()
        assertTrue { OscAddressMatcher.matches(pattern, "/a") }
        assertFalse { OscAddressMatcher.matches(pattern, "/b") }
        assertTrue { OscAddressMatcher.matches(pattern, "/c") }
        assertFalse { OscAddressMatcher.matches(pattern, "/d") }
        assertTrue { OscAddressMatcher.matches(pattern, "/-") }

        val pattern2 = "/[ac-]".compile()
        assertTrue { OscAddressMatcher.matches(pattern2, "/a") }
        assertFalse { OscAddressMatcher.matches(pattern2, "/b") }
        assertTrue { OscAddressMatcher.matches(pattern2, "/c") }
        assertFalse { OscAddressMatcher.matches(pattern2, "/d") }
        assertTrue { OscAddressMatcher.matches(pattern2, "/-") }
    }

    @Test
    fun `char class negation test`() {
        val pattern = "/[!a-c]".compile()
        assertTrue { OscAddressMatcher.matches(pattern, "/z") }
        assertFalse { OscAddressMatcher.matches(pattern, "/b") }

        val pattern2 = "/[^a-c]".compile()
        assertTrue { OscAddressMatcher.matches(pattern2, "/z") }
        assertFalse { OscAddressMatcher.matches(pattern2, "/b") }
    }

    @Test
    fun `multiple char class range`() {
        val pattern = "/[a-c0-5]".compile()
        assertTrue { OscAddressMatcher.matches(pattern, "/b") }
        assertTrue { OscAddressMatcher.matches(pattern, "/2") }
        assertFalse { OscAddressMatcher.matches(pattern, "/d") }

        val pattern2 = "/[a-cxyz0-5]".compile()
        assertTrue { OscAddressMatcher.matches(pattern2, "/b") }
        assertTrue { OscAddressMatcher.matches(pattern2, "/2") }
        assertTrue { OscAddressMatcher.matches(pattern2, "/y") }
        assertFalse { OscAddressMatcher.matches(pattern2, "/d") }
    }

    @Test
    fun `char class should support backtrack with star`() {
        val pattern = "/a*[bc]d".compile()
        assertTrue { OscAddressMatcher.matches(pattern, "/axxbd") }
        assertTrue { OscAddressMatcher.matches(pattern, "/abbd") }
        assertFalse { OscAddressMatcher.matches(pattern, "/axxdd") }
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
            assertTrue(OscAddressMatcher.matches(pattern, "/a"))
            assertTrue(OscAddressMatcher.matches(pattern, "/z"))
            assertFalse(OscAddressMatcher.matches(pattern, "/0"))
        }

        "/[-a]".compile().let { pattern ->
            assertTrue(OscAddressMatcher.matches(pattern, "/a"))
            assertTrue(OscAddressMatcher.matches(pattern, "/-"))
            assertFalse(OscAddressMatcher.matches(pattern, "/b"))
        }

        "/[a-]".compile().let { pattern ->
            assertTrue(OscAddressMatcher.matches(pattern, "/a"))
            assertTrue(OscAddressMatcher.matches(pattern, "/-"))
            assertFalse(OscAddressMatcher.matches(pattern, "/b"))
        }

        "/[!-]".compile().let { pattern ->
            assertTrue(OscAddressMatcher.matches(pattern, "/a"))
            assertFalse(OscAddressMatcher.matches(pattern, "/-"))
            assertTrue(OscAddressMatcher.matches(pattern, "/b"))
        }
    }

    @Test
    fun `alternation set basic test`() {
        val pattern1 = "/{foo,bar}".compile()
        assertTrue { OscAddressMatcher.matches(pattern1, "/foo") }
        assertTrue { OscAddressMatcher.matches(pattern1, "/bar") }
        assertFalse { OscAddressMatcher.matches(pattern1, "/baz") }

        val pattern2 = "/{a,bbb}".compile()
        assertTrue { OscAddressMatcher.matches(pattern2, "/a") }
        assertTrue { OscAddressMatcher.matches(pattern2, "/bbb") }
        assertFalse { OscAddressMatcher.matches(pattern2, "/baz") }

        val pattern3 = "/{bbb,a}".compile()
        assertTrue { OscAddressMatcher.matches(pattern3, "/a") }
        assertTrue { OscAddressMatcher.matches(pattern3, "/bbb") }
        assertFalse { OscAddressMatcher.matches(pattern3, "/baz") }
    }

    @Test
    fun `alternation single branch test`() {
        val pattern1 = "/{abc}".compile()
        assertTrue { OscAddressMatcher.matches(pattern1, "/abc") }
        assertFalse { OscAddressMatcher.matches(pattern1, "/abd") }
    }

    @Test
    fun `alternation & literal test`() {
        val pattern1 = "/x{ab,cd}y".compile()
        println(pattern1)
        assertTrue { OscAddressMatcher.matches(pattern1, "/xaby") }
        assertTrue { OscAddressMatcher.matches(pattern1, "/xcdy") }
        assertFalse { OscAddressMatcher.matches(pattern1, "/xay") }
    }

    @Test
    fun `alternation & star & anyChar combination test`() {
        val pattern1 = "/{a?,b*}".compile()
        println(pattern1)
        assertTrue { OscAddressMatcher.matches(pattern1, "/aa") }
        assertTrue { OscAddressMatcher.matches(pattern1, "/bbbbbbb") }
        assertFalse { OscAddressMatcher.matches(pattern1, "/abc") }
    }

    @Test
    fun `alternation & char class combination test`() {
        val pattern1 = "/{[a-c]y,cd}".compile()
        assertTrue { OscAddressMatcher.matches(pattern1, "/ay") }
        assertTrue { OscAddressMatcher.matches(pattern1, "/cd") }
        assertFalse { OscAddressMatcher.matches(pattern1, "/dy") }

        val pattern2 = "/{[a-c],cd}".compile()
        assertTrue { OscAddressMatcher.matches(pattern2, "/a") }
        assertTrue { OscAddressMatcher.matches(pattern2, "/cd") }
        assertFalse { OscAddressMatcher.matches(pattern2, "/ab") }
    }


    @Test
    fun `alternation illegal pattern test`() {
        assertFailsWith<OscAddressParseException> { "/{}".compile() }
        assertFailsWith<OscAddressParseException> { "/{a,,b}".compile() }
        assertFailsWith<OscAddressParseException> { "/{ab".compile() }
        assertFailsWith<OscAddressParseException> { "/ab}".compile() }
    }
}
