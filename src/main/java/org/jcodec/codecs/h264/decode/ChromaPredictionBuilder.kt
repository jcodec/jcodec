package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Const
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Prediction builder for chroma samples
 *
 * @author The JCodec project
 */
object ChromaPredictionBuilder {
    fun predictWithMode(residual: Array<IntArray>, chromaMode: Int, mbX: Int, leftAvailable: Boolean,
                        topAvailable: Boolean, leftRow: ByteArray, topLine: ByteArray, topLeft: ByteArray, pixOut: ByteArray) {
        when (chromaMode) {
            0 -> predictDC(residual, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut)
            1 -> predictHorizontal(residual, mbX, leftAvailable, leftRow, pixOut)
            2 -> predictVertical(residual, mbX, topAvailable, topLine, pixOut)
            3 -> predictPlane(residual, mbX, leftAvailable, topAvailable, leftRow, topLine, topLeft, pixOut)
        }
    }

    @JvmStatic
    fun predictDC(planeData: Array<IntArray>, mbX: Int, leftAvailable: Boolean, topAvailable: Boolean,
                  leftRow: ByteArray, topLine: ByteArray, pixOut: ByteArray) {
        predictDCInside(planeData, 0, 0, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut)
        predictDCTopBorder(planeData, 1, 0, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut)
        predictDCLeftBorder(planeData, 0, 1, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut)
        predictDCInside(planeData, 1, 1, mbX, leftAvailable, topAvailable, leftRow, topLine, pixOut)
    }

