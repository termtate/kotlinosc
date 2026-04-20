package io.github.termtate.kotlinosc.pattern

import io.github.termtate.kotlinosc.exception.OscAddressParseException
import io.github.termtate.kotlinosc.util.OscLogger
import io.github.termtate.kotlinosc.util.logger

/**
 * Tokenized representation of one address segment.
 */
internal sealed class Token(val minLen: Int, val maxLen: Int?) {
    data class Literal(val ch: Char) : Token(1, 1)
    data object AnyChar : Token(1, 1)           // '?'
    data object Star : Token(0, null)           // '*'

    /**
     * Character class token, for example `[abc]`, `[a-z]`, or negated forms.
     */
    data class CharClassToken(
        val singles: Set<Char>,
        val negated: Boolean,
        val ranges: List<CharRange>
    ) : Token(1, 1) {
        fun hit(c: Char): Boolean = (c in singles || ranges.any { c in it }).let {
            if (negated) {
                it.not()
            } else {
                it
            }
        }
    }

    /**
     * Alternation token, for example `{foo,bar,baz}`.
     */
    data class Alternation(
        val branches: List<Tokens>,
        private val _minLen: Int,
        private val _maxLen: Int?
    ) : Token(_minLen, _maxLen)
}

internal typealias Tokens = List<Token>

internal data class SegmentPattern(
    val tokens: Tokens,
    val minLen: Int,
    val maxLen: Int?
)

internal data class OscAddressCompiledPattern(
    val segments: List<SegmentPattern>
)

private class Cursor(private val s: String) {
    var i: Int = 0
        private set

    fun eof(): Boolean = i >= s.length
    fun peek(): Char? = if (eof()) null else s[i]
    fun next(): Char = s[i++]
    fun back(): Char = s[--i]
    fun previous(): Char = s[i - 1]
    fun match(ch: Char): Boolean = (peek() == ch).also { match -> if (match) i++ }
    fun expect(ch: Char, msg: String) {
        if (peek() != ch) error(msg)
    }
    fun error(msg: String): Nothing = throw OscAddressParseException("$msg at index $i")
}

internal object OscAddressCompiler : OscLogger {
    override val logTag: String
        get() = "OscAddressCompiler"

    fun compile(pattern: String): OscAddressCompiledPattern {
        logger.debug { "Compiling OSC address pattern: $pattern" }
        if (!pattern.startsWith('/')) {
            throw OscAddressParseException("pattern should start with '/'")
        }

        val segments = mutableListOf<SegmentPattern>()
        val cursor = Cursor(pattern)
        cursor.next()

        while (!cursor.eof()) {
            val tokens = parseTokens(cursor, emptySet(), slashAllowed = true)
            val segment = flushSegment(tokens)
            if (logger.isTraceEnabled()) {
                logger.trace {
                    "Compiled segment #${segments.size}: tokens=${segment.tokens.size}, minLen=${segment.minLen}, maxLen=${segment.maxLen ?: "INF"}"
                }
            }
            segments += segment
        }

        return OscAddressCompiledPattern(segments).also {
            logger.debug { "Pattern compiled successfully: segments=${it.segments.size}" }
        }
    }

    private fun flushSegment(tokens: Tokens): SegmentPattern {
        if (tokens.isEmpty()) {
            throw OscAddressParseException("pattern segment must not be empty")
        }
        var minLen = 0
        var maxLen: Int? = 0
        for (t in tokens) {
            minLen += t.minLen
            maxLen = t.maxLen?.let { tMax -> maxLen?.let { it + tMax } }
        }
        return SegmentPattern(
            tokens = tokens,
            minLen = minLen,
            maxLen = maxLen
        )
    }

