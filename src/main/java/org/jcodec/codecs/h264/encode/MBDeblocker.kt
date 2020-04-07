package org.jcodec.codecs.h264.encode

import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.decode.deblock.DeblockingFilter
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Contains various deblocking filter routines for deblocking on MB bases
 *
 * @author Stan Vitvitskyy
 */
open class MBDeblocker {
    /**
     * Deblocks bottom edge of topOutMB, right edge of leftOutMB and left/top and
     * inner block edges of outMB
     *
     * @param curPix        Pixels of the current MB
     * @param leftPix       Pixels of the leftMB
     * @param topPix        Pixels of the tipMB
     *
     * @param vertStrength  Border strengths for vertical edges (filtered first)
     * @param horizStrength Border strengths for the horizontal edges
     * @param lastH
     * @param lastW
     *
     * @param curQp         Current MB's qp
     * @param leftQp        Left MB's qp
     * @param topQp         Top MB's qp
     */
    fun deblockMBGeneric(curMB: EncodedMB, leftMB: EncodedMB?, topMB: EncodedMB?, vertStrength: Array<IntArray>,
                         horizStrength: Array<IntArray>) {
        val curPix = curMB.pixels
        val crQpOffset = 0
        val curChQp = calcQpChroma(curMB.qp, crQpOffset)
        if (leftMB != null) {
            val leftPix = leftMB.pixels
            val leftChQp = calcQpChroma(leftMB.qp, crQpOffset)
            val avgQp = MathUtil.clip(leftMB.qp + curMB.qp + 1 shr 1, 0, 51)
            val avgChQp = MathUtil.clip(leftChQp + curChQp + 1 shr 1, 0, 51)
            deblockBorder(vertStrength[0], avgQp, leftPix.getPlaneData(0), 3, curPix.getPlaneData(0), 0, P_POS_V,
                    Q_POS_V, false)
            deblockBorderChroma(vertStrength[0], avgChQp, leftPix.getPlaneData(1), 3, curPix.getPlaneData(1), 0,
                    P_POS_V_CHR, Q_POS_V_CHR, false)
            deblockBorderChroma(vertStrength[0], avgChQp, leftPix.getPlaneData(2), 3, curPix.getPlaneData(2), 0,
                    P_POS_V_CHR, Q_POS_V_CHR, false)
        }
        for (i in 1..3) {
            deblockBorder(vertStrength[i], curMB.qp, curPix.getPlaneData(0), i - 1, curPix.getPlaneData(0), i,
                    P_POS_V, Q_POS_V, false)
        }
        deblockBorderChroma(vertStrength[2], curChQp, curPix.getPlaneData(1), 1, curPix.getPlaneData(1), 2, P_POS_V_CHR,
                Q_POS_V_CHR, false)
        deblockBorderChroma(vertStrength[2], curChQp, curPix.getPlaneData(2), 1, curPix.getPlaneData(2), 2, P_POS_V_CHR,
                Q_POS_V_CHR, false)
        if (topMB != null) {
            val topPix = topMB.pixels
            val topChQp = calcQpChroma(topMB.qp, crQpOffset)
            val avgQp = MathUtil.clip(topMB.qp + curMB.qp + 1 shr 1, 0, 51)
            val avgChQp = MathUtil.clip(topChQp + curChQp + 1 shr 1, 0, 51)
            deblockBorder(horizStrength[0], avgQp, topPix.getPlaneData(0), 3, curPix.getPlaneData(0), 0, P_POS_H,
                    Q_POS_H, true)
            deblockBorderChroma(horizStrength[0], avgChQp, topPix.getPlaneData(1), 3, curPix.getPlaneData(1), 0,
                    P_POS_H_CHR, Q_POS_H_CHR, true)
            deblockBorderChroma(horizStrength[0], avgChQp, topPix.getPlaneData(2), 3, curPix.getPlaneData(2), 0,
                    P_POS_H_CHR, Q_POS_H_CHR, true)
        }
        for (i in 1..3) {
            deblockBorder(horizStrength[i], curMB.qp, curPix.getPlaneData(0), i - 1, curPix.getPlaneData(0), i,
                    P_POS_H, Q_POS_H, true)
        }
        deblockBorderChroma(horizStrength[2], curChQp, curPix.getPlaneData(1), 1, curPix.getPlaneData(1), 2,
                P_POS_H_CHR, Q_POS_H_CHR, true)
        deblockBorderChroma(horizStrength[2], curChQp, curPix.getPlaneData(2), 1, curPix.getPlaneData(2), 2,
                P_POS_H_CHR, Q_POS_H_CHR, true)
    }

