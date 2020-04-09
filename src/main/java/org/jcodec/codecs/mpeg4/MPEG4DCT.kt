package org.jcodec.codecs.mpeg4

import org.jcodec.common.shl
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object MPEG4DCT {
    private const val W1 = 2841
    private const val W2 = 2676
    private const val W3 = 2408
    private const val W5 = 1609
    private const val W6 = 1108
    private const val W7 = 565
    @JvmStatic
    fun idctPut(p: Array<ByteArray>, block: Array<ShortArray>, interlacing: Boolean) {
        idctRows(block[0])
        idctRows(block[1])
        idctRows(block[2])
        idctRows(block[3])
        idctRows(block[4])
        idctRows(block[5])
        var stride = 16
        val stride2 = 8
        var nextBlock = 128
        if (interlacing) {
            nextBlock = stride
            stride *= 2
        }
        idctColumnsPut(block[0], p[0], 0, stride)
        idctColumnsPut(block[1], p[0], 8, stride)
        idctColumnsPut(block[2], p[0], nextBlock, stride)
        idctColumnsPut(block[3], p[0], nextBlock + 8, stride)
        idctColumnsPut(block[4], p[1], 0, stride2)
        idctColumnsPut(block[5], p[2], 0, stride2)
    }

    @JvmStatic
    fun idctAdd(p: Array<ByteArray>, block: ShortArray, index: Int, interlacing: Boolean) {
        idctRows(block)
        when (index) {
            0 -> idctColumnsAdd(block, p[0], 0, 16)
            1 -> idctColumnsAdd(block, p[0], 8, 16)
            2 -> if (interlacing) {
                idctColumnsAdd(block, p[0], 16, 32)
            } else {
                idctColumnsAdd(block, p[0], 128, 16)
            }
            3 -> if (interlacing) {
                idctColumnsAdd(block, p[0], 24, 32)
            } else {
                idctColumnsAdd(block, p[0], 136, 16)
            }
            4 -> idctColumnsAdd(block, p[1], 0, 8)
            5 -> idctColumnsAdd(block, p[2], 0, 8)
        }
    }

    private fun clamp255(`val`: Int): Byte {
        var `val` = `val`
        `val` -= 255
        `val` = -(255 + (`val` shr 31 and `val`))
        return (-(`val` shr 31 and `val`) - 128).toByte()
    }

    fun idctColumnsPut(block: ShortArray, dst: ByteArray, dstOffset: Int, stride: Int) {
        for (i in 0..7) {
            val offset = dstOffset + i
            var X1: Int = block[i + 8 * 4] shl 8
            var X2 = block[i + 8 * 6].toInt()
            var X3 = block[i + 8 * 2].toInt()
            var X4 = block[i + 8 * 1].toInt()
            var X5 = block[i + 8 * 7].toInt()
            var X6 = block[i + 8 * 5].toInt()
            var X7 = block[i + 8 * 3].toInt()
            if (X1 or X2 or X3 or X4 or X5 or X6 or X7 == 0) {
                val v = clamp255(block[i + 8 * 0] + 32 shr 6)
                dst[offset + stride * 0] = v
                dst[offset + stride * 1] = v
                dst[offset + stride * 2] = v
                dst[offset + stride * 3] = v
                dst[offset + stride * 4] = v
                dst[offset + stride * 5] = v
                dst[offset + stride * 6] = v
                dst[offset + stride * 7] = v
                continue
            }
            var X0: Int = (block[i + 8 * 0] shl 8) + 8192
            var X8 = W7 * (X4 + X5) + 4
            X4 = X8 + (W1 - W7) * X4 shr 3
            X5 = X8 - (W1 + W7) * X5 shr 3
            X8 = W3 * (X6 + X7) + 4
            X6 = X8 - (W3 - W5) * X6 shr 3
            X7 = X8 - (W3 + W5) * X7 shr 3
            X8 = X0 + X1
            X0 -= X1
            X1 = W6 * (X3 + X2) + 4
            X2 = X1 - (W2 + W6) * X2 shr 3
            X3 = X1 + (W2 - W6) * X3 shr 3
            X1 = X4 + X6
            X4 -= X6
            X6 = X5 + X7
            X5 -= X7
            X7 = X8 + X3
            X8 -= X3
            X3 = X0 + X2
            X0 -= X2
            X2 = 181 * (X4 + X5) + 128 shr 8
            X4 = 181 * (X4 - X5) + 128 shr 8
            dst[offset + stride * 0] = clamp255(X7 + X1 shr 14)
            dst[offset + stride * 1] = clamp255(X3 + X2 shr 14)
            dst[offset + stride * 2] = clamp255(X0 + X4 shr 14)
            dst[offset + stride * 3] = clamp255(X8 + X6 shr 14)
            dst[offset + stride * 4] = clamp255(X8 - X6 shr 14)
            dst[offset + stride * 5] = clamp255(X0 - X4 shr 14)
            dst[offset + stride * 6] = clamp255(X3 - X2 shr 14)
            dst[offset + stride * 7] = clamp255(X7 - X1 shr 14)
        }
    }

    fun idctColumnsAdd(block: ShortArray, dst: ByteArray, dstOffset: Int, stride: Int) {
        for (i in 0..7) {
            val offset = dstOffset + i
            var X1: Int = block[i + 8 * 4] shl 8
            var X2 = block[i + 8 * 6].toInt()
            var X3 = block[i + 8 * 2].toInt()
            var X4 = block[i + 8 * 1].toInt()
            var X5 = block[i + 8 * 7].toInt()
            var X6 = block[i + 8 * 5].toInt()
            var X7 = block[i + 8 * 3].toInt()
            if (X1 or X2 or X3 or X4 or X5 or X6 or X7 == 0) {
                val pixel = block[i + 8 * 0] + 32 shr 6
                dst[offset + stride * 0] = MathUtil.clip(dst[offset + stride * 0] + pixel, -128, 127).toByte()
                dst[offset + stride * 1] = MathUtil.clip(dst[offset + stride * 1] + pixel, -128, 127).toByte()
                dst[offset + stride * 2] = MathUtil.clip(dst[offset + stride * 2] + pixel, -128, 127).toByte()
                dst[offset + stride * 3] = MathUtil.clip(dst[offset + stride * 3] + pixel, -128, 127).toByte()
                dst[offset + stride * 4] = MathUtil.clip(dst[offset + stride * 4] + pixel, -128, 127).toByte()
                dst[offset + stride * 5] = MathUtil.clip(dst[offset + stride * 5] + pixel, -128, 127).toByte()
                dst[offset + stride * 6] = MathUtil.clip(dst[offset + stride * 6] + pixel, -128, 127).toByte()
                dst[offset + stride * 7] = MathUtil.clip(dst[offset + stride * 7] + pixel, -128, 127).toByte()
                continue
            }
            var X0: Int = (block[i + 8 * 0] shl 8) + 8192
            var X8 = W7 * (X4 + X5) + 4
            X4 = X8 + (W1 - W7) * X4 shr 3
            X5 = X8 - (W1 + W7) * X5 shr 3
            X8 = W3 * (X6 + X7) + 4
            X6 = X8 - (W3 - W5) * X6 shr 3
            X7 = X8 - (W3 + W5) * X7 shr 3
            X8 = X0 + X1
            X0 -= X1
            X1 = W6 * (X3 + X2) + 4
            X2 = X1 - (W2 + W6) * X2 shr 3
            X3 = X1 + (W2 - W6) * X3 shr 3
            X1 = X4 + X6
            X4 -= X6
            X6 = X5 + X7
            X5 -= X7
            X7 = X8 + X3
            X8 -= X3
            X3 = X0 + X2
            X0 -= X2
            X2 = 181 * (X4 + X5) + 128 shr 8
            X4 = 181 * (X4 - X5) + 128 shr 8
            dst[offset + stride * 0] = MathUtil.clip(dst[offset + stride * 0] + (X7 + X1 shr 14), -128, 127).toByte()
            dst[offset + stride * 1] = MathUtil.clip(dst[offset + stride * 1] + (X3 + X2 shr 14), -128, 127).toByte()
            dst[offset + stride * 2] = MathUtil.clip(dst[offset + stride * 2] + (X0 + X4 shr 14), -128, 127).toByte()
            dst[offset + stride * 3] = MathUtil.clip(dst[offset + stride * 3] + (X8 + X6 shr 14), -128, 127).toByte()
            dst[offset + stride * 4] = MathUtil.clip(dst[offset + stride * 4] + (X8 - X6 shr 14), -128, 127).toByte()
            dst[offset + stride * 5] = MathUtil.clip(dst[offset + stride * 5] + (X0 - X4 shr 14), -128, 127).toByte()
            dst[offset + stride * 6] = MathUtil.clip(dst[offset + stride * 6] + (X3 - X2 shr 14), -128, 127).toByte()
            dst[offset + stride * 7] = MathUtil.clip(dst[offset + stride * 7] + (X7 - X1 shr 14), -128, 127).toByte()
        }
    }

    fun idctRows(block: ShortArray) {
        for (i in 0..7) {
            val offset = i shl 3
            var X1: Int = block[offset + 4] shl 11
            var X2 = block[offset + 6].toInt()
            var X3 = block[offset + 2].toInt()
            var X4 = block[offset + 1].toInt()
            var X5 = block[offset + 7].toInt()
            var X6 = block[offset + 5].toInt()
            var X7 = block[offset + 3].toInt()
            if (X1 or X2 or X3 or X4 or X5 or X6 or X7 == 0) {
                val v = (block[offset] shl 3).toShort()
                block[offset] = v
                block[offset + 1] = v
                block[offset + 2] = v
                block[offset + 3] = v
                block[offset + 4] = v
                block[offset + 5] = v
                block[offset + 6] = v
                block[offset + 7] = v
                continue
            }
            var X0: Int = (block[offset] shl 11) + 128
            var X8 = W7 * (X4 + X5)
            X4 = X8 + (W1 - W7) * X4
            X5 = X8 - (W1 + W7) * X5
            X8 = W3 * (X6 + X7)
            X6 = X8 - (W3 - W5) * X6
            X7 = X8 - (W3 + W5) * X7
            X8 = X0 + X1
            X0 -= X1
            X1 = W6 * (X3 + X2)
            X2 = X1 - (W2 + W6) * X2
            X3 = X1 + (W2 - W6) * X3
            X1 = X4 + X6
            X4 -= X6
            X6 = X5 + X7
            X5 -= X7
            X7 = X8 + X3
            X8 -= X3
            X3 = X0 + X2
            X0 -= X2
            X2 = 181 * (X4 + X5) + 128 shr 8
            X4 = 181 * (X4 - X5) + 128 shr 8
            block[offset] = (X7 + X1 shr 8).toShort()
            block[offset + 1] = (X3 + X2 shr 8).toShort()
            block[offset + 2] = (X0 + X4 shr 8).toShort()
            block[offset + 3] = (X8 + X6 shr 8).toShort()
            block[offset + 4] = (X8 - X6 shr 8).toShort()
            block[offset + 5] = (X0 - X4 shr 8).toShort()
            block[offset + 6] = (X3 - X2 shr 8).toShort()
            block[offset + 7] = (X7 - X1 shr 8).toShort()
        }
    }
}