    private fun parseTokens(cursor: Cursor, stopChars: Set<Char>, slashAllowed: Boolean): Tokens {
        val tokens = mutableListOf<Token>()

        while (!cursor.eof()) {
            when (val c = cursor.next()) {
                in stopChars -> {
                    return tokens
                }
                SEGMENT_SEPARATOR -> {
                    if (slashAllowed) {
                        return tokens
                    } else {
                        cursor.error("'/' cannot be inside of alternation set")
                    }
                }
                STAR_CHAR -> tokens += Token.Star
                ANY_CHAR -> tokens += Token.AnyChar
                CHAR_CLASS_START -> {
                    val token = parseCharClass(cursor)
                    tokens += token
                }
                CHAR_CLASS_END -> {
                    cursor.error("redundant ] char class bracket")
                }

                ALTERNATION_START -> {
                    val token = parseAlternation(cursor)
                    tokens += token
                }
                ALTERNATION_END -> {
                    cursor.error("redundant } alternation bracket")
                }

                else -> {
                    tokens += Token.Literal(c)
                }
            }
        }

        return tokens
    }

    private fun parseCharClass(cursor: Cursor): Token.CharClassToken {
        var negated = false
        val chars = mutableSetOf<Char>()
        val ranges = mutableListOf<CharRange>()
        var pendingChar: Char? = null

        if (cursor.peek() == CHAR_CLASS_NEGATE_BANG || cursor.peek() == CHAR_CLASS_NEGATE_CARET) {
            negated = true
            cursor.next()
        }

        while (!cursor.eof()) {
            when (val c = cursor.next()) {
                CHAR_CLASS_RANGE_DASH -> {
                    val nextChar = cursor.peek()
                    if (pendingChar == null || nextChar == null || nextChar == CHAR_CLASS_END) {
                        chars.add(c)
                    } else {
                        if (pendingChar <= nextChar) {
                            ranges.add(pendingChar..nextChar)
                        } else {
                            cursor.error("the start of char class range must be less than the end of char class range: $pendingChar-$nextChar")
                        }
                    }
                }
                CHAR_CLASS_END -> {
                    if (chars.isEmpty() && ranges.isEmpty()) {
                        cursor.error("empty char class")
                    }

                    return Token.CharClassToken(chars, negated, ranges).also { token ->
                        if (logger.isTraceEnabled()) {
                            logger.trace {
                                "Parsed char class: negated=${token.negated}, singles=${token.singles.size}, ranges=${token.ranges.size}"
                            }
                        }
                    }
                }
                else -> {
                    chars.add(c)
                    pendingChar = c
                }
            }
        }

        throw OscAddressParseException("Unclosed [ char class bracket")
    }

    private fun parseAlternation(cursor: Cursor): Token.Alternation {
        val branches = mutableListOf<Tokens>()
        var minLen: Int? = null
        var maxLen: Int? = 0

        while (!cursor.eof()) {
            val branch = parseTokens(cursor, stopChars = setOf(',', '}'), slashAllowed = false)
            if (branch.isEmpty()) {
                cursor.error("empty alternation branch")
            }
            branches += branch

            var tokensMinLen = 0

            var tokensMaxLen: Int? = 0

            for (t in branch) {
                tokensMinLen += t.minLen

                if (t.maxLen == null) {
                    tokensMaxLen = null
                } else {
                    if (tokensMaxLen != null) {
                        tokensMaxLen += t.maxLen
                    }
                }
            }


            minLen = minLen?.let { minOf(it, tokensMinLen) } ?: tokensMinLen
            maxLen = tokensMaxLen?.let { tMax -> maxLen?.let { max -> maxOf(max, tMax) } }

            when (cursor.previous()) {
                ALTERNATION_END -> {
                    return Token.Alternation(branches, minLen, maxLen).also { token ->
                        logger.debug {
                            "Parsed alternation: branches=${token.branches.size}, minLen=${token.minLen}, maxLen=${token.maxLen ?: "INF"}"
                        }
                    }
                }
                ALTERNATION_SEPARATOR -> {
                    continue
                }
                else -> {
                    cursor.error("Unclosed alternation bracket")
                }
            }
        }

        cursor.error("Unclosed alternation bracket")
    }
}

internal fun String.compile() = OscAddressCompiler.compile(this)

