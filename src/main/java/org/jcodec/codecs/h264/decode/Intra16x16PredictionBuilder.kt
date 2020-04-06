package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Const
import org.jcodec.common.ArrayUtil
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Prediction builder class for intra 16x16 coded macroblocks
 *
 *
 * @author The JCodec project
 */
object Intra16x16PredictionBuilder {
    fun predictWithMode(predMode: Int, residual: Array<IntArray>, leftAvailable: Boolean, topAvailable: Boolean,
                        leftRow: ByteArray, topLine: ByteArray, topLeft: ByteArray, x: Int, pixOut: ByteArray) {
        when (predMode) {
            0 -> predictVertical(residual, topAvailable, topLine, x, pixOut)
            1 -> predictHorizontal(residual, leftAvailable, leftRow, x, pixOut)
            2 -> predictDC(residual, leftAvailable, topAvailable, leftRow, topLine, x, pixOut)
            3 -> predictPlane(residual, leftAvailable, topAvailable, leftRow, topLine, topLeft, x, pixOut)
        }
    }

    @JvmStatic
    fun lumaPred(predMode: Int, leftAvailable: Boolean, topAvailable: Boolean, leftRow: ByteArray,
                 topLine: ByteArray, topLeft: Byte, x: Int, pred: Array<ByteArray>) {
        when (predMode) {
            0 -> lumaVerticalPred(topAvailable, topLine, x, pred)
            1 -> lumaHorizontalPred(leftAvailable, leftRow, x, pred)
            2 -> lumaDCPred(leftAvailable, topAvailable, leftRow, topLine, x, pred)
            3 -> lumaPlanePred(leftRow, topLine, topLeft, x, pred)
        }
    }

    @JvmStatic
    fun lumaPredSAD(predMode: Int, leftAvailable: Boolean, topAvailable: Boolean, leftRow: ByteArray,
                    topLine: ByteArray, topLeft: Byte, x: Int, pred: ByteArray): Int {
        return when (predMode) {
            0 -> lumaVerticalPredSAD(topAvailable, topLine, x, pred)
            1 -> lumaHorizontalPredSAD(leftAvailable, leftRow, x, pred)
            2 -> lumaDCPredSAD(leftAvailable, topAvailable, leftRow, topLine, x, pred)
            3 -> lumaPlanePredSAD(leftAvailable, topAvailable, leftRow, topLine, topLeft, x, pred)
            else -> lumaDCPredSAD(leftAvailable, topAvailable, leftRow, topLine, x, pred)
        }
    }

    @JvmStatic
    fun predictVertical(residual: Array<IntArray>, topAvailable: Boolean, topLine: ByteArray, x: Int, pixOut: ByteArray) {
        var off = 0
        for (j in 0..15) {
            var i = 0
            while (i < 16) {
                pixOut[off] = MathUtil.clip(residual[H264Const.LUMA_4x4_BLOCK_LUT[off]][H264Const.LUMA_4x4_POS_LUT[off]] + topLine[x + i],
                        -128, 127).toByte()
                i++
                off++
            }
        }
    }

    fun lumaVerticalPred(topAvailable: Boolean, topLine: ByteArray, x: Int, pred: Array<ByteArray>) {
        var off = 0
        for (j in 0..15) {
            var i = 0
            while (i < 16) {
                pred[H264Const.LUMA_4x4_BLOCK_LUT[off]][H264Const.LUMA_4x4_POS_LUT[off]] = topLine[x + i]
                i++
                off++
            }
        }
    }

    fun lumaVerticalPredSAD(topAvailable: Boolean, topLine: ByteArray, x: Int, pred: ByteArray): Int {
        if (!topAvailable) return Int.MAX_VALUE
        var sad = 0
        for (j in 0..15) {
            for (i in 0..15) sad += MathUtil.abs(pred[(j shl 4) + i] - topLine[x + i])
        }
        return sad
    }

    @JvmStatic
    fun predictHorizontal(residual: Array<IntArray>, leftAvailable: Boolean, leftRow: ByteArray, x: Int,
                          pixOut: ByteArray) {
        var off = 0
        for (j in 0..15) {
            var i = 0
            while (i < 16) {
                pixOut[off] = MathUtil.clip(residual[H264Const.LUMA_4x4_BLOCK_LUT[off]][H264Const.LUMA_4x4_POS_LUT[off]] + leftRow[j], -128,
                        127).toByte()
                i++
                off++
            }
        }
    }

    fun lumaHorizontalPred(leftAvailable: Boolean, leftRow: ByteArray, x: Int, pred: Array<ByteArray>) {
        var off = 0
        for (j in 0..15) {
            var i = 0
            while (i < 16) {
                pred[H264Const.LUMA_4x4_BLOCK_LUT[off]][H264Const.LUMA_4x4_POS_LUT[off]] = leftRow[j]
                i++
                off++
            }
        }
    }

    fun lumaHorizontalPredSAD(leftAvailable: Boolean, leftRow: ByteArray, x: Int, pred: ByteArray): Int {
        if (!leftAvailable) return Int.MAX_VALUE
        var sad = 0
        for (j in 0..15) {
            for (i in 0..15) sad += MathUtil.abs(pred[(j shl 4) + i] - leftRow[j])
        }
        return sad
    }

