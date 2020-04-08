package org.jcodec.common.io

import org.jcodec.common.and
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class BitReader private constructor(private val bb: ByteBuffer) {
    private var deficit = -1
    private var curInt = -1
    private var initPos: Int
    fun fork(): BitReader {
        val fork = BitReader(bb.duplicate())
        fork.initPos = 0
        fork.curInt = curInt
        fork.deficit = deficit
        return fork
    }

    fun readInt(): Int {
        return if (bb.remaining() >= 4) {
            deficit -= 32
            bb.get() and 0xff shl 24 or (bb.get() and 0xff shl 16) or (bb.get() and 0xff shl 8) or (bb.get() and 0xff)
        } else readIntSafe()
    }

    private fun readIntSafe(): Int {
        deficit -= bb.remaining() shl 3
        var res = 0
        if (bb.hasRemaining()) res = res or (bb.get() and 0xff)
        res = res shl 8
        if (bb.hasRemaining()) res = res or (bb.get() and 0xff)
        res = res shl 8
        if (bb.hasRemaining()) res = res or (bb.get() and 0xff)
        res = res shl 8
        if (bb.hasRemaining()) res = res or (bb.get() and 0xff)
        return res
    }

    fun read1Bit(): Int {
        val ret = curInt ushr 31
        curInt = curInt shl 1
        ++deficit
        if (deficit == 32) {
            curInt = readInt()
        }
        // System.out.println(ret);
        return ret
    }

    fun readNBitSigned(n: Int): Int {
        val v = readNBit(n)
        return if (read1Bit() == 0) v else -v
    }

    fun readNBit(n: Int): Int {
        var n = n
        require(n <= 32) { "Can not read more then 32 bit" }
        var ret = 0
        if (n + deficit > 31) {
            ret = ret or (curInt ushr deficit)
            n -= 32 - deficit
            ret = ret shl n
            deficit = 32
            curInt = readInt()
        }
        if (n != 0) {
            ret = ret or (curInt ushr 32 - n)
            curInt = curInt shl n
            deficit += n
        }
        return ret
    }

    fun moreData(): Boolean {
        val remaining = bb.remaining() + 4 - (deficit + 7 shr 3)
        return remaining > 1 || remaining == 1 && curInt != 0
    }

    fun remaining(): Int {
        return (bb.remaining() shl 3) + 32 - deficit
    }

    val isByteAligned: Boolean
        get() = deficit and 0x7 == 0

    fun skip(bits: Int): Int {
        var left = bits
        if (left + deficit > 31) {
            left -= 32 - deficit
            deficit = 32
            if (left > 31) {
                val skip = Math.min(left shr 3, bb.remaining())
                bb.position(bb.position() + skip)
                left -= skip shl 3
            }
            curInt = readInt()
        }
        deficit += left
        curInt = curInt shl left
        return bits
    }

    fun skipFast(bits: Int): Int {
        deficit += bits
        curInt = curInt shl bits
        return bits
    }

    fun bitsToAlign(): Int {
        return if (deficit and 0x7 > 0) 8 - (deficit and 0x7) else 0
    }

    fun align(): Int {
        return if (deficit and 0x7 > 0) skip(8 - (deficit and 0x7)) else 0
    }

    fun check24Bits(): Int {
        if (deficit > 16) {
            deficit -= 16
            curInt = curInt or (nextIgnore16() shl deficit)
        }
        if (deficit > 8) {
            deficit -= 8
            curInt = curInt or (nextIgnore() shl deficit)
        }
        return curInt ushr 8
    }

    fun check16Bits(): Int {
        if (deficit > 16) {
            deficit -= 16
            curInt = curInt or (nextIgnore16() shl deficit)
        }
        return curInt ushr 16
    }

    fun readFast16(n: Int): Int {
        if (n == 0) return 0
        if (deficit > 16) {
            deficit -= 16
            curInt = curInt or (nextIgnore16() shl deficit)
        }
        val ret = curInt ushr 32 - n
        deficit += n
        curInt = curInt shl n
        return ret
    }

    fun checkNBit(n: Int): Int {
        require(n <= 24) { "Can not check more then 24 bit" }
        return checkNBitDontCare(n)
    }

    fun checkNBitDontCare(n: Int): Int {
        while (deficit + n > 32) {
            deficit -= 8
            curInt = curInt or (nextIgnore() shl deficit)
        }
        return curInt ushr 32 - n
    }

    private fun nextIgnore16(): Int {
        return if (bb.remaining() > 1) bb.short and 0xffff else if (bb.hasRemaining()) bb.get() and 0xff shl 8 else 0
    }

    private fun nextIgnore(): Int {
        return if (bb.hasRemaining()) bb.get() and 0xff else 0
    }

    fun curBit(): Int {
        return deficit and 0x7
    }

    fun lastByte(): Boolean {
        return bb.remaining() + 4 - (deficit shr 3) <= 1
    }

    fun terminate() {
        val putBack = 32 - deficit shr 3
        bb.position(bb.position() - putBack)
    }

    fun position(): Int {
        return (bb.position() - initPos - 4 shl 3) + deficit
    }

    /**
     * Stops this bit reader. Returns underlying ByteBuffer pointer to the next
     * byte unread byte
     */
    fun stop() {
        bb.position(bb.position() - (32 - deficit shr 3))
    }

    fun checkAllBits(): Int {
        return curInt
    }

    fun readBool(): Boolean {
        return read1Bit() == 1
    }

    companion object {
        @JvmStatic
        fun createBitReader(bb: ByteBuffer): BitReader {
            val r = BitReader(bb)
            r.curInt = r.readInt()
            r.deficit = 0
            return r
        }
    }

    init {
        initPos = bb.position()
    }
}