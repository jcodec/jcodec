package org.jcodec.codecs.h264.decode.deblock

import org.jcodec.codecs.h264.H264Utils.Mv
import org.jcodec.codecs.h264.decode.DeblockerInput
import org.jcodec.common.model.Picture
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A filter that removes DCT artifacts on block boundaries.
 *
 * It's operation is dependant on QP and is designed the way that the strenth is
 * adjusted to the likelyhood of appearence of blocking artifacts on the
 * specific edges.
 *
 * Builds a parameter for deblocking filter based on the properties of specific
 * macroblocks.
 *
 * A parameter specifies the behavior of deblocking filter on each of 8 edges
 * that need to filtered for a macroblock.
 *
 * For each edge the following things are evaluated on it's both sides: presence
 * of DCT coded residual; motion vector difference; spatial location.
 *
 *
 * @author The JCodec project
 */
class DeblockingFilter(bitDepthLuma: Int, bitDepthChroma: Int, private val di: DeblockerInput) {
    fun deblockFrame(result: Picture) {
        val color = result.color
        val bsV = Array(4) { IntArray(4) }
        val bsH = Array(4) { IntArray(4) }
        for (i in 0 until di.shs.size) {
            calcBsH(result, i, bsH)
            calcBsV(result, i, bsV)
            for (c in 0 until color.nComp) {
                fillVerticalEdge(result, c, i, bsV)
                fillHorizontalEdge(result, c, i, bsH)
            }
        }
    }

    private fun calcBoundaryStrenth(atMbBoundary: Boolean, leftIntra: Boolean, rightIntra: Boolean, leftCoeff: Int,
                                    rightCoeff: Int, mvA0: Int, mvB0: Int, mvA1: Int, mvB1: Int, mbAddrA: Int, mbAddrB: Int): Int {
        if (atMbBoundary && (leftIntra || rightIntra)) return 4 else if (leftIntra || rightIntra) return 3 else {
            if (leftCoeff > 0 || rightCoeff > 0) return 2
            val nA = (if (Mv.mvRef(mvA0) == -1) 0 else 1) + if (Mv.mvRef(mvA1) == -1) 0 else 1
            val nB = (if (Mv.mvRef(mvB0) == -1) 0 else 1) + if (Mv.mvRef(mvB1) == -1) 0 else 1
            if (nA != nB) return 1
            val ra0: Picture? = if (Mv.mvRef(mvA0) < 0) null else di.refsUsed[mbAddrA]!![0]!![Mv.mvRef(mvA0)]
            val ra1: Picture? = if (Mv.mvRef(mvA1) < 0) null else di.refsUsed[mbAddrA]!![1]!![Mv.mvRef(mvA1)]
            val rb0: Picture? = if (Mv.mvRef(mvB0) < 0) null else di.refsUsed[mbAddrB]!![0]!![Mv.mvRef(mvB0)]
            val rb1: Picture? = if (Mv.mvRef(mvB1) < 0) null else di.refsUsed[mbAddrB]!![1]!![Mv.mvRef(mvB1)]
            if (ra0 !== rb0 && ra0 !== rb1 || ra1 !== rb0 && ra1 !== rb1 || rb0 !== ra0 && rb0 !== ra1 || (rb1 !== ra0
                            && rb1 !== ra1)) return 1
            if (ra0 === ra1 && ra1 === rb0 && rb0 === rb1) {
                return if (ra0 != null
                        && (mvThresh(mvA0, mvB0) || mvThresh(mvA1, mvB0) || mvThresh(mvA0, mvB1) || mvThresh(mvA1, mvB1))) 1 else 0
            } else if (ra0 === rb0 && ra1 === rb1) {
                return if (ra0 != null && mvThresh(mvA0, mvB0) || ra1 != null && mvThresh(mvA1, mvB1)) 1 else 0
            } else if (ra0 === rb1 && ra1 === rb0) {
                return if (ra0 != null && mvThresh(mvA0, mvB1) || ra1 != null && mvThresh(mvA1, mvB0)) 1 else 0
            }
        }
        return 0
    }

    private fun mvThresh(v0: Int, v1: Int): Boolean {
        return Math.abs(Mv.mvX(v0) - Mv.mvX(v1)) >= 4 || Math.abs(Mv.mvY(v0) - Mv.mvY(v1)) >= 4
    }

