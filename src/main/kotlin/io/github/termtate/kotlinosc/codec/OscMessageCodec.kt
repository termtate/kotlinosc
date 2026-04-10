package io.github.termtate.kotlinosc.codec

import io.github.termtate.kotlinosc.io.OscByteReader
import io.github.termtate.kotlinosc.io.OscByteWriter
import io.github.termtate.kotlinosc.type.OscMessage
import io.github.termtate.kotlinosc.arg.OscBlob
import io.github.termtate.kotlinosc.arg.OscFalse
import io.github.termtate.kotlinosc.arg.OscFloat32
import io.github.termtate.kotlinosc.arg.OscFloat64
import io.github.termtate.kotlinosc.arg.OscInfinitum
import io.github.termtate.kotlinosc.arg.OscArg
import io.github.termtate.kotlinosc.arg.OscString
import io.github.termtate.kotlinosc.arg.OscInt32
import io.github.termtate.kotlinosc.arg.OscInt64
import io.github.termtate.kotlinosc.arg.OscNil
import io.github.termtate.kotlinosc.arg.OscArray
import io.github.termtate.kotlinosc.arg.OscChar
import io.github.termtate.kotlinosc.arg.OscMIDI
import io.github.termtate.kotlinosc.arg.OscRGBA
import io.github.termtate.kotlinosc.arg.OscSymbol
import io.github.termtate.kotlinosc.arg.OscTimetag
import io.github.termtate.kotlinosc.arg.OscTrue
import io.github.termtate.kotlinosc.exception.OscArrayTagParseException
import io.github.termtate.kotlinosc.exception.OscTypeTagParseException
import io.github.termtate.kotlinosc.exception.OscUnknownTypeTagException

internal object OscMessageCodec : OscPacketCodec<OscMessage> {
    override fun encode(packet: OscMessage, writer: OscByteWriter): ByteArray {
        writer.writeString(packet.address)
        writer.writeString(
            packet.args.joinToString(
                "",
                prefix = OscArg.START_SEP.toString()
            ) { arg -> arg.tag }
        )
        for (arg in packet.args) {
            writer.writeArg(arg)
        }
        return writer.toByteArray()
    }

    override fun decode(reader: OscByteReader): OscMessage {
        val address = reader.readString()
        val tags = reader.readString()
        if (tags.firstOrNull() != OscArg.START_SEP) {
            throw OscTypeTagParseException("Invalid type tag content: type tag string need to be started with ','")
        }

        fun parseTags(startIndex: Int, insideArray: Boolean): Pair<List<TagNode>, Int> {
            var index = startIndex
            val nodes = buildList {
                while (index < tags.length) {
                    when (val tag = tags[index]) {
                        OscArray.LEFT_BRACKET -> {
                            val (children, nextIndex) = parseTags(index + 1, insideArray = true)
                            add(TagNode.Array(children))
                            index = nextIndex
                        }

                        OscArray.RIGHT_BRACKET -> {
                            if (!insideArray) {
                                throw OscArrayTagParseException("redundant ']' bracket")
                            }
                            return Pair(this, index + 1)
                        }

                        else -> {
                            add(TagNode.Normal(tag))
                            index++
                        }
                    }
                }
            }

            if (insideArray) {
                throw OscArrayTagParseException("unclosed '[' bracket")
            }
            return Pair(nodes, index)
        }

        val (tagTree, nextIndex) = parseTags(startIndex = 1, insideArray = false)
        if (nextIndex != tags.length) {
            throw OscUnknownTypeTagException("unexpected trailing type tag content")
        }

        fun parseArgs(tree: List<TagNode>): List<OscArg> {
            return buildList {
                for (node in tree) {
                    val arg = when (node) {
                        is TagNode.Normal -> reader.readArg(node.tag)

                        is TagNode.Array -> OscArray(parseArgs(node.elements))
                    }
                    add(arg)
                }
            }
        }

        return OscMessage(address, parseArgs(tagTree))
    }

    private sealed interface TagNode {
        data class Normal(val tag: Char) : TagNode
        data class Array(val elements: List<TagNode>) : TagNode
    }
}

private fun OscByteWriter.writeArg(arg: OscArg) {
    when (arg) {
        is OscInt32 -> writeInt32(arg.value)
        is OscFloat32 -> writeFloat32(arg.value)
        is OscString -> writeString(arg.value)
        is OscBlob -> writeBlob(arg.value)
        is OscTrue -> Unit
        is OscFalse -> Unit
        is OscNil -> Unit
        is OscInfinitum -> Unit
        is OscInt64 -> writeInt64(arg.value)
        is OscFloat64 -> writeFloat64(arg.value)
        is OscChar -> writeChar(arg.value)
        is OscRGBA -> writeRGBA(arg.value)
        is OscMIDI -> writeMIDI(arg.value)
        is OscTimetag -> writeInt64(arg.value)
        is OscSymbol -> writeString(arg.value)
        is OscArray -> arg.elements.forEach(::writeArg)
    }
}

private fun OscByteReader.readArg(tag: Char): OscArg {
    return when (tag) {
        OscInt32.TYPE_TAG -> OscInt32(readInt32())
        OscFloat32.TYPE_TAG -> OscFloat32(readFloat32())
        OscString.TYPE_TAG -> OscString(readString())
        OscBlob.TYPE_TAG -> OscBlob(readBlob())
        OscTrue.TYPE_TAG -> OscTrue
        OscFalse.TYPE_TAG -> OscFalse
        OscNil.TYPE_TAG -> OscNil
        OscInfinitum.TYPE_TAG -> OscInfinitum
        OscInt64.TYPE_TAG -> OscInt64(readInt64())
        OscFloat64.TYPE_TAG -> OscFloat64(readFloat64())
        OscChar.TYPE_TAG -> OscChar(readChar())
        OscRGBA.TYPE_TAG -> OscRGBA(readRGBA())
        OscMIDI.TYPE_TAG -> OscMIDI(readMIDI())
        OscTimetag.TYPE_TAG -> OscTimetag(readInt64().toULong())
        OscSymbol.TYPE_TAG -> OscSymbol(readString())
        else -> throw OscUnknownTypeTagException("Unexcepted type tag character: '$tag'")
    }
}

