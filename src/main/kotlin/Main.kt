package org.example

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale.getDefault



//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val osc = OscMessage(
        address = "/volume",
        args = listOf(
            OscArg.Int32(17),
            OscArg.Float32(.4f),
            OscArg.String("path"),
            OscArg.Blob(byteArrayOf(1, 2, 3))
        )
    )
    val bytearray = OscMessageCode.encode(osc)
    println(bytearray.asString())

    println(OscMessageCode.decode(bytearray))

}

/**
 * 最基础 OSC 支持的参数类型（先做 i/f/s/b）
 */
sealed class OscArg(val tag: Char) {
    data class Int32(val value: Int) : OscArg('i')
    data class Float32(val value: Float) : OscArg('f')
    data class String(val value: kotlin.String) : OscArg('s')
    data class Blob(val value: ByteArray) : OscArg('b') {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Blob

            if (!value.contentEquals(other.value)) return false

            return true
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }

    companion object {
        const val START_SEP = ','
    }
}

/**
 * 最基础的 OSC Message
 * address: 例如 "/synth/1/freq"
 * args: 按顺序对应 typetag
 */
data class OscMessage(
    val address: String,
    val args: List<OscArg> = emptyList()
)

/**
 * 编解码（只做 OscMessage <-> ByteArray）
 * 注意：实现时必须处理
 * - C-string 以 0 结尾
 * - 4 字节对齐 padding
 * - int32/float32 使用 big-endian
 * - blob: int32 length + bytes + padding
 */
interface OscMessageCodec {
    /** 编码成一个 OSC packet（UDP datagram payload） */
    fun encode(message: OscMessage): ByteArray

    /** 从一个 OSC packet 解码（只支持 message；遇到 bundle 可抛异常） */
    fun decode(packet: ByteArray, offset: Int = 0, length: Int = packet.size): OscMessage
}


object OscMessageCode : OscMessageCodec {
    override fun encode(message: OscMessage): ByteArray {
        val oscByteWriter = OscByteWriter()
        // write address
        oscByteWriter.writeString(message.address)
        // write tags
        oscByteWriter.writeString(
            message.args.joinToString(
                "",
                prefix = OscArg.START_SEP.toString()
            ) { arg -> arg.tag.toString() }
        )
        // write args
        for (arg in message.args) {
            when (arg) {
                is OscArg.Int32 -> oscByteWriter.writeInt32(arg.value)
                is OscArg.Float32 -> oscByteWriter.writeFloat32(arg.value)
                is OscArg.String -> oscByteWriter.writeString(arg.value)
                is OscArg.Blob -> oscByteWriter.writeBlob(arg.value)
            }
        }
        return oscByteWriter.toByteArray()
    }

    override fun decode(packet: ByteArray, offset: Int, length: Int): OscMessage {
        require(offset >= 0 && length >= 0) { "offset and length should be positive" }
        require(offset + length <= packet.size) { "offset + length cannot be greater than packet.size" }
        val oscByteReader = OscByteReader(packet.sliceArray(offset until (offset+length)))
        val address = oscByteReader.readString()
        val tags = oscByteReader.readString()
        val tagList = buildList {
            if (tags.firstOrNull() != ',') {
                throw IllegalArgumentException("Invalid type tag content: type tag string need to be started with ','")
            }
            for (tag in tags.drop(1)) {
                val arg = when (tag) {
                    'i' -> OscArg.Int32(oscByteReader.readInt32())
                    'f' -> OscArg.Float32(oscByteReader.readFloat32())
                    's' -> OscArg.String(oscByteReader.readString())
                    'b' -> OscArg.Blob(oscByteReader.readBlob())
                    else -> throw IllegalArgumentException("Unexcepted type tag character: '$tag'")
                }
                add(arg)
            }
        }

        return OscMessage(address, tagList)
    }
}

const val ALIGN_BYTES = 4

class OscByteWriter {
    private val out = ByteArrayOutputStream()

    private fun align(size: Int) {
        if (size % ALIGN_BYTES == 0) {
            return
        }
        repeat(ALIGN_BYTES - size % ALIGN_BYTES) {
            out.write(0)
        }
    }

    private fun Int.toBytesBE(): ByteArray {
        // TODO: reuse ByteBuffer
        return ByteBuffer
            .allocate(ALIGN_BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(this)
            .array()
    }

    fun Float.toBytesBE(): ByteArray {
        // TODO: reuse ByteBuffer
        return ByteBuffer
            .allocate(ALIGN_BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .putFloat(this)
            .array()
    }

    fun writeInt32(v: Int) {
        out.write(v.toBytesBE())
    }

    fun writeFloat32(v: Float) {
        out.write(v.toBytesBE())
    }

    fun writeString(v: String) {
        val bytes = v.toByteArray(Charsets.UTF_8)
        out.write(bytes)
        // ends with 00
        out.write(0)
        /**
         * align to [ALIGN_BYTES]
          */
        align(bytes.size + 1)
    }

    fun writeBlob(v: ByteArray) {
        // blob size first
        out.write(v.size.toBytesBE())
        // data next
        out.write(v)
        // align
        align(v.size)
    }

    fun toByteArray(): ByteArray = out.toByteArray()
}

class OscByteReader(private val data: ByteArray) {
    private var position = 0

    fun readInt32(): Int {
        if (position + ALIGN_BYTES > data.size) {
            throw IllegalArgumentException("OSC-int padding overflow at $position")
        }
        return ByteBuffer
            .wrap(data, position, ALIGN_BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .int
            .also { position += ALIGN_BYTES }
    }

    fun readFloat32(): Float {
        if (position + ALIGN_BYTES > data.size) {
            throw IllegalArgumentException("OSC-float padding overflow at $position")
        }
        return ByteBuffer
            .wrap(data, position, ALIGN_BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .float
            .also { position += ALIGN_BYTES }
    }

    fun readString(): String {
        val start = position

        while (position < data.size && data[position] != 0.toByte()) {
            position++
        }
        if (position >= data.size) {
            throw IllegalArgumentException("Unterminated OSC-string at $start")
        }
        val result = String(data, start, position - start, Charsets.UTF_8)
        position++

        val consumed = position - start
        val padded = (consumed + 3) and -4
        val skip = padded - consumed
        if (position + skip > data.size) {
            throw IllegalArgumentException("OSC-string padding overflow at $start")
        }
        position += skip

        return result
    }

    fun readBlob(): ByteArray {
        val size = readInt32()
        check(size >= 0) { "osc-blob size cannot be less than zero" }

        val padded = (size + 3) and -4
        if (position + padded > data.size) {
            throw IllegalArgumentException("OSC-blob padding overflow at $position")
        }
        return data.sliceArray(position until (position + size)).also { position += padded }
    }

}

fun ByteArray.asString() =
    joinToString(" ") {
        "%02x".format(it).uppercase(getDefault())
    }