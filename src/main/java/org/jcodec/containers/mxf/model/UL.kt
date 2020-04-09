package org.jcodec.containers.mxf.model

import org.jcodec.common.Preconditions
import org.jcodec.common.StringUtils
import org.jcodec.common.and
import org.jcodec.common.shr
import org.jcodec.platform.Platform
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * An UL class that wraps UL bytes, introduced to implement custom comparison
 * rules
 *
 *
 *
 * SMPTE 298-2009
 *
 * 4.2 SMPTE-Administered Universal Label A fixed-length (16-byte) universal
 * label, defined by this standard and administered by SMPTE.
 *
 * @author The JCodec project
 */
class UL(private val bytes: ByteArray) {
    override fun hashCode(): Int {
        return bytes[4] and 0xff shl 24 or (bytes[5] and 0xff shl 16) or (bytes[6] and 0xff shl 8) or (bytes[7] and 0xff)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is UL) return false
        val other = other.bytes
        for (i in 4 until Math.min(bytes.size, other.size)) if (bytes[i] != other[i]) return false
        return true
    }

    fun maskEquals(o: UL?, mask: Int): Boolean {
        var mask = mask
        if (o == null) return false
        val other = o.bytes
        mask = mask shr 4
        var i = 4
        while (i < Math.min(bytes.size, other.size)) {
            if (mask and 0x1 == 1 && bytes[i] != other[i]) return false
            i++
            mask = mask shr 1
        }
        return true
    }

    override fun toString(): String {
        if (bytes.size == 0) return ""
        val str = CharArray(bytes.size * 3 - 1)
        var i = 0
        var j = 0
        i = 0
        while (i < bytes.size - 1) {
            str[j++] = hex[bytes[i] shr 4 and 0xf]
            str[j++] = hex[bytes[i] and 0xf]
            str[j++] = '.'
            i++
        }
        str[j++] = hex[bytes[i] shr 4 and 0xf]
        str[j++] = hex[bytes[i] and 0xf]
        return Platform.stringFromChars(str)
    }

    operator fun get(i: Int): Int {
        return bytes[i].toInt()
    }

    companion object {
        @JvmStatic
        fun newULFromInts(args: IntArray): UL {
            val bytes = ByteArray(args.size)
            for (i in args.indices) {
                bytes[i] = args[i].toByte()
            }
            return UL(bytes)
        }

        @JvmStatic
        fun newUL(ul: String): UL {
            Preconditions.checkNotNull(ul)
            val split = StringUtils.splitS(ul, ".")
            val b = ByteArray(split.size)
            for (i in split.indices) {
                val parseInt = split[i].toInt(16)
                b[i] = parseInt.toByte()
            }
            return UL(b)
        }

        private val hex = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

        @JvmStatic
        fun read(_bb: ByteBuffer): UL {
            val umid = ByteArray(16)
            _bb[umid]
            return UL(umid)
        }
    }

}