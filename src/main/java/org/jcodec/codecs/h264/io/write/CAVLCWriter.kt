package org.jcodec.codecs.h264.io.write

import org.jcodec.api.NotImplementedException
import org.jcodec.common.io.BitWriter
import org.jcodec.common.tools.Debug
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A class responsible for outputting exp-Golomb values into binary stream
 *
 * @author The JCodec project
 */
object CAVLCWriter {
    @JvmStatic
    fun writeUtrace(out: BitWriter, value: Int, n: Int, message: String?) {
        out.writeNBit(value, n)
        Debug.trace(message, value)
    }

    @JvmStatic
    fun writeUE(out: BitWriter, value: Int) {
        var bits = 0
        var cumul = 0
        for (i in 0..14) {
            if (value < cumul + (1 shl i)) {
                bits = i
                break
            }
            cumul += 1 shl i
        }
        out.writeNBit(0, bits)
        out.write1Bit(1)
        out.writeNBit(value - cumul, bits)
    }

    @JvmStatic
    fun writeSE(out: BitWriter, value: Int) {
        writeUE(out, MathUtil.golomb(value))
    }

    @JvmStatic
    fun writeUEtrace(out: BitWriter, value: Int, message: String?) {
        writeUE(out, value)
        Debug.trace(message, value)
    }

    @JvmStatic
    fun writeSEtrace(out: BitWriter, value: Int, message: String?) {
        writeUE(out, MathUtil.golomb(value))
        Debug.trace(message, value)
    }

    @JvmStatic
    fun writeTE(out: BitWriter, value: Int, max: Int) {
        if (max > 1) writeUE(out, value) else out.write1Bit(value.inv() and 0x1)
    }

    @JvmStatic
    fun writeBool(out: BitWriter, value: Boolean, message: String?) {
        out.write1Bit(if (value) 1 else 0)
        Debug.trace(message, if (value) 1 else 0)
    }

    @JvmStatic
    fun writeU(out: BitWriter, i: Int, n: Int) {
        out.writeNBit(i, n)
    }

    fun writeNBit(out: BitWriter, value: Long, n: Int, message: String?) {
        for (i in 0 until n) {
            out.write1Bit((value shr n - i - 1).toInt() and 0x1)
        }
        Debug.trace(message, value)
    }

    @JvmStatic
    fun writeTrailingBits(out: BitWriter) {
        out.write1Bit(1)
        out.flush()
    }

    fun writeSliceTrailingBits() {
        throw NotImplementedException("todo")
    }
}