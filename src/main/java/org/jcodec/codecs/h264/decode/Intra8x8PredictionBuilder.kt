package org.jcodec.codecs.h264.decode

import org.jcodec.common.Preconditions
import org.jcodec.common.shl
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Builds intra prediction for intra 8x8 coded macroblocks
 *
 * @author The JCodec project
 */
class Intra8x8PredictionBuilder {
    var topBuf: ByteArray
    var leftBuf: ByteArray
    var genBuf: ByteArray
    fun predictWithMode(mode: Int, residual: IntArray, leftAvailable: Boolean, topAvailable: Boolean,
                        topLeftAvailable: Boolean, topRightAvailable: Boolean, leftRow: ByteArray, topLine: ByteArray, topLeft: ByteArray,
                        mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        when (mode) {
            0 -> {
                Preconditions.checkState(topAvailable, "")
                predictVertical(residual, topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX, blkX, blkY, pixOut)
            }
            1 -> {
                Preconditions.checkState(leftAvailable, "")
                predictHorizontal(residual, topLeftAvailable, topLeft, leftRow, mbOffX, blkX, blkY, pixOut)
            }
            2 -> predictDC(residual, topLeftAvailable, topRightAvailable, leftAvailable, topAvailable, topLeft, leftRow,
                    topLine, mbOffX, blkX, blkY, pixOut)
            3 -> {
                Preconditions.checkState(topAvailable, "")
                predictDiagonalDownLeft(residual, topLeftAvailable, topAvailable, topRightAvailable, topLeft, topLine,
                        mbOffX, blkX, blkY, pixOut)
            }
            4 -> {
                Preconditions.checkState(topAvailable && leftAvailable && topLeftAvailable, "")
                predictDiagonalDownRight(residual, topRightAvailable, topLeft, leftRow, topLine, mbOffX, blkX, blkY, pixOut)
            }
            5 -> {
                Preconditions.checkState(topAvailable && leftAvailable && topLeftAvailable, "")
                predictVerticalRight(residual, topRightAvailable, topLeft, leftRow, topLine, mbOffX, blkX, blkY, pixOut)
            }
            6 -> {
                Preconditions.checkState(topAvailable && leftAvailable && topLeftAvailable, "")
                predictHorizontalDown(residual, topRightAvailable, topLeft, leftRow, topLine, mbOffX, blkX, blkY, pixOut)
            }
            7 -> {
                Preconditions.checkState(topAvailable, "")
                predictVerticalLeft(residual, topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX, blkX, blkY,
                        pixOut)
            }
            8 -> {
                Preconditions.checkState(leftAvailable, "")
                predictHorizontalUp(residual, topLeftAvailable, topLeft, leftRow, mbOffX, blkX, blkY, pixOut)
            }
        }
        val oo1 = mbOffX + blkX
        val off1 = (blkY shl 4) + blkX + 7
        topLeft[blkY shr 2] = topLine[oo1 + 7]
        for (i in 0..7) leftRow[blkY + i] = pixOut[off1 + (i shl 4)]
        val off2 = (blkY shl 4) + blkX + 112
        for (i in 0..7) topLine[oo1 + i] = pixOut[off2 + i]
        topLeft[(blkY shr 2) + 1] = leftRow[blkY + 3]
    }

    private fun interpolateTop(topLeftAvailable: Boolean, topRightAvailable: Boolean, topLeft: ByteArray, topLine: ByteArray,
                               blkX: Int, blkY: Int, out: ByteArray) {
        val a = (if (topLeftAvailable) topLeft[blkY shr 2] else topLine[blkX]).toInt()
        out[0] = (a + (topLine[blkX].toInt() shl 1) + topLine[blkX + 1] + 2 shr 2).toByte()
        var i: Int
        i = 1
        while (i < 7) {
            out[i] = (topLine[blkX + i - 1] + (topLine[blkX + i].toInt() shl 1) + topLine[blkX + i + 1] + 2 shr 2).toByte()
            i++
        }
        if (topRightAvailable) {
            while (i < 15) {
                out[i] = (topLine[blkX + i - 1] + (topLine[blkX + i].toInt() shl 1) + topLine[blkX + i + 1] + 2 shr 2).toByte()
                i++
            }
            out[15] = (topLine[blkX + 14] + (topLine[blkX + 15].toInt() shl 1) + topLine[blkX + 15] + 2 shr 2).toByte()
        } else {
            out[7] = (topLine[blkX + 6] + (topLine[blkX + 7].toInt() shl 1) + topLine[blkX + 7] + 2 shr 2).toByte()
            i = 8
            while (i < 16) {
                out[i] = topLine[blkX + 7]
                i++
            }
        }
    }

