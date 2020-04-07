package org.jcodec.containers.mkv.util

import org.jcodec.common.and
import org.jcodec.common.or

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 *
 * EBML IO implementation
 *
 * @author The JCodec project
 */
object EbmlUtil {
    /**
     * Encodes unsigned integer with given length
     *
     * @param value
     * unsigned integer to be encoded
     * @param length
     * ebml sequence length
     * @return
     */
    @JvmStatic
    fun ebmlEncodeLen(value: Long, length: Int): ByteArray {
        val b = ByteArray(length)
        for (idx in 0 until length) {
            // Rightmost bytes should go to end of array to preserve big-endian notation
            b[length - idx - 1] = (value ushr 8 * idx and 0xFFL).toByte()
        }
        b[0] = (b[0] or (0x80 ushr length - 1)).toByte()
        return b
    }

    /**
     * Encodes unsigned integer value according to ebml convention
     *
     * @param value
     * unsigned integer to be encoded
     * @return
     */
    @JvmStatic
    fun ebmlEncode(value: Long): ByteArray {
        return ebmlEncodeLen(value, ebmlLength(value))
    }

    val lengthOptions = byteArrayOf(0, 0x80.toByte(), 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01)

    /**
     * This method is used mostly during reading EBML bitstream. It asnwers the question "What is the length of an integer (signed/unsigned) encountered in the bitstream"
     *
     * @param b
     * @return
     */
    @JvmStatic
    fun computeLength(b: Byte): Int {
        if (b.toInt() == 0x00) throw RuntimeException("Invalid head element for ebml sequence")
        var i = 1
        while (b and lengthOptions[i] == 0) i++
        return i
    }

    const val one: Long = 0x7F

    // 0x3F 0x80
    const val two: Long = 0x3F80

    // 0x1F 0xC0 0x00
    const val three: Long = 0x1FC000

    // 0x0F 0xE0 0x00 0x00
    const val four: Long = 0x0FE00000

    // 0x07 0xF0 0x00 0x00 0x00
    const val five = 0x07F0000000L

    // 0x03 0xF8 0x00 0x00 0x00 0x00
    const val six = 0x03F800000000L

    // 0x01 0xFC 0x00 0x00 0x00 0x00 0x00
    const val seven = 0x01FC0000000000L

    // 0x00 0xFE 0x00 0x00 0x00 0x00 0x00 0x00
    const val eight = 0xFE000000000000L
    val ebmlLengthMasks = longArrayOf(0, one, two, three, four, five, six, seven, eight)

    /**
     * This method is used mostly during writing EBML bitstream. It answers the following question "How many bytes should be used to encode unsigned integer value"
     *
     * @param v
     * unsigned integer to be encoded
     * @return
     */
    @JvmStatic
    fun ebmlLength(v: Long): Int {
        if (v == 0L) return 1
        var length = 8
        while (length > 0 && v and ebmlLengthMasks[length] == 0L) length--
        return length
    }

    @JvmStatic
    fun toHexString(a: ByteArray): String {
        val sb = StringBuilder()
        for (b in a) sb.append(String.format("0x%02x ", (b and 0xff)))
        return sb.toString()
    }
}