    /**
     * @param cur  Pixels and parameters of encoded and reconstructed current
     * macroblock
     * @param left Pixels and parameters of encoded and reconstructed left
     * macroblock
     * @param top  Pixels and parameters of encoded and reconstructed top macroblock
     * @param c
     * @param b
     */
    fun deblockMBP(cur: EncodedMB, left: EncodedMB?, top: EncodedMB?) {
        val vertStrength = Array(4) { IntArray(4) }
        val horizStrength = Array(4) { IntArray(4) }
        calcStrengthForBlocks(cur, left, vertStrength, LOOKUP_IDX_P_V, LOOKUP_IDX_Q_V)
        calcStrengthForBlocks(cur, top, horizStrength, LOOKUP_IDX_P_H, LOOKUP_IDX_Q_H)
        deblockMBGeneric(cur, left, top, vertStrength, horizStrength)
    }

    private fun deblockBorder(boundary: IntArray, qp: Int, p: ByteArray, pi: Int, q: ByteArray, qi: Int, pTab: Array<IntArray>, qTab: Array<IntArray>,
                              horiz: Boolean) {
        val inc1 = if (horiz) 16 else 1
        val inc2 = inc1 * 2
        val inc3 = inc1 * 3
        for (b in 0..3) {
            if (boundary[b] == 4) {
                var i = 0
                var ii = b shl 2
                while (i < 4) {
                    filterBs4(qp, qp, p, q, pTab[pi][ii] - inc3, pTab[pi][ii] - inc2, pTab[pi][ii] - inc1, pTab[pi][ii],
                            qTab[qi][ii], qTab[qi][ii] + inc1, qTab[qi][ii] + inc2, qTab[qi][ii] + inc3)
                    ++i
                    ++ii
                }
            } else if (boundary[b] > 0) {
                var i = 0
                var ii = b shl 2
                while (i < 4) {
                    filterBs(boundary[b], qp, qp, p, q, pTab[pi][ii] - inc2, pTab[pi][ii] - inc1, pTab[pi][ii],
                            qTab[qi][ii], qTab[qi][ii] + inc1, qTab[qi][ii] + inc2)
                    ++i
                    ++ii
                }
            }
        }
    }

    protected open fun filterBs4Chr(indexAlpha: Int, indexBeta: Int, pelsP: ByteArray, pelsQ: ByteArray, p1Idx: Int, p0Idx: Int,
                                    q0Idx: Int, q1Idx: Int) {
        _filterBs4(indexAlpha, indexBeta, pelsP, pelsQ, -1, -1, p1Idx, p0Idx, q0Idx, q1Idx, -1, -1, true)
    }

    protected open fun filterBsChr(bs: Int, indexAlpha: Int, indexBeta: Int, pelsP: ByteArray, pelsQ: ByteArray, p1Idx: Int, p0Idx: Int,
                                   q0Idx: Int, q1Idx: Int) {
        _filterBs(bs, indexAlpha, indexBeta, pelsP, pelsQ, -1, p1Idx, p0Idx, q0Idx, q1Idx, -1, true)
    }

    protected open fun filterBs4(indexAlpha: Int, indexBeta: Int, pelsP: ByteArray, pelsQ: ByteArray, p3Idx: Int, p2Idx: Int, p1Idx: Int,
                                 p0Idx: Int, q0Idx: Int, q1Idx: Int, q2Idx: Int, q3Idx: Int) {
        _filterBs4(indexAlpha, indexBeta, pelsP, pelsQ, p3Idx, p2Idx, p1Idx, p0Idx, q0Idx, q1Idx, q2Idx, q3Idx, false)
    }

    protected open fun filterBs(bs: Int, indexAlpha: Int, indexBeta: Int, pelsP: ByteArray, pelsQ: ByteArray, p2Idx: Int, p1Idx: Int,
                                p0Idx: Int, q0Idx: Int, q1Idx: Int, q2Idx: Int) {
        _filterBs(bs, indexAlpha, indexBeta, pelsP, pelsQ, p2Idx, p1Idx, p0Idx, q0Idx, q1Idx, q2Idx, false)
    }

