package io.github.termtate.kotlinosc.matcher

import io.github.termtate.kotlinosc.exception.OscAddressParseException
import io.github.termtate.kotlinosc.pattern.compile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OscAddressMatcherAlternationTest {
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
    fun `alternation and literal test`() {
        val pattern1 = "/x{ab,cd}y".compile()
        assertTrue { strictMatcher.matches(pattern1, "/xaby") }
        assertTrue { strictMatcher.matches(pattern1, "/xcdy") }
        assertFalse { strictMatcher.matches(pattern1, "/xay") }
    }

    @Test
    fun `alternation with wildcard and anyChar combination test`() {
        val pattern1 = "/{a?,b*}".compile()
        assertTrue { strictMatcher.matches(pattern1, "/aa") }
        assertTrue { strictMatcher.matches(pattern1, "/bbbbbbb") }
        assertFalse { strictMatcher.matches(pattern1, "/abc") }
    }

    @Test
    fun `alternation with char class combination test`() {
        val pattern1 = "/{[a-c]y,cd}".compile()
        assertTrue { strictMatcher.matches(pattern1, "/ay") }
        assertTrue { strictMatcher.matches(pattern1, "/cd") }
        assertFalse { strictMatcher.matches(pattern1, "/dy") }

        val pattern2 = "/{[a-c],cd}".compile()
        assertTrue { strictMatcher.matches(pattern2, "/a") }
        assertTrue { strictMatcher.matches(pattern2, "/cd") }
        assertFalse { strictMatcher.matches(pattern2, "/ab") }

        assertFailsWith<OscAddressParseException> { "/{ab, c[d}]".compile() }
    }

    @Test
    fun `nested alternation basic test`() {
        val pattern = "/{a,{b,c}}".compile()
        assertTrue { strictMatcher.matches(pattern, "/a") }
        assertTrue { strictMatcher.matches(pattern, "/b") }
        assertTrue { strictMatcher.matches(pattern, "/c") }
        assertFalse { strictMatcher.matches(pattern, "/d") }
    }

    @Test
    fun `nested alternation should compose with literals wildcard and char class`() {
        val pattern1 = "/x{ab,{c?,d*}}y".compile()
        assertTrue { strictMatcher.matches(pattern1, "/xaby") }
        assertTrue { strictMatcher.matches(pattern1, "/xcry") }
        assertTrue { strictMatcher.matches(pattern1, "/xddddy") }
        assertFalse { strictMatcher.matches(pattern1, "/xcy") }

        val pattern2 = "/{{[ab],c},de}".compile()
        assertTrue { strictMatcher.matches(pattern2, "/a") }
        assertTrue { strictMatcher.matches(pattern2, "/b") }
        assertTrue { strictMatcher.matches(pattern2, "/c") }
        assertTrue { strictMatcher.matches(pattern2, "/de") }
        assertFalse { strictMatcher.matches(pattern2, "/d") }
    }

    @Test
    fun `alternation illegal pattern test`() {
        assertFailsWith<OscAddressParseException> { "/{}".compile() }
        assertFailsWith<OscAddressParseException> { "/{a,,b}".compile() }
        assertFailsWith<OscAddressParseException> { "/{ab".compile() }
        assertFailsWith<OscAddressParseException> { "/ab}".compile() }
        assertFailsWith<OscAddressParseException> { "/{,a}".compile() }
        assertFailsWith<OscAddressParseException> { "/{a,{,b}}".compile() }
        assertFailsWith<OscAddressParseException> { "/{a,{b,}}".compile() }
        assertFailsWith<OscAddressParseException> { "/{a,{b/c}}".compile() }
    }
}