    private fun calcBsH(pic: Picture, mbAddr: Int, bs: Array<IntArray>) {
        val sh = di.shs[mbAddr]!!
        val mbWidth = sh.sps!!.picWidthInMbsMinus1 + 1
        val mbX = mbAddr % mbWidth
        val mbY = mbAddr / mbWidth
        val topAvailable = mbY > 0 && (sh.disableDeblockingFilterIdc != 2 || di.shs[mbAddr - mbWidth] == sh)
        val thisIntra = di.mbTypes[mbAddr] != null && di.mbTypes[mbAddr]!!.isIntra
        if (topAvailable) {
            val topIntra = di.mbTypes[mbAddr - mbWidth] != null && di.mbTypes[mbAddr - mbWidth]!!.isIntra
            for (blkX in 0..3) {
                val thisBlkX = (mbX shl 2) + blkX
                val thisBlkY = mbY shl 2
                bs[0][blkX] = calcBoundaryStrenth(true, topIntra, thisIntra, di.nCoeff[thisBlkY][thisBlkX],
                        di.nCoeff[thisBlkY - 1][thisBlkX], di.mvs.getMv(thisBlkX, thisBlkY, 0),
                        di.mvs.getMv(thisBlkX, thisBlkY - 1, 0), di.mvs.getMv(thisBlkX, thisBlkY, 1),
                        di.mvs.getMv(thisBlkX, thisBlkY - 1, 1), mbAddr, mbAddr - mbWidth)
            }
        }
        for (blkY in 1..3) {
            for (blkX in 0..3) {
                val thisBlkX = (mbX shl 2) + blkX
                val thisBlkY = (mbY shl 2) + blkY
                bs[blkY][blkX] = calcBoundaryStrenth(false, thisIntra, thisIntra, di.nCoeff[thisBlkY][thisBlkX],
                        di.nCoeff[thisBlkY - 1][thisBlkX], di.mvs.getMv(thisBlkX, thisBlkY, 0),
                        di.mvs.getMv(thisBlkX, thisBlkY - 1, 0), di.mvs.getMv(thisBlkX, thisBlkY, 1),
                        di.mvs.getMv(thisBlkX, thisBlkY - 1, 1), mbAddr, mbAddr)
            }
        }
    }

    private fun fillHorizontalEdge(pic: Picture, comp: Int, mbAddr: Int, bs: Array<IntArray>) {
        val sh = di.shs[mbAddr]!!
        val mbWidth = sh.sps!!.picWidthInMbsMinus1 + 1
        val alpha = sh.sliceAlphaC0OffsetDiv2 shl 1
        val beta = sh.sliceBetaOffsetDiv2 shl 1
        val mbX = mbAddr % mbWidth
        val mbY = mbAddr / mbWidth
        val topAvailable = mbY > 0 && (sh.disableDeblockingFilterIdc != 2 || di.shs[mbAddr - mbWidth] == sh)
        val curQp = di.mbQps[comp][mbAddr]
        val cW = 2 - pic.color.compWidth[comp]
        val cH = 2 - pic.color.compHeight[comp]
        if (topAvailable) {
            val topQp = di.mbQps[comp][mbAddr - mbWidth]
            val avgQp = topQp + curQp + 1 shr 1
            for (blkX in 0..3) {
                val thisBlkX = (mbX shl 2) + blkX
                val thisBlkY = mbY shl 2
                filterBlockEdgeHoris(pic, comp, thisBlkX shl cW, thisBlkY shl cH, getIdxAlpha(alpha, avgQp),
                        getIdxBeta(beta, avgQp), bs[0][blkX], 1 shl cW)
            }
        }
        val skip4x4 = comp == 0 && di.tr8x8Used[mbAddr] || cH == 1
        for (blkY in 1..3) {
            if (skip4x4 && blkY and 1 == 1) continue
            for (blkX in 0..3) {
                val thisBlkX = (mbX shl 2) + blkX
                val thisBlkY = (mbY shl 2) + blkY
                filterBlockEdgeHoris(pic, comp, thisBlkX shl cW, thisBlkY shl cH, getIdxAlpha(alpha, curQp),
                        getIdxBeta(beta, curQp), bs[blkY][blkX], 1 shl cW)
            }
        }
    }

