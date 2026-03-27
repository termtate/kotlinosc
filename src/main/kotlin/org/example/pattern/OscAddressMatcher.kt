package org.example.pattern

import org.example.config.OscSettings
import org.example.util.OscLogger
import org.example.util.logger


internal object OscAddressMatcher : OscLogger {
    override val logTag: String
        get() = "OscAddressMatcher"

    fun matches(compiled: OscAddressCompiledPattern, address: String): Boolean {
        if (!address.startsWith('/')) {
            if (OscSettings.AddressPattern.strict) {
                throw IllegalArgumentException("address should start with '/'")
            } else {
                return false
            }
        }

        val segments = compiled.segments
        var segIndex = 0
        var i = 1
        var start = 1

        fun segmentMatch(seg: SegmentPattern, text: String): Boolean {
            logger.debug { "segment match pattern: $seg, text: $text" }
            if (text.length < seg.minLen) return false
            if (seg.maxLen != null && text.length > seg.maxLen) return false

            val positions = matchTokens(seg.tokens, text, 0)
            return positions[text.length]
        }

        while (i <= address.length) {
            if (i == address.length || address[i] == SEGMENT_SEPARATOR) {
                if (segIndex >= segments.size) return false
                val segText = address.substring(start, i)
                if (segText.isEmpty()) {
                    if (OscSettings.AddressPattern.strict) {
                        throw IllegalArgumentException("address segment must not be empty")
                    } else {
                        return false
                    }
                }
                if (!segmentMatch(segments[segIndex], segText)) return false
                segIndex++
                start = i + 1
            }
            i++
        }

        return segIndex == segments.size
    }

    private fun matchTokens(tokens: Tokens, text: String, startPosition: Int): BooleanArray {
        if (logger.isTraceEnabled()) {
            logger.trace { "matchTokens start, tokens: $tokens, text: $text, startPosition: $startPosition" }
        }
        var curr = BooleanArray(text.length + 1) { false }
        curr[startPosition] = true
        var next = curr.copyOf()

        for (token in tokens) {
            for (i in next.indices) {
                next[i] = false
            }
            for (pos in 0..text.length) {
                if (curr[pos]) {
                    if (logger.isTraceEnabled()) {
                        logger.trace { "curr[pos] hit, curr: ${curr.contentToString()}, pos: $pos, current token: $token" }
                    }
                    if (token == Token.Star) {
                        // '*' matches any suffix length; fill once from the first reachable position.
                        var firstReachable = -1
                        for (k in 0..text.length) {
                            if (curr[k]) {
                                firstReachable = k
                                break
                            }
                        }
                        if (firstReachable >= 0) {
                            for (j in firstReachable..text.length) {
                                next[j] = true
                            }
                        }
                        break
                    } else {
                        if (pos >= text.length) {
                            continue
                        }
                        when (token) {
                            is Token.Literal -> if (token.ch == text[pos]) {
                                next[pos + 1] = true
                            }
                            Token.AnyChar -> next[pos + 1] = true
                            is Token.CharClassToken -> if (token.hit(text[pos])) {
                                next[pos + 1] = true
                            }
                            is Token.Alternation -> {
                                for (b in token.branches) {
                                    if (logger.isTraceEnabled()) {
                                        logger.trace { "Alternation branch matchTokens(), branch: $b" }
                                    }
                                    next overlap matchTokens(b, text, pos)
                                }
                            }
                        }
                    }
                }
            }
            curr = next.also { next = curr }
        }

        return curr
    }

    private infix fun BooleanArray.overlap(other: BooleanArray) {
        var i = 0

        while (i < size && i < other.size) {
            this[i] = this[i] || other[i]
            i++
        }
    }
}