    @JvmStatic
    fun predictDC(residual: Array<IntArray>, leftAvailable: Boolean, topAvailable: Boolean, leftRow: ByteArray,
                  topLine: ByteArray, x: Int, pixOut: ByteArray) {
        val s0 = getDC(leftAvailable, topAvailable, leftRow, topLine, x)
        for (i in 0..255) pixOut[i] = MathUtil.clip(residual[H264Const.LUMA_4x4_BLOCK_LUT[i]][H264Const.LUMA_4x4_POS_LUT[i]] + s0, -128, 127).toByte()
    }

    fun lumaDCPred(leftAvailable: Boolean, topAvailable: Boolean, leftRow: ByteArray, topLine: ByteArray, x: Int,
                   pred: Array<ByteArray>) {
        val s0 = getDC(leftAvailable, topAvailable, leftRow, topLine, x)
        for (i in pred.indices) for (j in 0 until pred[i].size) {
            pred[i][j] = (pred[i][j].toInt() + s0).toByte()
        }
    }

    fun lumaDCPredSAD(leftAvailable: Boolean, topAvailable: Boolean, leftRow: ByteArray, topLine: ByteArray, x: Int,
                      pred: ByteArray): Int {
        val s0 = getDC(leftAvailable, topAvailable, leftRow, topLine, x)
        var sad = 0
        for (i in pred.indices) sad += MathUtil.abs(pred[i] - s0)
        return sad
    }

    private fun getDC(leftAvailable: Boolean, topAvailable: Boolean, leftRow: ByteArray, topLine: ByteArray, x: Int): Int {
        val s0: Int
        s0 = if (leftAvailable && topAvailable) {
            ArrayUtil.sumByte(leftRow) + ArrayUtil.sumByte3(topLine, x, 16) + 16 shr 5
        } else if (leftAvailable) {
            ArrayUtil.sumByte(leftRow) + 8 shr 4
        } else if (topAvailable) {
            ArrayUtil.sumByte3(topLine, x, 16) + 8 shr 4
        } else {
            0
        }
        return s0
    }

    @JvmStatic
    fun predictPlane(residual: Array<IntArray>, leftAvailable: Boolean, topAvailable: Boolean, leftRow: ByteArray,
                     topLine: ByteArray, topLeft: ByteArray, x: Int, pixOut: ByteArray) {
        var H = 0
        for (i in 0..6) {
            H += (i + 1) * (topLine[x + 8 + i] - topLine[x + 6 - i])
        }
        H += 8 * (topLine[x + 15] - topLeft[0])
        var V = 0
        for (j in 0..6) {
            V += (j + 1) * (leftRow[8 + j] - leftRow[6 - j])
        }
        V += 8 * (leftRow[15] - topLeft[0])
        val c = 5 * V + 32 shr 6
        val b = 5 * H + 32 shr 6
        val a = 16 * (leftRow[15] + topLine[x + 15])
        var off = 0
        for (j in 0..15) {
            var i = 0
            while (i < 16) {
                val `val` = MathUtil.clip(a + b * (i - 7) + c * (j - 7) + 16 shr 5, -128, 127)
                pixOut[off] = MathUtil.clip(residual[H264Const.LUMA_4x4_BLOCK_LUT[off]][H264Const.LUMA_4x4_POS_LUT[off]] + `val`, -128, 127).toByte()
                i++
                off++
            }
        }
    }

    fun lumaPlanePred(leftRow: ByteArray, topLine: ByteArray, topLeft: Byte, x: Int, pred: Array<ByteArray>) {
        var H = 0
        for (i in 0..6) {
            H += (i + 1) * (topLine[x + 8 + i] - topLine[x + 6 - i])
        }
        H += 8 * (topLine[x + 15] - topLeft)
        var V = 0
        for (j in 0..6) {
            V += (j + 1) * (leftRow[8 + j] - leftRow[6 - j])
        }
        V += 8 * (leftRow[15] - topLeft)
        val c = 5 * V + 32 shr 6
        val b = 5 * H + 32 shr 6
        val a = 16 * (leftRow[15] + topLine[x + 15])
        var off = 0
        for (j in 0..15) {
            var i = 0
            while (i < 16) {
                val `val` = MathUtil.clip(a + b * (i - 7) + c * (j - 7) + 16 shr 5, -128, 127)
                pred[H264Const.LUMA_4x4_BLOCK_LUT[off]][H264Const.LUMA_4x4_POS_LUT[off]] = `val`.toByte()
                i++
                off++
            }
        }
    }

    fun lumaPlanePredSAD(leftAvailable: Boolean, topAvailable: Boolean, leftRow: ByteArray, topLine: ByteArray, topLeft: Byte, x: Int, pred: ByteArray): Int {
        if (!leftAvailable || !topAvailable) return Int.MAX_VALUE
        var H = 0
        for (i in 0..6) {
            H += (i + 1) * (topLine[x + 8 + i] - topLine[x + 6 - i])
        }
        H += 8 * (topLine[x + 15] - topLeft)
        var V = 0
        for (j in 0..6) {
            V += (j + 1) * (leftRow[8 + j] - leftRow[6 - j])
        }
        V += 8 * (leftRow[15] - topLeft)
        val c = 5 * V + 32 shr 6
        val b = 5 * H + 32 shr 6
        val a = 16 * (leftRow[15] + topLine[x + 15])
        var sad = 0
        var off = 0
        for (j in 0..15) {
            var i = 0
            while (i < 16) {
                val `val` = MathUtil.clip(a + b * (i - 7) + c * (j - 7) + 16 shr 5, -128, 127)
                sad += MathUtil.abs(pred[off] - `val`)
                i++
                off++
            }
        }
        return sad
    }
}