    private fun calcBsV(pic: Picture, mbAddr: Int, bs: Array<IntArray>) {
        val sh = di.shs[mbAddr]!!
        val mbWidth = sh.sps!!.picWidthInMbsMinus1 + 1
        val mbX = mbAddr % mbWidth
        val mbY = mbAddr / mbWidth
        val leftAvailable = mbX > 0 && (sh.disableDeblockingFilterIdc != 2 || di.shs[mbAddr - 1] == sh)
        val thisIntra = di.mbTypes[mbAddr] != null && di.mbTypes[mbAddr]!!.isIntra
        if (leftAvailable) {
            val leftIntra = di.mbTypes[mbAddr - 1] != null && di.mbTypes[mbAddr - 1]!!.isIntra
            for (blkY in 0..3) {
                val thisBlkX = mbX shl 2
                val thisBlkY = (mbY shl 2) + blkY
                bs[blkY][0] = calcBoundaryStrenth(true, leftIntra, thisIntra, di.nCoeff[thisBlkY][thisBlkX],
                        di.nCoeff[thisBlkY][thisBlkX - 1], di.mvs.getMv(thisBlkX, thisBlkY, 0),
                        di.mvs.getMv(thisBlkX - 1, thisBlkY, 0), di.mvs.getMv(thisBlkX, thisBlkY, 1),
                        di.mvs.getMv(thisBlkX - 1, thisBlkY, 1), mbAddr, mbAddr - 1)
            }
        }
        for (blkX in 1..3) {
            for (blkY in 0 until (1 shl 2)) {
                val thisBlkX = (mbX shl 2) + blkX
                val thisBlkY = (mbY shl 2) + blkY
                bs[blkY][blkX] = calcBoundaryStrenth(false, thisIntra, thisIntra, di.nCoeff[thisBlkY][thisBlkX],
                        di.nCoeff[thisBlkY][thisBlkX - 1], di.mvs.getMv(thisBlkX, thisBlkY, 0),
                        di.mvs.getMv(thisBlkX - 1, thisBlkY, 0), di.mvs.getMv(thisBlkX, thisBlkY, 1),
                        di.mvs.getMv(thisBlkX - 1, thisBlkY, 1), mbAddr, mbAddr)
            }
        }
    }

    private fun fillVerticalEdge(pic: Picture, comp: Int, mbAddr: Int, bs: Array<IntArray>) {
        val sh = di.shs[mbAddr]!!
        val mbWidth = sh.sps!!.picWidthInMbsMinus1 + 1
        val alpha = sh.sliceAlphaC0OffsetDiv2 shl 1
        val beta = sh.sliceBetaOffsetDiv2 shl 1
        val mbX = mbAddr % mbWidth
        val mbY = mbAddr / mbWidth
        val leftAvailable = mbX > 0 && (sh.disableDeblockingFilterIdc != 2 || di.shs[mbAddr - 1] == sh)
        val curQp = di.mbQps[comp][mbAddr]
        val cW = 2 - pic.color.compWidth[comp]
        val cH = 2 - pic.color.compHeight[comp]
        if (leftAvailable) {
            val leftQp = di.mbQps[comp][mbAddr - 1]
            val avgQpV = leftQp + curQp + 1 shr 1
            for (blkY in 0..3) {
                val thisBlkX = mbX shl 2
                val thisBlkY = (mbY shl 2) + blkY
                filterBlockEdgeVert(pic, comp, thisBlkX shl cW, thisBlkY shl cH, getIdxAlpha(alpha, avgQpV),
                        getIdxBeta(beta, avgQpV), bs[blkY][0], 1 shl cH)
            }
        }
        val skip4x4 = comp == 0 && di.tr8x8Used[mbAddr] || cW == 1
        for (blkX in 1..3) {
            if (skip4x4 && blkX and 1 == 1) continue
            for (blkY in 0..3) {
                val thisBlkX = (mbX shl 2) + blkX
                val thisBlkY = (mbY shl 2) + blkY
                filterBlockEdgeVert(pic, comp, thisBlkX shl cW, thisBlkY shl cH, getIdxAlpha(alpha, curQp),
                        getIdxBeta(beta, curQp), bs[blkY][blkX], 1 shl cH)
            }
        }
    }

    private fun filterBlockEdgeHoris(pic: Picture, comp: Int, x: Int, y: Int, indexAlpha: Int, indexBeta: Int, bs: Int,
                                     blkW: Int) {
        val stride = pic.getPlaneWidth(comp)
        val offset = y * stride + x
        for (pixOff in 0 until blkW) {
            val p2Idx = offset - 3 * stride + pixOff
            val p1Idx = offset - 2 * stride + pixOff
            val p0Idx = offset - stride + pixOff
            val q0Idx = offset + pixOff
            val q1Idx = offset + stride + pixOff
            val q2Idx = offset + 2 * stride + pixOff
            if (bs == 4) {
                val p3Idx = offset - 4 * stride + pixOff
                val q3Idx = offset + 3 * stride + pixOff
                filterBs4(indexAlpha, indexBeta, pic.getPlaneData(comp), pic.getPlaneData(comp), p3Idx, p2Idx, p1Idx,
                        p0Idx, q0Idx, q1Idx, q2Idx, q3Idx, comp != 0)
            } else if (bs > 0) {
                filterBs(bs, indexAlpha, indexBeta, pic.getPlaneData(comp), pic.getPlaneData(comp), p2Idx, p1Idx,
                        p0Idx, q0Idx, q1Idx, q2Idx, comp != 0)
            }
        }
    }

