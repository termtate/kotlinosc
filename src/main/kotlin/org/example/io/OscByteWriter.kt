package org.example.io

import org.example.type.MIDI
import org.example.type.RGBA
import java.io.ByteArrayOutputStream

internal class OscByteWriter {
    private val out = ByteArrayOutputStream()

    private fun align(size: Int) {
        val padded = size + 3 and -4

        repeat(padded - size) {
            out.write(0)
        }
    }

    private fun Int.to4BytesBE(): ByteArray {
        return byteArrayOf(
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte(),
        )
    }

    private fun Float.to4BytesBE(): ByteArray = this.toRawBits().to4BytesBE()

    private fun Long.to8BytesBE(): ByteArray {
        return byteArrayOf(
            (this shr 56).toByte(),
            (this shr 48).toByte(),
            (this shr 40).toByte(),
            (this shr 32).toByte(),
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte(),
        )
    }

    private fun ULong.to8BytesBE(): ByteArray {
        return byteArrayOf(
            (this shr 56).toByte(),
            (this shr 48).toByte(),
            (this shr 40).toByte(),
            (this shr 32).toByte(),
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte(),
        )
    }

    fun Double.to8BytesBE(): ByteArray = this.toRawBits().to8BytesBE()

    fun writeInt32(v: Int) {
        out.write(v.to4BytesBE())
    }

    fun writeFloat32(v: Float) {
        out.write(v.to4BytesBE())
    }

    fun writeInt64(v: Long) {
        out.write(v.to8BytesBE())
    }

    fun writeInt64(v: ULong) {
        out.write(v.to8BytesBE())
    }

    fun writeFloat64(v: Double) {
        out.write(v.to8BytesBE())
    }

    fun writeChar(v: Int) {
        out.write(v.to4BytesBE())
    }

    fun writeRGBA(v: RGBA) {
        out.write(v.r.toInt())
        out.write(v.g.toInt())
        out.write(v.b.toInt())
        out.write(v.a.toInt())
    }

    fun writeMIDI(v: MIDI) {
        out.write(v.portId.toInt())
        out.write(v.status.toInt())
        out.write(v.data1.toInt())
        out.write(v.data2.toInt())
    }

    fun writeString(v: String) {
        val bytes = v.toByteArray(Charsets.UTF_8)
        out.write(bytes)
        // ends with 00
        out.write(0)
        // align
        align(bytes.size + 1)
    }

    fun writeBlob(v: ByteArray) {
        // blob size first
        out.write(v.size.to4BytesBE())
        // data next
        out.write(v)
        // align
        align(v.size)
    }

    fun writeSizedPacket(packet: ByteArray) {
        out.write(packet.size.to4BytesBE())
        out.write(packet)
    }

    fun toByteArray(): ByteArray = out.toByteArray()
}