    @JvmStatic
    fun predictVertical(residual: Array<IntArray>, mbX: Int, topAvailable: Boolean, topLine: ByteArray, pixOut: ByteArray) {
        var off = 0
        var j = 0
        while (j < 8) {
            var i = 0
            while (i < 8) {
                pixOut[off] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off]][H264Const.CHROMA_POS_LUT[off]]
                        + topLine[(mbX shl 3) + i], -128, 127).toByte()
                i++
                off++
            }
            j++
        }
    }

    @JvmStatic
    fun predictHorizontal(residual: Array<IntArray>, mbX: Int, leftAvailable: Boolean, leftRow: ByteArray, pixOut: ByteArray) {
        var off = 0
        var j = 0
        while (j < 8) {
            var i = 0
            while (i < 8) {
                pixOut[off] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off]][H264Const.CHROMA_POS_LUT[off]] + leftRow[j], -128, 127).toByte()
                i++
                off++
            }
            j++
        }
    }

    fun predictDCInside(residual: Array<IntArray>, blkX: Int, blkY: Int, mbX: Int, leftAvailable: Boolean,
                        topAvailable: Boolean, leftRow: ByteArray, topLine: ByteArray, pixOut: ByteArray) {
        var s0: Int
        val blkOffX = (blkX shl 2) + (mbX shl 3)
        val blkOffY = blkY shl 2
        if (leftAvailable && topAvailable) {
            s0 = 0
            for (i in 0..3) s0 += leftRow[i + blkOffY]
            for (i in 0..3) s0 += topLine[blkOffX + i]
            s0 = s0 + 4 shr 3
        } else if (leftAvailable) {
            s0 = 0
            for (i in 0..3) s0 += leftRow[blkOffY + i]
            s0 = s0 + 2 shr 2
        } else if (topAvailable) {
            s0 = 0
            for (i in 0..3) s0 += topLine[blkOffX + i]
            s0 = s0 + 2 shr 2
        } else {
            s0 = 0
        }
        var off = (blkY shl 5) + (blkX shl 2)
        var j = 0
        while (j < 4) {
            pixOut[off] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off]][H264Const.CHROMA_POS_LUT[off]] + s0, -128, 127).toByte()
            pixOut[off + 1] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off + 1]][H264Const.CHROMA_POS_LUT[off + 1]] + s0, -128, 127).toByte()
            pixOut[off + 2] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off + 2]][H264Const.CHROMA_POS_LUT[off + 2]] + s0, -128, 127).toByte()
            pixOut[off + 3] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off + 3]][H264Const.CHROMA_POS_LUT[off + 3]] + s0, -128, 127).toByte()
            j++
            off += 8
        }
    }

    fun predictDCTopBorder(residual: Array<IntArray>, blkX: Int, blkY: Int, mbX: Int, leftAvailable: Boolean,
                           topAvailable: Boolean, leftRow: ByteArray, topLine: ByteArray, pixOut: ByteArray) {
        var s1: Int
        val blkOffX = (blkX shl 2) + (mbX shl 3)
        val blkOffY = blkY shl 2
        if (topAvailable) {
            s1 = 0
            for (i in 0..3) s1 += topLine[blkOffX + i]
            s1 = s1 + 2 shr 2
        } else if (leftAvailable) {
            s1 = 0
            for (i in 0..3) s1 += leftRow[blkOffY + i]
            s1 = s1 + 2 shr 2
        } else {
            s1 = 0
        }
        var off = (blkY shl 5) + (blkX shl 2)
        var j = 0
        while (j < 4) {
            pixOut[off] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off]][H264Const.CHROMA_POS_LUT[off]] + s1, -128, 127).toByte()
            pixOut[off + 1] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off + 1]][H264Const.CHROMA_POS_LUT[off + 1]] + s1, -128, 127).toByte()
            pixOut[off + 2] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off + 2]][H264Const.CHROMA_POS_LUT[off + 2]] + s1, -128, 127).toByte()
            pixOut[off + 3] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off + 3]][H264Const.CHROMA_POS_LUT[off + 3]] + s1, -128, 127).toByte()
            j++
            off += 8
        }
    }

    fun predictDCLeftBorder(residual: Array<IntArray>, blkX: Int, blkY: Int, mbX: Int, leftAvailable: Boolean,
                            topAvailable: Boolean, leftRow: ByteArray, topLine: ByteArray, pixOut: ByteArray) {
        var s2: Int
        val blkOffX = (blkX shl 2) + (mbX shl 3)
        val blkOffY = blkY shl 2
        if (leftAvailable) {
            s2 = 0
            for (i in 0..3) s2 += leftRow[blkOffY + i]
            s2 = s2 + 2 shr 2
        } else if (topAvailable) {
            s2 = 0
            for (i in 0..3) s2 += topLine[blkOffX + i]
            s2 = s2 + 2 shr 2
        } else {
            s2 = 0
        }
        var off = (blkY shl 5) + (blkX shl 2)
        var j = 0
        while (j < 4) {
            pixOut[off] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off]][H264Const.CHROMA_POS_LUT[off]] + s2, -128, 127).toByte()
            pixOut[off + 1] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off + 1]][H264Const.CHROMA_POS_LUT[off + 1]] + s2, -128, 127).toByte()
            pixOut[off + 2] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off + 2]][H264Const.CHROMA_POS_LUT[off + 2]] + s2, -128, 127).toByte()
            pixOut[off + 3] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off + 3]][H264Const.CHROMA_POS_LUT[off + 3]] + s2, -128, 127).toByte()
            j++
            off += 8
        }
    }

    @JvmStatic
    fun predictPlane(residual: Array<IntArray>, mbX: Int, leftAvailable: Boolean, topAvailable: Boolean,
                     leftRow: ByteArray, topLine: ByteArray, topLeft: ByteArray, pixOut: ByteArray) {
        var H = 0
        val blkOffX = mbX shl 3
        for (i in 0..2) {
            H += (i + 1) * (topLine[blkOffX + 4 + i] - topLine[blkOffX + 2 - i])
        }
        H += 4 * (topLine[blkOffX + 7] - topLeft[0])
        var V = 0
        for (j in 0..2) {
            V += (j + 1) * (leftRow[4 + j] - leftRow[2 - j])
        }
        V += 4 * (leftRow[7] - topLeft[0])
        val c = 34 * V + 32 shr 6
        val b = 34 * H + 32 shr 6
        val a = 16 * (leftRow[7] + topLine[blkOffX + 7])
        var off = 0
        var j = 0
        while (j < 8) {
            var i = 0
            while (i < 8) {
                val `val` = a + b * (i - 3) + c * (j - 3) + 16 shr 5
                pixOut[off] = MathUtil.clip(residual[H264Const.CHROMA_BLOCK_LUT[off]][H264Const.CHROMA_POS_LUT[off]] + MathUtil.clip(`val`, -128, 127), -128, 127).toByte()
                i++
                off++
            }
            j++
        }
    }
}