package org.example.pattern

import org.example.exception.OscAddressParseException
import org.example.util.OscLogger
import org.example.util.logger

/**
 * @property minLen 这个token最少能匹配多少字符
 * @property maxLen 这个token最多能匹配多少字符，null代表+∞无上界
 */
internal sealed class Token(val minLen: Int, val maxLen: Int?) {
    data class Literal(val ch: Char) : Token(1, 1)
    data object AnyChar : Token(1, 1)           // '?'
    data object Star : Token(0, null)           // '*'

    /**
     * @property singles 包含的单个字符集
     * @property negated char class是否对匹配结果取反（开头是否有 ! or ^）
     * @property ranges 包含的字符范围
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
     * @property _minLen 各分支 minLen 的最小值
     * @property _maxLen 各分支 maxLen 的最大值
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
            val tokens = parseTokens(cursor)
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

    private fun parseTokens(cursor: Cursor): Tokens {
        val tokens = mutableListOf<Token>()

        while (!cursor.eof()) {
            when (val c = cursor.next()) {
                SEGMENT_SEPARATOR -> {
                    return tokens
                }
                STAR_CHAR -> tokens += Token.Star
                ANY_CHAR -> tokens += Token.AnyChar
                '[' -> {
                    val token = parseCharClass(cursor)
                    tokens += token
                }
                ']' -> {
                    cursor.error("redundant ] char class bracket")
                }

                '{' -> {
                    val token = parseAlternation(cursor)
                    tokens += token
                }
                '}' -> {
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

        if (cursor.peek() == '!' || cursor.peek() == '^') {
            negated = true
            cursor.next()
        }

        while (!cursor.eof()) {
            when (val c = cursor.next()) {
                '-' -> {
                    val nextChar = cursor.peek()
                    if (pendingChar == null || nextChar == null || nextChar == ']') {
                        chars.add(c)
                    } else {
                        if (pendingChar <= nextChar) {
                            ranges.add(pendingChar..nextChar)
                        } else {
                            cursor.error("the start of char class range must be less than the end of char class range: $pendingChar-$nextChar")
                        }
                    }
                }
                ']' -> {
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
        var tokens = mutableListOf<Token>()

        fun flushTokens() {
            if (tokens.isEmpty()) {
                cursor.error("empty alternation branch")
            }
            branches += tokens

            var tokensMinLen = 0

            var tokensMaxLen: Int? = 0

            for (t in tokens) {
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
            if (logger.isTraceEnabled()) {
                logger.trace {
                    "Parsed alternation branch #${branches.size}: tokens=${tokens.size}, branchMinLen=$tokensMinLen, branchMaxLen=${tokensMaxLen ?: "INF"}"
                }
            }

            tokens = mutableListOf()
        }

        while (!cursor.eof()) {
            when (val c = cursor.next()) {
                SEGMENT_SEPARATOR -> cursor.error("'/' cannot be inside of alternation set")
                STAR_CHAR -> tokens += Token.Star
                ANY_CHAR -> tokens += Token.AnyChar
                '[' -> {
                    val token = parseCharClass(cursor)
                    tokens += token
                }
                ']' -> cursor.error("redundant ] char class bracket")
                '{' -> cursor.error("nested alternation bracket is unsupported")  // TODO
                '}' -> {
                    flushTokens()
                    return Token.Alternation(branches, minLen!!, maxLen).also { token ->
                        logger.debug {
                            "Parsed alternation: branches=${token.branches.size}, minLen=${token.minLen}, maxLen=${token.maxLen ?: "INF"}"
                        }
                    }
                }
                ',' -> {
                    flushTokens()
                }
                else -> tokens += Token.Literal(c)
            }
        }

        throw OscAddressParseException("Unclosed alternation bracket")
    }
}

internal fun String.compile() = OscAddressCompiler.compile(this)
