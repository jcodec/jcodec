package org.jcodec.codecs.h264.decode

import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Builds intra prediction for intra 4x4 coded macroblocks
 *
 * @author The JCodec project
 */
object Intra4x4PredictionBuilder {
    fun predictWithMode(mode: Int, residual: IntArray, leftAvailable: Boolean, topAvailable: Boolean,
                        topRightAvailable: Boolean, leftRow: ByteArray, topLine: ByteArray, topLeft: ByteArray, mbOffX: Int, blkX: Int, blkY: Int,
                        pixOut: ByteArray) {
        when (mode) {
            0 -> predictVertical(residual, topAvailable, topLine, mbOffX, blkX, blkY, pixOut)
            1 -> predictHorizontal(residual, leftAvailable, leftRow, mbOffX, blkX, blkY, pixOut)
            2 -> predictDC(residual, leftAvailable, topAvailable, leftRow, topLine, mbOffX, blkX, blkY, pixOut)
            3 -> predictDiagonalDownLeft(residual, topAvailable, topRightAvailable, topLine, mbOffX, blkX, blkY, pixOut)
            4 -> predictDiagonalDownRight(residual, leftAvailable, topAvailable, leftRow, topLine, topLeft, mbOffX, blkX,
                    blkY, pixOut)
            5 -> predictVerticalRight(residual, leftAvailable, topAvailable, leftRow, topLine, topLeft, mbOffX, blkX, blkY,
                    pixOut)
            6 -> predictHorizontalDown(residual, leftAvailable, topAvailable, leftRow, topLine, topLeft, mbOffX, blkX, blkY,
                    pixOut)
            7 -> predictVerticalLeft(residual, topAvailable, topRightAvailable, topLine, mbOffX, blkX, blkY, pixOut)
            8 -> predictHorizontalUp(residual, leftAvailable, leftRow, mbOffX, blkX, blkY, pixOut)
        }
        val oo1 = mbOffX + blkX
        val off1 = (blkY shl 4) + blkX + 3
        topLeft[blkY shr 2] = topLine[oo1 + 3]
        leftRow[blkY] = pixOut[off1]
        leftRow[blkY + 1] = pixOut[off1 + 16]
        leftRow[blkY + 2] = pixOut[off1 + 32]
        leftRow[blkY + 3] = pixOut[off1 + 48]
        val off2 = (blkY shl 4) + blkX + 48
        topLine[oo1] = pixOut[off2]
        topLine[oo1 + 1] = pixOut[off2 + 1]
        topLine[oo1 + 2] = pixOut[off2 + 2]
        topLine[oo1 + 3] = pixOut[off2 + 3]
    }

    @JvmStatic
    fun predictVertical(residual: IntArray, topAvailable: Boolean, topLine: ByteArray, mbOffX: Int, blkX: Int,
                        blkY: Int, pixOut: ByteArray) {
        var pixOff = (blkY shl 4) + blkX
        val toff = mbOffX + blkX
        var rOff = 0
        for (j in 0..3) {
            pixOut[pixOff] = MathUtil.clip(residual[rOff] + topLine[toff], -128, 127).toByte()
            pixOut[pixOff + 1] = MathUtil.clip(residual[rOff + 1] + topLine[toff + 1], -128, 127).toByte()
            pixOut[pixOff + 2] = MathUtil.clip(residual[rOff + 2] + topLine[toff + 2], -128, 127).toByte()
            pixOut[pixOff + 3] = MathUtil.clip(residual[rOff + 3] + topLine[toff + 3], -128, 127).toByte()
            rOff += 4
            pixOff += 16
        }
    }

    @JvmStatic
    fun predictHorizontal(residual: IntArray, leftAvailable: Boolean, leftRow: ByteArray, mbOffX: Int, blkX: Int,
                          blkY: Int, pixOut: ByteArray) {
        var pixOff = (blkY shl 4) + blkX
        var rOff = 0
        for (j in 0..3) {
            val l = leftRow[blkY + j].toInt()
            pixOut[pixOff] = MathUtil.clip(residual[rOff] + l, -128, 127).toByte()
            pixOut[pixOff + 1] = MathUtil.clip(residual[rOff + 1] + l, -128, 127).toByte()
            pixOut[pixOff + 2] = MathUtil.clip(residual[rOff + 2] + l, -128, 127).toByte()
            pixOut[pixOff + 3] = MathUtil.clip(residual[rOff + 3] + l, -128, 127).toByte()
            rOff += 4
            pixOff += 16
        }
    }