    protected fun _filterBs4(indexAlpha: Int, indexBeta: Int, pelsP: ByteArray, pelsQ: ByteArray, p3Idx: Int, p2Idx: Int,
                             p1Idx: Int, p0Idx: Int, q0Idx: Int, q1Idx: Int, q2Idx: Int, q3Idx: Int, isChroma: Boolean) {
        val p0 = pelsP[p0Idx].toInt()
        val q0 = pelsQ[q0Idx].toInt()
        val p1 = pelsP[p1Idx].toInt()
        val q1 = pelsQ[q1Idx].toInt()
        val alphaThresh = DeblockingFilter.alphaTab[indexAlpha]
        val betaThresh = DeblockingFilter.betaTab[indexBeta]
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

    protected fun _filterBs(bs: Int, indexAlpha: Int, indexBeta: Int, pelsP: ByteArray, pelsQ: ByteArray, p2Idx: Int, p1Idx: Int,
                            p0Idx: Int, q0Idx: Int, q1Idx: Int, q2Idx: Int, isChroma: Boolean) {
        val p1 = pelsP[p1Idx].toInt()
        val p0 = pelsP[p0Idx].toInt()
        val q0 = pelsQ[q0Idx].toInt()
        val q1 = pelsQ[q1Idx].toInt()
        val alphaThresh = DeblockingFilter.alphaTab[indexAlpha]
        val betaThresh = DeblockingFilter.betaTab[indexBeta]
        val filterEnabled = Math.abs(p0 - q0) < alphaThresh && Math.abs(p1 - p0) < betaThresh && Math.abs(q1 - q0) < betaThresh
        if (!filterEnabled) return
        val tC0 = DeblockingFilter.tcs[bs - 1][indexAlpha]
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

    private fun deblockBorderChroma(boundary: IntArray, qp: Int, p: ByteArray, pi: Int, q: ByteArray, qi: Int, pTab: Array<IntArray>,
                                    qTab: Array<IntArray>, horiz: Boolean) {
        val inc1 = if (horiz) 8 else 1
        for (b in 0..3) {
            if (boundary[b] == 4) {
                var i = 0
                var ii = b shl 1
                while (i < 2) {
                    filterBs4Chr(qp, qp, p, q, pTab[pi][ii] - inc1, pTab[pi][ii], qTab[qi][ii], qTab[qi][ii] + inc1)
                    ++i
                    ++ii
                }
            } else if (boundary[b] > 0) {
                var i = 0
                var ii = b shl 1
                while (i < 2) {
                    filterBsChr(boundary[b], qp, qp, p, q, pTab[pi][ii] - inc1, pTab[pi][ii], qTab[qi][ii],
                            qTab[qi][ii] + inc1)
                    ++i
                    ++ii
                }
            }
        }
    }

    companion object {
        @JvmField
        val LOOKUP_IDX_P_V = arrayOf(intArrayOf(3, 7, 11, 15), intArrayOf(0, 4, 8, 12), intArrayOf(1, 5, 9, 13), intArrayOf(2, 6, 10, 14))

        @JvmField
        val LOOKUP_IDX_Q_V = arrayOf(intArrayOf(0, 4, 8, 12), intArrayOf(1, 5, 9, 13), intArrayOf(2, 6, 10, 14), intArrayOf(3, 7, 11, 15))

        @JvmField
        val LOOKUP_IDX_P_H = arrayOf(intArrayOf(12, 13, 14, 15), intArrayOf(0, 1, 2, 3), intArrayOf(4, 5, 6, 7), intArrayOf(8, 9, 10, 11))

        @JvmField
        val LOOKUP_IDX_Q_H = arrayOf(intArrayOf(0, 1, 2, 3), intArrayOf(4, 5, 6, 7), intArrayOf(8, 9, 10, 11), intArrayOf(12, 13, 14, 15))
        fun calcQpChroma(qp: Int, crQpOffset: Int): Int {
            return H264Const.QP_SCALE_CR[MathUtil.clip(qp + crQpOffset, 0, 51)]
        }

        private fun buildPPosH(): Array<IntArray> {
            val qPos = Array(4) { IntArray(16) }
            for (i in 0..3) {
                for (j in 0..15) {
                    qPos[i][j] = j + (i shl 6) + 48
                }
            }
            return qPos
        }

        private fun buildQPosH(): Array<IntArray> {
            val pPos = Array(4) { IntArray(16) }
            for (i in 0..3) {
                for (j in 0..15) {
                    pPos[i][j] = j + (i shl 6)
                }
            }
            return pPos
        }

        private fun buildPPosV(): Array<IntArray> {
            val qPos = Array(4) { IntArray(16) }
            for (i in 0..3) {
                for (j in 0..15) {
                    qPos[i][j] = (j shl 4) + (i shl 2) + 3
                }
            }
            return qPos
        }

        private fun buildQPosV(): Array<IntArray> {
            val pPos = Array(4) { IntArray(16) }
            for (i in 0..3) {
                for (j in 0..15) {
                    pPos[i][j] = (j shl 4) + (i shl 2)
                }
            }
            return pPos
        }

        private fun buildPPosHChr(): Array<IntArray> {
            val qPos = Array(4) { IntArray(8) }
            for (i in 0..3) {
                for (j in 0..7) {
                    qPos[i][j] = j + (i shl 4) + 8
                }
            }
            return qPos
        }

        private fun buildQPosHChr(): Array<IntArray> {
            val pPos = Array(4) { IntArray(8) }
            for (i in 0..3) {
                for (j in 0..7) {
                    pPos[i][j] = j + (i shl 4)
                }
            }
            return pPos
        }

        private fun buildPPosVChr(): Array<IntArray> {
            val qPos = Array(4) { IntArray(8) }
            for (i in 0..3) {
                for (j in 0..7) {
                    qPos[i][j] = (j shl 3) + (i shl 1) + 1
                }
            }
            return qPos
        }

        private fun buildQPosVChr(): Array<IntArray> {
            val pPos = Array(4) { IntArray(8) }
            for (i in 0..3) {
                for (j in 0..7) {
                    pPos[i][j] = (j shl 3) + (i shl 1)
                }
            }
            return pPos
        }

        @JvmStatic
        fun calcStrengthForBlocks(cur: EncodedMB, other: EncodedMB?, outStrength: Array<IntArray>, LOOKUP_IDX_P: Array<IntArray>,
                                  LOOKUP_IDX_Q: Array<IntArray>) {
            val thisIntra = cur.type!!.isIntra
            if (other != null) {
                val otherIntra = other.type!!.isIntra
                for (i in 0..3) {
                    val bsMvx = strengthMv(other.mx[LOOKUP_IDX_P[0][i]], cur.mx[LOOKUP_IDX_Q[0][i]])
                    val bsMvy = strengthMv(other.my[LOOKUP_IDX_P[0][i]], cur.my[LOOKUP_IDX_Q[0][i]])
                    val bsNc = strengthNc(other.nc[LOOKUP_IDX_P[0][i]], cur.nc[LOOKUP_IDX_Q[0][i]])
                    val max3 = MathUtil.max3(bsMvx, bsMvy, bsNc)
                    outStrength[0][i] = if (otherIntra || thisIntra) 4 else max3
                }
            }
            for (i in 1..3) {
                for (j in 0..3) {
                    val bsMvx = strengthMv(cur.mx[LOOKUP_IDX_P[i][j]], cur.mx[LOOKUP_IDX_Q[i][j]])
                    val bsMvy = strengthMv(cur.my[LOOKUP_IDX_P[i][j]], cur.my[LOOKUP_IDX_Q[i][j]])
                    val bsNc = strengthNc(cur.nc[LOOKUP_IDX_P[i][j]], cur.nc[LOOKUP_IDX_Q[i][j]])
                    val max3 = MathUtil.max3(bsMvx, bsMvy, bsNc)
                    outStrength[i][j] = if (thisIntra) 3 else max3
                }
            }
        }

        private fun strengthNc(ncA: Int, ncB: Int): Int {
            return if (ncA > 0 || ncB > 0) 2 else 0
        }

        private fun strengthMv(v0: Int, v1: Int): Int {
            return if (Math.abs(v0 - v1) >= 4) 1 else 0
        }

        private val P_POS_V = buildPPosV()
        private val Q_POS_V = buildQPosV()
        private val P_POS_H = buildPPosH()
        private val Q_POS_H = buildQPosH()
        private val P_POS_V_CHR = buildPPosVChr()
        private val Q_POS_V_CHR = buildQPosVChr()
        private val P_POS_H_CHR = buildPPosHChr()
        private val Q_POS_H_CHR = buildQPosHChr()
    }
}