    private fun interpolateLeft(topLeftAvailable: Boolean, topLeft: ByteArray, leftRow: ByteArray, blkY: Int, out: ByteArray) {
        val a = (if (topLeftAvailable) topLeft[blkY shr 2] else leftRow[0]).toInt()
        out[0] = (a + (leftRow[blkY].toInt() shl 1) + leftRow[blkY + 1] + 2 shr 2).toByte()
        for (i in 1..6) out[i] = (leftRow[blkY + i - 1] + (leftRow[blkY + i] shl 1) + leftRow[blkY + i + 1] + 2 shr 2).toByte()
        out[7] = (leftRow[blkY + 6] + (leftRow[blkY + 7] shl 1) + leftRow[blkY + 7] + 2 shr 2).toByte()
    }

    private fun interpolateTopLeft(topAvailable: Boolean, leftAvailable: Boolean, topLeft: ByteArray, topLine: ByteArray,
                                   leftRow: ByteArray, mbOffX: Int, blkX: Int, blkY: Int): Int {
        val a = topLeft[blkY shr 2].toInt()
        val b = (if (topAvailable) topLine[mbOffX + blkX].toInt() else a)
        val c = (if (leftAvailable) leftRow[blkY].toInt() else a)
        val aa = a shl 1
        return aa + b + c + 2 shr 2
    }

    fun copyAdd(pred: ByteArray, srcOff: Int, residual: IntArray, pixOff: Int, rOff: Int, out: ByteArray) {
        out[pixOff] = MathUtil.clip(residual[rOff] + pred[srcOff], -128, 127).toByte()
        out[pixOff + 1] = MathUtil.clip(residual[rOff + 1] + pred[srcOff + 1], -128, 127).toByte()
        out[pixOff + 2] = MathUtil.clip(residual[rOff + 2] + pred[srcOff + 2], -128, 127).toByte()
        out[pixOff + 3] = MathUtil.clip(residual[rOff + 3] + pred[srcOff + 3], -128, 127).toByte()
        out[pixOff + 4] = MathUtil.clip(residual[rOff + 4] + pred[srcOff + 4], -128, 127).toByte()
        out[pixOff + 5] = MathUtil.clip(residual[rOff + 5] + pred[srcOff + 5], -128, 127).toByte()
        out[pixOff + 6] = MathUtil.clip(residual[rOff + 6] + pred[srcOff + 6], -128, 127).toByte()
        out[pixOff + 7] = MathUtil.clip(residual[rOff + 7] + pred[srcOff + 7], -128, 127).toByte()
    }

    fun fillAdd(residual: IntArray, pixOff: Int, `val`: Int, pixOut: ByteArray) {
        var pixOff = pixOff
        var rOff = 0
        for (i in 0..7) {
            pixOut[pixOff] = MathUtil.clip(residual[rOff] + `val`, -128, 127).toByte()
            pixOut[pixOff + 1] = MathUtil.clip(residual[rOff + 1] + `val`, -128, 127).toByte()
            pixOut[pixOff + 2] = MathUtil.clip(residual[rOff + 2] + `val`, -128, 127).toByte()
            pixOut[pixOff + 3] = MathUtil.clip(residual[rOff + 3] + `val`, -128, 127).toByte()
            pixOut[pixOff + 4] = MathUtil.clip(residual[rOff + 4] + `val`, -128, 127).toByte()
            pixOut[pixOff + 5] = MathUtil.clip(residual[rOff + 5] + `val`, -128, 127).toByte()
            pixOut[pixOff + 6] = MathUtil.clip(residual[rOff + 6] + `val`, -128, 127).toByte()
            pixOut[pixOff + 7] = MathUtil.clip(residual[rOff + 7] + `val`, -128, 127).toByte()
            pixOff += 16
            rOff += 8
        }
    }