    private fun filterBlockEdgeVert(pic: Picture, comp: Int, x: Int, y: Int, indexAlpha: Int, indexBeta: Int, bs: Int,
                                    blkH: Int) {
        val stride = pic.getPlaneWidth(comp)
        for (i in 0 until blkH) {
            val offsetQ = (y + i) * stride + x
            val p2Idx = offsetQ - 3
            val p1Idx = offsetQ - 2
            val p0Idx = offsetQ - 1
            val q1Idx = offsetQ + 1
            val q2Idx = offsetQ + 2
            if (bs == 4) {
                val p3Idx = offsetQ - 4
                val q3Idx = offsetQ + 3
                filterBs4(indexAlpha, indexBeta, pic.getPlaneData(comp), pic.getPlaneData(comp), p3Idx, p2Idx, p1Idx,
                        p0Idx, offsetQ, q1Idx, q2Idx, q3Idx, comp != 0)
            } else if (bs > 0) {
                filterBs(bs, indexAlpha, indexBeta, pic.getPlaneData(comp), pic.getPlaneData(comp), p2Idx, p1Idx,
                        p0Idx, offsetQ, q1Idx, q2Idx, comp != 0)
            }
        }
    }

    companion object {
        @JvmField
        var alphaTab = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 5, 6, 7, 8, 9, 10, 12,
                13, 15, 17, 20, 22, 25, 28, 32, 36, 40, 45, 50, 56, 63, 71, 80, 90, 101, 113, 127, 144, 162, 182, 203, 226,
                255, 255)
        @JvmField
        var betaTab = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 6,
                6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18)
        @JvmField
        var tcs = arrayOf(intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 6, 6, 7, 8, 9, 10, 11, 13), intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
                2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 6, 7, 8, 8, 10, 11, 12, 13, 15, 17), intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3,
                3, 3, 4, 4, 4, 5, 6, 6, 7, 8, 9, 10, 11, 13, 14, 16, 18, 20, 23, 25))
        var inverse = intArrayOf(0, 1, 4, 5, 2, 3, 6, 7, 8, 9, 12, 13, 10, 11, 14, 15)
        private fun getIdxBeta(sliceBetaOffset: Int, avgQp: Int): Int {
            return MathUtil.clip(avgQp + sliceBetaOffset, 0, 51)
        }

        private fun getIdxAlpha(sliceAlphaC0Offset: Int, avgQp: Int): Int {
            return MathUtil.clip(avgQp + sliceAlphaC0Offset, 0, 51)
        }

        fun filterBs(bs: Int, indexAlpha: Int, indexBeta: Int, pelsP: ByteArray, pelsQ: ByteArray, p2Idx: Int, p1Idx: Int,
                     p0Idx: Int, q0Idx: Int, q1Idx: Int, q2Idx: Int, isChroma: Boolean) {
            val p1 = pelsP[p1Idx].toInt()
            val p0 = pelsP[p0Idx].toInt()
            val q0 = pelsQ[q0Idx].toInt()
            val q1 = pelsQ[q1Idx].toInt()
            val alphaThresh = alphaTab[indexAlpha]
            val betaThresh = betaTab[indexBeta]
            val filterEnabled = Math.abs(p0 - q0) < alphaThresh && Math.abs(p1 - p0) < betaThresh && Math.abs(q1 - q0) < betaThresh
            if (!filterEnabled) return

            // System.out.printf("%h %h %h %h %h %h %h %h\n", q3, q2, q1, q0, p0,
            // p1, p2, p3);
            val tC0 = tcs[bs - 1][indexAlpha]
            val conditionP: Boolean
            val conditionQ: Boolean
            val tC: Int
            if (!isChroma) {
                val ap = Math.abs(pelsP[p2Idx] - p0)
                val aq = Math.abs(pelsQ[q2Idx] - q0)
                tC = tC0 + (if (ap < betaThresh) 1 else 0) + if (aq < betaThresh) 1 else 0
                conditionP = ap < betaThresh
                conditionQ = aq < betaThresh
            } else {
                tC = tC0 + 1
                conditionP = false
                conditionQ = false
            }
            var sigma = (q0 - p0 shl 2) + (p1 - q1) + 4 shr 3
            sigma = if (sigma < -tC) -tC else if (sigma > tC) tC else sigma
            var p0n = p0 + sigma
            p0n = if (p0n < -128) -128 else p0n
            var q0n = q0 - sigma
            q0n = if (q0n < -128) -128 else q0n
            if (conditionP) {
                val p2 = pelsP[p2Idx].toInt()
                var diff = p2 + (p0 + q0 + 1 shr 1) - (p1 shl 1) shr 1
                diff = if (diff < -tC0) -tC0 else if (diff > tC0) tC0 else diff
                val p1n = p1 + diff
                pelsP[p1Idx] = MathUtil.clip(p1n, -128, 127).toByte()
            }
            if (conditionQ) {
                val q2 = pelsQ[q2Idx].toInt()
                var diff = q2 + (p0 + q0 + 1 shr 1) - (q1 shl 1) shr 1
                diff = if (diff < -tC0) -tC0 else if (diff > tC0) tC0 else diff
                val q1n = q1 + diff
                pelsQ[q1Idx] = MathUtil.clip(q1n, -128, 127).toByte()
            }
            pelsQ[q0Idx] = MathUtil.clip(q0n, -128, 127).toByte()
            pelsP[p0Idx] = MathUtil.clip(p0n, -128, 127).toByte()
        }

        fun filterBs4(indexAlpha: Int, indexBeta: Int, pelsP: ByteArray, pelsQ: ByteArray, p3Idx: Int, p2Idx: Int,
                      p1Idx: Int, p0Idx: Int, q0Idx: Int, q1Idx: Int, q2Idx: Int, q3Idx: Int, isChroma: Boolean) {
            val p0 = pelsP[p0Idx].toInt()
            val q0 = pelsQ[q0Idx].toInt()
            val p1 = pelsP[p1Idx].toInt()
            val q1 = pelsQ[q1Idx].toInt()
            val alphaThresh = alphaTab[indexAlpha]
            val betaThresh = betaTab[indexBeta]
            val filterEnabled = Math.abs(p0 - q0) < alphaThresh && Math.abs(p1 - p0) < betaThresh && Math.abs(q1 - q0) < betaThresh
            if (!filterEnabled) return
            val conditionP: Boolean
            val conditionQ: Boolean
            if (isChroma) {
                conditionP = false
                conditionQ = false
            } else {
                val ap = Math.abs(pelsP[p2Idx] - p0)
                val aq = Math.abs(pelsQ[q2Idx] - q0)
                conditionP = ap < betaThresh && Math.abs(p0 - q0) < (alphaThresh shr 2) + 2
                conditionQ = aq < betaThresh && Math.abs(p0 - q0) < (alphaThresh shr 2) + 2
            }
            if (conditionP) {
                val p3 = pelsP[p3Idx].toInt()
                val p2 = pelsP[p2Idx].toInt()
                val p0n = p2 + 2 * p1 + 2 * p0 + 2 * q0 + q1 + 4 shr 3
                val p1n = p2 + p1 + p0 + q0 + 2 shr 2
                val p2n = 2 * p3 + 3 * p2 + p1 + p0 + q0 + 4 shr 3
                pelsP[p0Idx] = MathUtil.clip(p0n, -128, 127).toByte()
                pelsP[p1Idx] = MathUtil.clip(p1n, -128, 127).toByte()
                pelsP[p2Idx] = MathUtil.clip(p2n, -128, 127).toByte()
            } else {
                val p0n = 2 * p1 + p0 + q1 + 2 shr 2
                pelsP[p0Idx] = MathUtil.clip(p0n, -128, 127).toByte()
            }
            if (conditionQ && !isChroma) {
                val q2 = pelsQ[q2Idx].toInt()
                val q3 = pelsQ[q3Idx].toInt()
                val q0n = p1 + 2 * p0 + 2 * q0 + 2 * q1 + q2 + 4 shr 3
                val q1n = p0 + q0 + q1 + q2 + 2 shr 2
                val q2n = 2 * q3 + 3 * q2 + q1 + q0 + p0 + 4 shr 3
                pelsQ[q0Idx] = MathUtil.clip(q0n, -128, 127).toByte()
                pelsQ[q1Idx] = MathUtil.clip(q1n, -128, 127).toByte()
                pelsQ[q2Idx] = MathUtil.clip(q2n, -128, 127).toByte()
            } else {
                val q0n = 2 * q1 + q0 + p1 + 2 shr 2
                pelsQ[q0Idx] = MathUtil.clip(q0n, -128, 127).toByte()
            }
        }
    }

}