    @JvmStatic
    fun predictDC(residual: IntArray, leftAvailable: Boolean, topAvailable: Boolean, leftRow: ByteArray,
                  topLine: ByteArray, mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        val `val`: Int
        `val` = if (leftAvailable && topAvailable) {
            (leftRow[blkY] + leftRow[blkY + 1] + leftRow[blkY + 2] + leftRow[blkY + 3] + topLine[mbOffX + blkX]
                    + topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2] + topLine[mbOffX + blkX + 3] + 4) shr 3
        } else if (leftAvailable) {
            leftRow[blkY] + leftRow[blkY + 1] + leftRow[blkY + 2] + leftRow[blkY + 3] + 2 shr 2
        } else if (topAvailable) {
            (topLine[mbOffX + blkX] + topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2]
                    + topLine[mbOffX + blkX + 3] + 2) shr 2
        } else {
            0
        }
        var pixOff = (blkY shl 4) + blkX
        var rOff = 0
        for (j in 0..3) {
            pixOut[pixOff] = MathUtil.clip(residual[rOff] + `val`, -128, 127).toByte()
            pixOut[pixOff + 1] = MathUtil.clip(residual[rOff + 1] + `val`, -128, 127).toByte()
            pixOut[pixOff + 2] = MathUtil.clip(residual[rOff + 2] + `val`, -128, 127).toByte()
            pixOut[pixOff + 3] = MathUtil.clip(residual[rOff + 3] + `val`, -128, 127).toByte()
            pixOff += 16
            rOff += 4
        }
    }

    infix fun Byte.shl(x: Int): Int = this.toInt() shl x

    @JvmStatic
    fun predictDiagonalDownLeft(residual: IntArray, topAvailable: Boolean, topRightAvailable: Boolean,
                                topLine: ByteArray, mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        val to = mbOffX + blkX
        var tr0 = topLine[to + 3].toInt()
        var tr1 = topLine[to + 3].toInt()
        var tr2 = topLine[to + 3].toInt()
        var tr3 = topLine[to + 3].toInt()
        if (topRightAvailable) {
            tr0 = topLine[to + 4].toInt()
            tr1 = topLine[to + 5].toInt()
            tr2 = topLine[to + 6].toInt()
            tr3 = topLine[to + 7].toInt()
        }
        val c0: Int = topLine[to] + topLine[to + 2] + (topLine[to + 1] shl 1) + 2 shr 2
        val c1: Int = topLine[to + 1] + topLine[to + 3] + (topLine[to + 2] shl 1) + 2 shr 2
        val c2: Int = topLine[to + 2] + tr0 + (topLine[to + 3] shl 1) + 2 shr 2
        val c3 = topLine[to + 3] + tr1 + (tr0 shl 1) + 2 shr 2
        val c4 = tr0 + tr2 + (tr1 shl 1) + 2 shr 2
        val c5 = tr1 + tr3 + (tr2 shl 1) + 2 shr 2
        val c6 = tr2 + 3 * tr3 + 2 shr 2
        val off = (blkY shl 4) + blkX
        pixOut[off] = MathUtil.clip(residual[0] + c0, -128, 127).toByte()
        pixOut[off + 1] = MathUtil.clip(residual[1] + c1, -128, 127).toByte()
        pixOut[off + 2] = MathUtil.clip(residual[2] + c2, -128, 127).toByte()
        pixOut[off + 3] = MathUtil.clip(residual[3] + c3, -128, 127).toByte()
        pixOut[off + 16] = MathUtil.clip(residual[4] + c1, -128, 127).toByte()
        pixOut[off + 17] = MathUtil.clip(residual[5] + c2, -128, 127).toByte()
        pixOut[off + 18] = MathUtil.clip(residual[6] + c3, -128, 127).toByte()
        pixOut[off + 19] = MathUtil.clip(residual[7] + c4, -128, 127).toByte()
        pixOut[off + 32] = MathUtil.clip(residual[8] + c2, -128, 127).toByte()
        pixOut[off + 33] = MathUtil.clip(residual[9] + c3, -128, 127).toByte()
        pixOut[off + 34] = MathUtil.clip(residual[10] + c4, -128, 127).toByte()
        pixOut[off + 35] = MathUtil.clip(residual[11] + c5, -128, 127).toByte()
        pixOut[off + 48] = MathUtil.clip(residual[12] + c3, -128, 127).toByte()
        pixOut[off + 49] = MathUtil.clip(residual[13] + c4, -128, 127).toByte()
        pixOut[off + 50] = MathUtil.clip(residual[14] + c5, -128, 127).toByte()
        pixOut[off + 51] = MathUtil.clip(residual[15] + c6, -128, 127).toByte()
    }

    @JvmStatic
    fun predictDiagonalDownRight(residual: IntArray, leftAvailable: Boolean, topAvailable: Boolean,
                                 leftRow: ByteArray, topLine: ByteArray, topLeft: ByteArray, mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        val off = (blkY shl 4) + blkX
        val c0 = topLine[mbOffX + blkX] + 2 * topLeft[blkY shr 2] + leftRow[blkY] + 2 shr 2
        val c1: Int = topLeft[blkY shr 2] + (topLine[mbOffX + blkX + 0] shl 1) + topLine[mbOffX + blkX + 1] + 2 shr 2
        val c2: Int = topLine[mbOffX + blkX] + (topLine[mbOffX + blkX + 1] shl 1) + topLine[mbOffX + blkX + 2] + 2 shr 2
        val c3: Int = topLine[mbOffX + blkX + 1] + (topLine[mbOffX + blkX + 2] shl 1) + topLine[mbOffX + blkX + 3] + 2 shr 2
        pixOut[off] = MathUtil.clip(residual[0] + c0, -128, 127).toByte()
        pixOut[off + 1] = MathUtil.clip(residual[1] + c1, -128, 127).toByte()
        pixOut[off + 2] = MathUtil.clip(residual[2] + c2, -128, 127).toByte()
        pixOut[off + 3] = MathUtil.clip(residual[3] + c3, -128, 127).toByte()
        val c4: Int = topLeft[blkY shr 2] + (leftRow[blkY] shl 1) + leftRow[blkY + 1] + 2 shr 2
        val c6: Int = topLeft[blkY shr 2] + (topLine[mbOffX + blkX] shl 1) + topLine[mbOffX + blkX + 1] + 2 shr 2
        val c7: Int = topLine[mbOffX + blkX] + (topLine[mbOffX + blkX + 1] shl 1) + topLine[mbOffX + blkX + 2] + 2 shr 2
        pixOut[off + 16] = MathUtil.clip(residual[4] + c4, -128, 127).toByte()
        pixOut[off + 17] = MathUtil.clip(residual[5] + c0, -128, 127).toByte()
        pixOut[off + 18] = MathUtil.clip(residual[6] + c6, -128, 127).toByte()
        pixOut[off + 19] = MathUtil.clip(residual[7] + c7, -128, 127).toByte()
        val c8: Int = leftRow[blkY + 0] + (leftRow[blkY + 1] shl 1) + leftRow[blkY + 2] + 2 shr 2
        val c9: Int = topLeft[blkY shr 2] + (leftRow[blkY] shl 1) + leftRow[blkY + 1] + 2 shr 2
        val c11: Int = topLeft[blkY shr 2] + (topLine[mbOffX + blkX] shl 1) + topLine[mbOffX + blkX + 1] + 2 shr 2
        pixOut[off + 32] = MathUtil.clip(residual[8] + c8, -128, 127).toByte()
        pixOut[off + 33] = MathUtil.clip(residual[9] + c9, -128, 127).toByte()
        pixOut[off + 34] = MathUtil.clip(residual[10] + c0, -128, 127).toByte()
        pixOut[off + 35] = MathUtil.clip(residual[11] + c11, -128, 127).toByte()
        val c12: Int = leftRow[blkY + 1] + (leftRow[blkY + 2] shl 1) + leftRow[blkY + 3] + 2 shr 2
        val c13: Int = leftRow[blkY] + (leftRow[blkY + 1] shl 1) + leftRow[blkY + 2] + 2 shr 2
        val c14: Int = topLeft[blkY shr 2] + (leftRow[blkY] shl 1) + leftRow[blkY + 1] + 2 shr 2
        pixOut[off + 48] = MathUtil.clip(residual[12] + c12, -128, 127).toByte()
        pixOut[off + 49] = MathUtil.clip(residual[13] + c13, -128, 127).toByte()
        pixOut[off + 50] = MathUtil.clip(residual[14] + c14, -128, 127).toByte()
        pixOut[off + 51] = MathUtil.clip(residual[15] + c0, -128, 127).toByte()
    }

    @JvmStatic
    fun predictVerticalRight(residual: IntArray, leftAvailable: Boolean, topAvailable: Boolean,
                             leftRow: ByteArray, topLine: ByteArray, topLeft: ByteArray, mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        val v1 = topLeft[blkY shr 2] + topLine[mbOffX + blkX + 0] + 1 shr 1
        val v2 = topLine[mbOffX + blkX + 0] + topLine[mbOffX + blkX + 1] + 1 shr 1
        val v3 = topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2] + 1 shr 1
        val v4 = topLine[mbOffX + blkX + 2] + topLine[mbOffX + blkX + 3] + 1 shr 1
        val v5 = leftRow[blkY] + 2 * topLeft[blkY shr 2] + topLine[mbOffX + blkX + 0] + 2 shr 2
        val v6 = topLeft[blkY shr 2] + 2 * topLine[mbOffX + blkX + 0] + topLine[mbOffX + blkX + 1] + 2 shr 2
        val v7 = topLine[mbOffX + blkX + 0] + 2 * topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2] + 2 shr 2
        val v8 = topLine[mbOffX + blkX + 1] + 2 * topLine[mbOffX + blkX + 2] + topLine[mbOffX + blkX + 3] + 2 shr 2
        val v9 = topLeft[blkY shr 2] + 2 * leftRow[blkY] + leftRow[blkY + 1] + 2 shr 2
        val v10 = leftRow[blkY] + 2 * leftRow[blkY + 1] + leftRow[blkY + 2] + 2 shr 2
        val off = (blkY shl 4) + blkX
        pixOut[off] = MathUtil.clip(residual[0] + v1, -128, 127).toByte()
        pixOut[off + 1] = MathUtil.clip(residual[1] + v2, -128, 127).toByte()
        pixOut[off + 2] = MathUtil.clip(residual[2] + v3, -128, 127).toByte()
        pixOut[off + 3] = MathUtil.clip(residual[3] + v4, -128, 127).toByte()
        pixOut[off + 16] = MathUtil.clip(residual[4] + v5, -128, 127).toByte()
        pixOut[off + 17] = MathUtil.clip(residual[5] + v6, -128, 127).toByte()
        pixOut[off + 18] = MathUtil.clip(residual[6] + v7, -128, 127).toByte()
        pixOut[off + 19] = MathUtil.clip(residual[7] + v8, -128, 127).toByte()
        pixOut[off + 32] = MathUtil.clip(residual[8] + v9, -128, 127).toByte()
        pixOut[off + 33] = MathUtil.clip(residual[9] + v1, -128, 127).toByte()
        pixOut[off + 34] = MathUtil.clip(residual[10] + v2, -128, 127).toByte()
        pixOut[off + 35] = MathUtil.clip(residual[11] + v3, -128, 127).toByte()
        pixOut[off + 48] = MathUtil.clip(residual[12] + v10, -128, 127).toByte()
        pixOut[off + 49] = MathUtil.clip(residual[13] + v5, -128, 127).toByte()
        pixOut[off + 50] = MathUtil.clip(residual[14] + v6, -128, 127).toByte()
        pixOut[off + 51] = MathUtil.clip(residual[15] + v7, -128, 127).toByte()
    }

    @JvmStatic
    fun predictHorizontalDown(residual: IntArray, leftAvailable: Boolean, topAvailable: Boolean,
                              leftRow: ByteArray, topLine: ByteArray, topLeft: ByteArray, mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        val c0 = topLeft[blkY shr 2] + leftRow[blkY] + 1 shr 1
        val c1 = leftRow[blkY] + 2 * topLeft[blkY shr 2] + topLine[mbOffX + blkX + 0] + 2 shr 2
        val c2 = topLeft[blkY shr 2] + 2 * topLine[mbOffX + blkX + 0] + topLine[mbOffX + blkX + 1] + 2 shr 2
        val c3 = topLine[mbOffX + blkX + 0] + 2 * topLine[mbOffX + blkX + 1] + topLine[mbOffX + blkX + 2] + 2 shr 2
        val c4 = leftRow[blkY] + leftRow[blkY + 1] + 1 shr 1
        val c5 = topLeft[blkY shr 2] + 2 * leftRow[blkY] + leftRow[blkY + 1] + 2 shr 2
        val c6 = leftRow[blkY + 1] + leftRow[blkY + 2] + 1 shr 1
        val c7 = leftRow[blkY] + 2 * leftRow[blkY + 1] + leftRow[blkY + 2] + 2 shr 2
        val c8 = leftRow[blkY + 2] + leftRow[blkY + 3] + 1 shr 1
        val c9 = leftRow[blkY + 1] + 2 * leftRow[blkY + 2] + leftRow[blkY + 3] + 2 shr 2
        val off = (blkY shl 4) + blkX
        pixOut[off] = MathUtil.clip(residual[0] + c0, -128, 127).toByte()
        pixOut[off + 1] = MathUtil.clip(residual[1] + c1, -128, 127).toByte()
        pixOut[off + 2] = MathUtil.clip(residual[2] + c2, -128, 127).toByte()
        pixOut[off + 3] = MathUtil.clip(residual[3] + c3, -128, 127).toByte()
        pixOut[off + 16] = MathUtil.clip(residual[4] + c4, -128, 127).toByte()
        pixOut[off + 17] = MathUtil.clip(residual[5] + c5, -128, 127).toByte()
        pixOut[off + 18] = MathUtil.clip(residual[6] + c0, -128, 127).toByte()
        pixOut[off + 19] = MathUtil.clip(residual[7] + c1, -128, 127).toByte()
        pixOut[off + 32] = MathUtil.clip(residual[8] + c6, -128, 127).toByte()
        pixOut[off + 33] = MathUtil.clip(residual[9] + c7, -128, 127).toByte()
        pixOut[off + 34] = MathUtil.clip(residual[10] + c4, -128, 127).toByte()
        pixOut[off + 35] = MathUtil.clip(residual[11] + c5, -128, 127).toByte()
        pixOut[off + 48] = MathUtil.clip(residual[12] + c8, -128, 127).toByte()
        pixOut[off + 49] = MathUtil.clip(residual[13] + c9, -128, 127).toByte()
        pixOut[off + 50] = MathUtil.clip(residual[14] + c6, -128, 127).toByte()
        pixOut[off + 51] = MathUtil.clip(residual[15] + c7, -128, 127).toByte()
    }

    @JvmStatic
    fun predictVerticalLeft(residual: IntArray, topAvailable: Boolean, topRightAvailable: Boolean,
                            topLine: ByteArray, mbOffX: Int, blkX: Int, blkY: Int, pixOut: ByteArray) {
        val to = mbOffX + blkX
        var tr0 = topLine[to + 3].toInt()
        var tr1 = topLine[to + 3].toInt()
        var tr2 = topLine[to + 3].toInt()
        if (topRightAvailable) {
            tr0 = topLine[to + 4].toInt()
            tr1 = topLine[to + 5].toInt()
            tr2 = topLine[to + 6].toInt()
        }
        val c0 = topLine[to] + topLine[to + 1] + 1 shr 1
        val c1 = topLine[to + 1] + topLine[to + 2] + 1 shr 1
        val c2 = topLine[to + 2] + topLine[to + 3] + 1 shr 1
        val c3 = topLine[to + 3] + tr0 + 1 shr 1
        val c4 = tr0 + tr1 + 1 shr 1
        val c5 = topLine[to] + 2 * topLine[to + 1] + topLine[to + 2] + 2 shr 2
        val c6 = topLine[to + 1] + 2 * topLine[to + 2] + topLine[to + 3] + 2 shr 2
        val c7 = topLine[to + 2] + 2 * topLine[to + 3] + tr0 + 2 shr 2
        val c8 = topLine[to + 3] + 2 * tr0 + tr1 + 2 shr 2
        val c9 = tr0 + 2 * tr1 + tr2 + 2 shr 2
        val off = (blkY shl 4) + blkX
        pixOut[off] = MathUtil.clip(residual[0] + c0, -128, 127).toByte()
        pixOut[off + 1] = MathUtil.clip(residual[1] + c1, -128, 127).toByte()
        pixOut[off + 2] = MathUtil.clip(residual[2] + c2, -128, 127).toByte()
        pixOut[off + 3] = MathUtil.clip(residual[3] + c3, -128, 127).toByte()
        pixOut[off + 16] = MathUtil.clip(residual[4] + c5, -128, 127).toByte()
        pixOut[off + 17] = MathUtil.clip(residual[5] + c6, -128, 127).toByte()
        pixOut[off + 18] = MathUtil.clip(residual[6] + c7, -128, 127).toByte()
        pixOut[off + 19] = MathUtil.clip(residual[7] + c8, -128, 127).toByte()
        pixOut[off + 32] = MathUtil.clip(residual[8] + c1, -128, 127).toByte()
        pixOut[off + 33] = MathUtil.clip(residual[9] + c2, -128, 127).toByte()
        pixOut[off + 34] = MathUtil.clip(residual[10] + c3, -128, 127).toByte()
        pixOut[off + 35] = MathUtil.clip(residual[11] + c4, -128, 127).toByte()
        pixOut[off + 48] = MathUtil.clip(residual[12] + c6, -128, 127).toByte()
        pixOut[off + 49] = MathUtil.clip(residual[13] + c7, -128, 127).toByte()
        pixOut[off + 50] = MathUtil.clip(residual[14] + c8, -128, 127).toByte()
        pixOut[off + 51] = MathUtil.clip(residual[15] + c9, -128, 127).toByte()
    }

    @JvmStatic
    fun predictHorizontalUp(residual: IntArray, leftAvailable: Boolean, leftRow: ByteArray, mbOffX: Int, blkX: Int,
                            blkY: Int, pixOut: ByteArray) {
        val c0 = leftRow[blkY] + leftRow[blkY + 1] + 1 shr 1
        val c1: Int = leftRow[blkY] + (leftRow[blkY + 1] shl 1) + leftRow[blkY + 2] + 2 shr 2
        val c2 = leftRow[blkY + 1] + leftRow[blkY + 2] + 1 shr 1
        val c3: Int = leftRow[blkY + 1] + (leftRow[blkY + 2] shl 1) + leftRow[blkY + 3] + 2 shr 2
        val c4 = leftRow[blkY + 2] + leftRow[blkY + 3] + 1 shr 1
        val c5: Int = leftRow[blkY + 2] + (leftRow[blkY + 3] shl 1) + leftRow[blkY + 3] + 2 shr 2
        val c6 = leftRow[blkY + 3].toInt()
        val off = (blkY shl 4) + blkX
        pixOut[off] = MathUtil.clip(residual[0] + c0, -128, 127).toByte()
        pixOut[off + 1] = MathUtil.clip(residual[1] + c1, -128, 127).toByte()
        pixOut[off + 2] = MathUtil.clip(residual[2] + c2, -128, 127).toByte()
        pixOut[off + 3] = MathUtil.clip(residual[3] + c3, -128, 127).toByte()
        pixOut[off + 16] = MathUtil.clip(residual[4] + c2, -128, 127).toByte()
        pixOut[off + 17] = MathUtil.clip(residual[5] + c3, -128, 127).toByte()
        pixOut[off + 18] = MathUtil.clip(residual[6] + c4, -128, 127).toByte()
        pixOut[off + 19] = MathUtil.clip(residual[7] + c5, -128, 127).toByte()
        pixOut[off + 32] = MathUtil.clip(residual[8] + c4, -128, 127).toByte()
        pixOut[off + 33] = MathUtil.clip(residual[9] + c5, -128, 127).toByte()
        pixOut[off + 34] = MathUtil.clip(residual[10] + c6, -128, 127).toByte()
        pixOut[off + 35] = MathUtil.clip(residual[11] + c6, -128, 127).toByte()
        pixOut[off + 48] = MathUtil.clip(residual[12] + c6, -128, 127).toByte()
        pixOut[off + 49] = MathUtil.clip(residual[13] + c6, -128, 127).toByte()
        pixOut[off + 50] = MathUtil.clip(residual[14] + c6, -128, 127).toByte()
        pixOut[off + 51] = MathUtil.clip(residual[15] + c6, -128, 127).toByte()
    }
}