    fun predictVertical(residual: IntArray, topLeftAvailable: Boolean, topRightAvailable: Boolean, topLeft: ByteArray,
                        topLine: ByteArray, mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf)
        var pixOff = (blkY shl 4) + blkX
        var rOff = 0
        for (i in 0..7) {
            pixOut[pixOff] = MathUtil.clip(residual[rOff] + topBuf[0], -128, 127).toByte()
            pixOut[pixOff + 1] = MathUtil.clip(residual[rOff + 1] + topBuf[1], -128, 127).toByte()
            pixOut[pixOff + 2] = MathUtil.clip(residual[rOff + 2] + topBuf[2], -128, 127).toByte()
            pixOut[pixOff + 3] = MathUtil.clip(residual[rOff + 3] + topBuf[3], -128, 127).toByte()
            pixOut[pixOff + 4] = MathUtil.clip(residual[rOff + 4] + topBuf[4], -128, 127).toByte()
            pixOut[pixOff + 5] = MathUtil.clip(residual[rOff + 5] + topBuf[5], -128, 127).toByte()
            pixOut[pixOff + 6] = MathUtil.clip(residual[rOff + 6] + topBuf[6], -128, 127).toByte()
            pixOut[pixOff + 7] = MathUtil.clip(residual[rOff + 7] + topBuf[7], -128, 127).toByte()
            pixOff += 16
            rOff += 8
        }
    }

    fun predictHorizontal(residual: IntArray, topLeftAvailable: Boolean, topLeft: ByteArray, leftRow: ByteArray, mbOffX: Int,
                          blkX: Int, blkY: Int, pixOut: ByteArray) {
        interpolateLeft(topLeftAvailable, topLeft, leftRow, blkY, leftBuf)
        var pixOff = (blkY shl 4) + blkX
        var rOff = 0
        for (i in 0..7) {
            pixOut[pixOff] = MathUtil.clip(residual[rOff] + leftBuf[i], -128, 127).toByte()
            pixOut[pixOff + 1] = MathUtil.clip(residual[rOff + 1] + leftBuf[i], -128, 127).toByte()
            pixOut[pixOff + 2] = MathUtil.clip(residual[rOff + 2] + leftBuf[i], -128, 127).toByte()
            pixOut[pixOff + 3] = MathUtil.clip(residual[rOff + 3] + leftBuf[i], -128, 127).toByte()
            pixOut[pixOff + 4] = MathUtil.clip(residual[rOff + 4] + leftBuf[i], -128, 127).toByte()
            pixOut[pixOff + 5] = MathUtil.clip(residual[rOff + 5] + leftBuf[i], -128, 127).toByte()
            pixOut[pixOff + 6] = MathUtil.clip(residual[rOff + 6] + leftBuf[i], -128, 127).toByte()
            pixOut[pixOff + 7] = MathUtil.clip(residual[rOff + 7] + leftBuf[i], -128, 127).toByte()
            pixOff += 16
            rOff += 8
        }
    }

    fun predictDC(residual: IntArray, topLeftAvailable: Boolean, topRightAvailable: Boolean, leftAvailable: Boolean,
                  topAvailable: Boolean, topLeft: ByteArray, leftRow: ByteArray, topLine: ByteArray, mbOffX: Int, blkX: Int, blkY: Int,
                  pixOut: ByteArray) {
        if (topAvailable && leftAvailable) {
            interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf)
            interpolateLeft(topLeftAvailable, topLeft, leftRow, blkY, leftBuf)
            val sum1 = topBuf[0] + topBuf[1] + topBuf[2] + topBuf[3]
            val sum2 = topBuf[4] + topBuf[5] + topBuf[6] + topBuf[7]
            val sum3 = leftBuf[0] + leftBuf[1] + leftBuf[2] + leftBuf[3]
            val sum4 = leftBuf[4] + leftBuf[5] + leftBuf[6] + leftBuf[7]
            fillAdd(residual, (blkY shl 4) + blkX, sum1 + sum2 + sum3 + sum4 + 8 shr 4, pixOut)
        } else if (leftAvailable) {
            interpolateLeft(topLeftAvailable, topLeft, leftRow, blkY, leftBuf)
            val sum3 = leftBuf[0] + leftBuf[1] + leftBuf[2] + leftBuf[3]
            val sum4 = leftBuf[4] + leftBuf[5] + leftBuf[6] + leftBuf[7]
            fillAdd(residual, (blkY shl 4) + blkX, sum3 + sum4 + 4 shr 3, pixOut)
        } else if (topAvailable) {
            interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf)
            val sum1 = topBuf[0] + topBuf[1] + topBuf[2] + topBuf[3]
            val sum2 = topBuf[4] + topBuf[5] + topBuf[6] + topBuf[7]
            fillAdd(residual, (blkY shl 4) + blkX, sum1 + sum2 + 4 shr 3, pixOut)
        } else {
            fillAdd(residual, (blkY shl 4) + blkX, 0, pixOut)
        }
    }

    fun predictDiagonalDownLeft(residual: IntArray, topLeftAvailable: Boolean, topAvailable: Boolean,
                                topRightAvailable: Boolean, topLeft: ByteArray, topLine: ByteArray, mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf)
        genBuf[0] = (topBuf[0] + topBuf[2] + (topBuf[1] shl 1) + 2 shr 2).toByte()
        genBuf[1] = (topBuf[1] + topBuf[3] + (topBuf[2] shl 1) + 2 shr 2).toByte()
        genBuf[2] = (topBuf[2] + topBuf[4] + (topBuf[3] shl 1) + 2 shr 2).toByte()
        genBuf[3] = (topBuf[3] + topBuf[5] + (topBuf[4] shl 1) + 2 shr 2).toByte()
        genBuf[4] = (topBuf[4] + topBuf[6] + (topBuf[5] shl 1) + 2 shr 2).toByte()
        genBuf[5] = (topBuf[5] + topBuf[7] + (topBuf[6] shl 1) + 2 shr 2).toByte()
        genBuf[6] = (topBuf[6] + topBuf[8] + (topBuf[7] shl 1) + 2 shr 2).toByte()
        genBuf[7] = (topBuf[7] + topBuf[9] + (topBuf[8] shl 1) + 2 shr 2).toByte()
        genBuf[8] = (topBuf[8] + topBuf[10] + (topBuf[9] shl 1) + 2 shr 2).toByte()
        genBuf[9] = (topBuf[9] + topBuf[11] + (topBuf[10] shl 1) + 2 shr 2).toByte()
        genBuf[10] = (topBuf[10] + topBuf[12] + (topBuf[11] shl 1) + 2 shr 2).toByte()
        genBuf[11] = (topBuf[11] + topBuf[13] + (topBuf[12] shl 1) + 2 shr 2).toByte()
        genBuf[12] = (topBuf[12] + topBuf[14] + (topBuf[13] shl 1) + 2 shr 2).toByte()
        genBuf[13] = (topBuf[13] + topBuf[15] + (topBuf[14] shl 1) + 2 shr 2).toByte()
        genBuf[14] = (topBuf[14] + topBuf[15] + (topBuf[15] shl 1) + 2 shr 2).toByte()
        val off = (blkY shl 4) + blkX
        copyAdd(genBuf, 0, residual, off, 0, pixOut)
        copyAdd(genBuf, 1, residual, off + 16, 8, pixOut)
        copyAdd(genBuf, 2, residual, off + 32, 16, pixOut)
        copyAdd(genBuf, 3, residual, off + 48, 24, pixOut)
        copyAdd(genBuf, 4, residual, off + 64, 32, pixOut)
        copyAdd(genBuf, 5, residual, off + 80, 40, pixOut)
        copyAdd(genBuf, 6, residual, off + 96, 48, pixOut)
        copyAdd(genBuf, 7, residual, off + 112, 56, pixOut)
    }

    fun predictDiagonalDownRight(residual: IntArray, topRightAvailable: Boolean, topLeft: ByteArray, leftRow: ByteArray,
                                 topLine: ByteArray, mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        interpolateTop(true, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf)
        interpolateLeft(true, topLeft, leftRow, blkY, leftBuf)
        val tl = interpolateTopLeft(true, true, topLeft, topLine, leftRow, mbOffX, blkX, blkY)
        genBuf[0] = (leftBuf[7] + leftBuf[5] + (leftBuf[6] shl 1) + 2 shr 2).toByte()
        genBuf[1] = (leftBuf[6] + leftBuf[4] + (leftBuf[5] shl 1) + 2 shr 2).toByte()
        genBuf[2] = (leftBuf[5] + leftBuf[3] + (leftBuf[4] shl 1) + 2 shr 2).toByte()
        genBuf[3] = (leftBuf[4] + leftBuf[2] + (leftBuf[3] shl 1) + 2 shr 2).toByte()
        genBuf[4] = (leftBuf[3] + leftBuf[1] + (leftBuf[2] shl 1) + 2 shr 2).toByte()
        genBuf[5] = (leftBuf[2] + leftBuf[0] + (leftBuf[1] shl 1) + 2 shr 2).toByte()
        genBuf[6] = (leftBuf[1] + tl + (leftBuf[0] shl 1) + 2 shr 2).toByte()
        genBuf[7] = (leftBuf[0] + topBuf[0] + (tl shl 1) + 2 shr 2).toByte()
        genBuf[8] = (tl + topBuf[1] + (topBuf[0] shl 1) + 2 shr 2).toByte()
        genBuf[9] = (topBuf[0] + topBuf[2] + (topBuf[1] shl 1) + 2 shr 2).toByte()
        genBuf[10] = (topBuf[1] + topBuf[3] + (topBuf[2] shl 1) + 2 shr 2).toByte()
        genBuf[11] = (topBuf[2] + topBuf[4] + (topBuf[3] shl 1) + 2 shr 2).toByte()
        genBuf[12] = (topBuf[3] + topBuf[5] + (topBuf[4] shl 1) + 2 shr 2).toByte()
        genBuf[13] = (topBuf[4] + topBuf[6] + (topBuf[5] shl 1) + 2 shr 2).toByte()
        genBuf[14] = (topBuf[5] + topBuf[7] + (topBuf[6] shl 1) + 2 shr 2).toByte()
        val off = (blkY shl 4) + blkX
        copyAdd(genBuf, 7, residual, off, 0, pixOut)
        copyAdd(genBuf, 6, residual, off + 16, 8, pixOut)
        copyAdd(genBuf, 5, residual, off + 32, 16, pixOut)
        copyAdd(genBuf, 4, residual, off + 48, 24, pixOut)
        copyAdd(genBuf, 3, residual, off + 64, 32, pixOut)
        copyAdd(genBuf, 2, residual, off + 80, 40, pixOut)
        copyAdd(genBuf, 1, residual, off + 96, 48, pixOut)
        copyAdd(genBuf, 0, residual, off + 112, 56, pixOut)
    }

    fun predictVerticalRight(residual: IntArray, topRightAvailable: Boolean, topLeft: ByteArray, leftRow: ByteArray,
                             topLine: ByteArray, mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        interpolateTop(true, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf)
        interpolateLeft(true, topLeft, leftRow, blkY, leftBuf)
        val tl = interpolateTopLeft(true, true, topLeft, topLine, leftRow, mbOffX, blkX, blkY)
        genBuf[0] = (leftBuf[5] + leftBuf[3] + (leftBuf[4] shl 1) + 2 shr 2).toByte()
        genBuf[1] = (leftBuf[3] + leftBuf[1] + (leftBuf[2] shl 1) + 2 shr 2).toByte()
        genBuf[2] = (leftBuf[1] + tl + (leftBuf[0] shl 1) + 2 shr 2).toByte()
        genBuf[3] = (tl + topBuf[0] + 1 shr 1).toByte()
        genBuf[4] = (topBuf[0] + topBuf[1] + 1 shr 1).toByte()
        genBuf[5] = (topBuf[1] + topBuf[2] + 1 shr 1).toByte()
        genBuf[6] = (topBuf[2] + topBuf[3] + 1 shr 1).toByte()
        genBuf[7] = (topBuf[3] + topBuf[4] + 1 shr 1).toByte()
        genBuf[8] = (topBuf[4] + topBuf[5] + 1 shr 1).toByte()
        genBuf[9] = (topBuf[5] + topBuf[6] + 1 shr 1).toByte()
        genBuf[10] = (topBuf[6] + topBuf[7] + 1 shr 1).toByte()
        genBuf[11] = (leftBuf[6] + leftBuf[4] + (leftBuf[5] shl 1) + 2 shr 2).toByte()
        genBuf[12] = (leftBuf[4] + leftBuf[2] + (leftBuf[3] shl 1) + 2 shr 2).toByte()
        genBuf[13] = (leftBuf[2] + leftBuf[0] + (leftBuf[1] shl 1) + 2 shr 2).toByte()
        genBuf[14] = (leftBuf[0] + topBuf[0] + (tl shl 1) + 2 shr 2).toByte()
        genBuf[15] = (tl + topBuf[1] + (topBuf[0] shl 1) + 2 shr 2).toByte()
        genBuf[16] = (topBuf[0] + topBuf[2] + (topBuf[1] shl 1) + 2 shr 2).toByte()
        genBuf[17] = (topBuf[1] + topBuf[3] + (topBuf[2] shl 1) + 2 shr 2).toByte()
        genBuf[18] = (topBuf[2] + topBuf[4] + (topBuf[3] shl 1) + 2 shr 2).toByte()
        genBuf[19] = (topBuf[3] + topBuf[5] + (topBuf[4] shl 1) + 2 shr 2).toByte()
        genBuf[20] = (topBuf[4] + topBuf[6] + (topBuf[5] shl 1) + 2 shr 2).toByte()
        genBuf[21] = (topBuf[5] + topBuf[7] + (topBuf[6] shl 1) + 2 shr 2).toByte()
        val off = (blkY shl 4) + blkX
        copyAdd(genBuf, 3, residual, off, 0, pixOut)
        copyAdd(genBuf, 14, residual, off + 16, 8, pixOut)
        copyAdd(genBuf, 2, residual, off + 32, 16, pixOut)
        copyAdd(genBuf, 13, residual, off + 48, 24, pixOut)
        copyAdd(genBuf, 1, residual, off + 64, 32, pixOut)
        copyAdd(genBuf, 12, residual, off + 80, 40, pixOut)
        copyAdd(genBuf, 0, residual, off + 96, 48, pixOut)
        copyAdd(genBuf, 11, residual, off + 112, 56, pixOut)
    }

    fun predictHorizontalDown(residual: IntArray, topRightAvailable: Boolean, topLeft: ByteArray, leftRow: ByteArray,
                              topLine: ByteArray, mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        interpolateTop(true, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf)
        interpolateLeft(true, topLeft, leftRow, blkY, leftBuf)
        val tl = interpolateTopLeft(true, true, topLeft, topLine, leftRow, mbOffX, blkX, blkY)
        genBuf[0] = (leftBuf[7] + leftBuf[6] + 1 shr 1).toByte()
        genBuf[1] = (leftBuf[5] + leftBuf[7] + (leftBuf[6] shl 1) + 2 shr 2).toByte()
        genBuf[2] = (leftBuf[6] + leftBuf[5] + 1 shr 1).toByte()
        genBuf[3] = (leftBuf[4] + leftBuf[6] + (leftBuf[5] shl 1) + 2 shr 2).toByte()
        genBuf[4] = (leftBuf[5] + leftBuf[4] + 1 shr 1).toByte()
        genBuf[5] = (leftBuf[3] + leftBuf[5] + (leftBuf[4] shl 1) + 2 shr 2).toByte()
        genBuf[6] = (leftBuf[4] + leftBuf[3] + 1 shr 1).toByte()
        genBuf[7] = (leftBuf[2] + leftBuf[4] + (leftBuf[3] shl 1) + 2 shr 2).toByte()
        genBuf[8] = (leftBuf[3] + leftBuf[2] + 1 shr 1).toByte()
        genBuf[9] = (leftBuf[1] + leftBuf[3] + (leftBuf[2] shl 1) + 2 shr 2).toByte()
        genBuf[10] = (leftBuf[2] + leftBuf[1] + 1 shr 1).toByte()
        genBuf[11] = (leftBuf[0] + leftBuf[2] + (leftBuf[1] shl 1) + 2 shr 2).toByte()
        genBuf[12] = (leftBuf[1] + leftBuf[0] + 1 shr 1).toByte()
        genBuf[13] = (tl + leftBuf[1] + (leftBuf[0] shl 1) + 2 shr 2).toByte()
        genBuf[14] = (leftBuf[0] + tl + 1 shr 1).toByte()
        genBuf[15] = (leftBuf[0] + topBuf[0] + (tl shl 1) + 2 shr 2).toByte()
        genBuf[16] = (tl + topBuf[1] + (topBuf[0] shl 1) + 2 shr 2).toByte()
        genBuf[17] = (topBuf[0] + topBuf[2] + (topBuf[1] shl 1) + 2 shr 2).toByte()
        genBuf[18] = (topBuf[1] + topBuf[3] + (topBuf[2] shl 1) + 2 shr 2).toByte()
        genBuf[19] = (topBuf[2] + topBuf[4] + (topBuf[3] shl 1) + 2 shr 2).toByte()
        genBuf[20] = (topBuf[3] + topBuf[5] + (topBuf[4] shl 1) + 2 shr 2).toByte()
        genBuf[21] = (topBuf[4] + topBuf[6] + (topBuf[5] shl 1) + 2 shr 2).toByte()
        val off = (blkY shl 4) + blkX
        copyAdd(genBuf, 14, residual, off, 0, pixOut)
        copyAdd(genBuf, 12, residual, off + 16, 8, pixOut)
        copyAdd(genBuf, 10, residual, off + 32, 16, pixOut)
        copyAdd(genBuf, 8, residual, off + 48, 24, pixOut)
        copyAdd(genBuf, 6, residual, off + 64, 32, pixOut)
        copyAdd(genBuf, 4, residual, off + 80, 40, pixOut)
        copyAdd(genBuf, 2, residual, off + 96, 48, pixOut)
        copyAdd(genBuf, 0, residual, off + 112, 56, pixOut)
    }

    fun predictVerticalLeft(residual: IntArray, topLeftAvailable: Boolean, topRightAvailable: Boolean,
                            topLeft: ByteArray, topLine: ByteArray, mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        interpolateTop(topLeftAvailable, topRightAvailable, topLeft, topLine, mbOffX + blkX, blkY, topBuf)
        genBuf[0] = (topBuf[0] + topBuf[1] + 1 shr 1).toByte()
        genBuf[1] = (topBuf[1] + topBuf[2] + 1 shr 1).toByte()
        genBuf[2] = (topBuf[2] + topBuf[3] + 1 shr 1).toByte()
        genBuf[3] = (topBuf[3] + topBuf[4] + 1 shr 1).toByte()
        genBuf[4] = (topBuf[4] + topBuf[5] + 1 shr 1).toByte()
        genBuf[5] = (topBuf[5] + topBuf[6] + 1 shr 1).toByte()
        genBuf[6] = (topBuf[6] + topBuf[7] + 1 shr 1).toByte()
        genBuf[7] = (topBuf[7] + topBuf[8] + 1 shr 1).toByte()
        genBuf[8] = (topBuf[8] + topBuf[9] + 1 shr 1).toByte()
        genBuf[9] = (topBuf[9] + topBuf[10] + 1 shr 1).toByte()
        genBuf[10] = (topBuf[10] + topBuf[11] + 1 shr 1).toByte()
        genBuf[11] = (topBuf[0] + topBuf[2] + (topBuf[1] shl 1) + 2 shr 2).toByte()
        genBuf[12] = (topBuf[1] + topBuf[3] + (topBuf[2] shl 1) + 2 shr 2).toByte()
        genBuf[13] = (topBuf[2] + topBuf[4] + (topBuf[3] shl 1) + 2 shr 2).toByte()
        genBuf[14] = (topBuf[3] + topBuf[5] + (topBuf[4] shl 1) + 2 shr 2).toByte()
        genBuf[15] = (topBuf[4] + topBuf[6] + (topBuf[5] shl 1) + 2 shr 2).toByte()
        genBuf[16] = (topBuf[5] + topBuf[7] + (topBuf[6] shl 1) + 2 shr 2).toByte()
        genBuf[17] = (topBuf[6] + topBuf[8] + (topBuf[7] shl 1) + 2 shr 2).toByte()
        genBuf[18] = (topBuf[7] + topBuf[9] + (topBuf[8] shl 1) + 2 shr 2).toByte()
        genBuf[19] = (topBuf[8] + topBuf[10] + (topBuf[9] shl 1) + 2 shr 2).toByte()
        genBuf[20] = (topBuf[9] + topBuf[11] + (topBuf[10] shl 1) + 2 shr 2).toByte()
        genBuf[21] = (topBuf[10] + topBuf[12] + (topBuf[11] shl 1) + 2 shr 2).toByte()
        val off = (blkY shl 4) + blkX
        copyAdd(genBuf, 0, residual, off, 0, pixOut)
        copyAdd(genBuf, 11, residual, off + 16, 8, pixOut)
        copyAdd(genBuf, 1, residual, off + 32, 16, pixOut)
        copyAdd(genBuf, 12, residual, off + 48, 24, pixOut)
        copyAdd(genBuf, 2, residual, off + 64, 32, pixOut)
        copyAdd(genBuf, 13, residual, off + 80, 40, pixOut)
        copyAdd(genBuf, 3, residual, off + 96, 48, pixOut)
        copyAdd(genBuf, 14, residual, off + 112, 56, pixOut)
    }

    fun predictHorizontalUp(residual: IntArray, topLeftAvailable: Boolean, topLeft: ByteArray, leftRow: ByteArray,
                            mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        interpolateLeft(topLeftAvailable, topLeft, leftRow, blkY, leftBuf)
        genBuf[0] = (leftBuf[0] + leftBuf[1] + 1 shr 1).toByte()
        genBuf[1] = (leftBuf[2] + leftBuf[0] + (leftBuf[1] shl 1) + 2 shr 2).toByte()
        genBuf[2] = (leftBuf[1] + leftBuf[2] + 1 shr 1).toByte()
        genBuf[3] = (leftBuf[3] + leftBuf[1] + (leftBuf[2] shl 1) + 2 shr 2).toByte()
        genBuf[4] = (leftBuf[2] + leftBuf[3] + 1 shr 1).toByte()
        genBuf[5] = (leftBuf[4] + leftBuf[2] + (leftBuf[3] shl 1) + 2 shr 2).toByte()
        genBuf[6] = (leftBuf[3] + leftBuf[4] + 1 shr 1).toByte()
        genBuf[7] = (leftBuf[5] + leftBuf[3] + (leftBuf[4] shl 1) + 2 shr 2).toByte()
        genBuf[8] = (leftBuf[4] + leftBuf[5] + 1 shr 1).toByte()
        genBuf[9] = (leftBuf[6] + leftBuf[4] + (leftBuf[5] shl 1) + 2 shr 2).toByte()
        genBuf[10] = (leftBuf[5] + leftBuf[6] + 1 shr 1).toByte()
        genBuf[11] = (leftBuf[7] + leftBuf[5] + (leftBuf[6] shl 1) + 2 shr 2).toByte()
        genBuf[12] = (leftBuf[6] + leftBuf[7] + 1 shr 1).toByte()
        genBuf[13] = (leftBuf[6] + leftBuf[7] + (leftBuf[7] shl 1) + 2 shr 2).toByte()
        genBuf[21] = leftBuf[7]
        genBuf[20] = genBuf[21]
        genBuf[19] = genBuf[20]
        genBuf[18] = genBuf[19]
        genBuf[17] = genBuf[18]
        genBuf[16] = genBuf[17]
        genBuf[15] = genBuf[16]
        genBuf[14] = genBuf[15]
        val off = (blkY shl 4) + blkX
        copyAdd(genBuf, 0, residual, off, 0, pixOut)
        copyAdd(genBuf, 2, residual, off + 16, 8, pixOut)
        copyAdd(genBuf, 4, residual, off + 32, 16, pixOut)
        copyAdd(genBuf, 6, residual, off + 48, 24, pixOut)
        copyAdd(genBuf, 8, residual, off + 64, 32, pixOut)
        copyAdd(genBuf, 10, residual, off + 80, 40, pixOut)
        copyAdd(genBuf, 12, residual, off + 96, 48, pixOut)
        copyAdd(genBuf, 14, residual, off + 112, 56, pixOut)
    }

    init {
        topBuf = ByteArray(16)
        leftBuf = ByteArray(8)
        genBuf = ByteArray(24)
    }
}