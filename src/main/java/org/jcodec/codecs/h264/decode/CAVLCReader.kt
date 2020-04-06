package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Utils2
import org.jcodec.common.io.BitReader
import org.jcodec.common.tools.Debug

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object CAVLCReader {
    @JvmStatic
    fun readNBit(bits: BitReader, n: Int, message: String?): Int {
        val `val` = bits.readNBit(n)
        Debug.trace(message, `val`)
        return `val`
    }

    fun readUE(bits: BitReader): Int {
        var cnt = 0
        while (bits.read1Bit() == 0 && cnt < 32) cnt++
        var res = 0
        if (cnt > 0) {
            val `val` = bits.readNBit(cnt).toLong()
            res = ((1 shl cnt) - 1 + `val`).toInt()
        }
        return res
    }

    @JvmStatic
    fun readUEtrace(bits: BitReader, message: String?): Int {
        val res = readUE(bits)
        Debug.trace(message, res)
        return res
    }

    @JvmStatic
    fun readSE(bits: BitReader, message: String?): Int {
        var `val` = readUE(bits)
        `val` = H264Utils2.golomb2Signed(`val`)
        Debug.trace(message, `val`)
        return `val`
    }

    @JvmStatic
    fun readBool(bits: BitReader, message: String?): Boolean {
        val res = if (bits.read1Bit() == 0) false else true
        Debug.trace(message, if (res) 1 else 0)
        return res
    }

    @JvmStatic
    fun readU(bits: BitReader, i: Int, string: String?): Int {
        return readNBit(bits, i, string)
    }

    @JvmStatic
    fun readTE(bits: BitReader, max: Int): Int {
        return if (max > 1) readUE(bits) else bits.read1Bit().inv() and 0x1
    }

    fun readME(bits: BitReader, string: String?): Int {
        return readUEtrace(bits, string)
    }

    @JvmStatic
    fun readZeroBitCount(bits: BitReader, message: String?): Int {
        var count = 0
        while (bits.read1Bit() == 0 && count < 32) count++
        if (Debug.debug) Debug.trace(message, count.toString())
        return count
    }

    @JvmStatic
    fun moreRBSPData(bits: BitReader): Boolean {
        return !(bits.remaining() < 32 && bits.checkNBit(1) == 1 && bits.checkNBit(24) shl 9 == 0)
    }
}