package org.jcodec.codecs.h264.encode

import org.jcodec.codecs.h264.io.model.SeqParameterSet
import org.jcodec.common.model.Picture
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Estimates motion using diagonal search
 *
 * @author Stanislav Vitvitskyy
 */
class MotionEstimator(private val ref: Picture?, private val sps: SeqParameterSet, maxSearchRange: Int) {
    private val maxSearchRange: Int
    private val mvTopX: IntArray
    private val mvTopY: IntArray
    private val mvTopR: IntArray
    private var mvLeftX = 0
    private var mvLeftY = 0
    private var mvLeftR = 0
    private var mvTopLeftX = 0
    private var mvTopLeftY = 0
    private var mvTopLeftR = 0
    fun mvEstimate(pic: Picture, mbX: Int, mbY: Int): IntArray {
        val refIdx = 1
        val patch = ByteArray(256)
        val trAvb = mbY > 0 && mbX < sps.picWidthInMbsMinus1
        val tlAvb = mbX > 0 && mbY > 0
        val cx = if (trAvb) mvTopX[mbX + 1] else 0
        val cy = if (trAvb) mvTopY[mbX + 1] else 0
        val cr = if (trAvb) mvTopR[mbX + 1] == refIdx else false
        val dx = if (tlAvb) mvTopLeftX else 0
        val dy = if (tlAvb) mvTopLeftY else 0
        val dr = if (tlAvb) mvTopLeftY == refIdx else false
        val mvpx = H264EncoderUtils.median(mvLeftX, mvLeftR == refIdx, mvTopX[mbX], mvTopR[mbX] == refIdx, cx, cr, dx, dr, mbX > 0,
                mbY > 0, trAvb, tlAvb)
        val mvpy = H264EncoderUtils.median(mvLeftY, mvLeftR == refIdx, mvTopY[mbX], mvTopR[mbX] == refIdx, cy, cr, dy, dr, mbX > 0,
                mbY > 0, trAvb, tlAvb)
        MBEncoderHelper.take(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX shl 4, mbY shl 4,
                patch, 16, 16)
        return estimateFullPix(ref!!, patch, mbX, mbY, mvpx, mvpy)
    }

    fun mvSave(mbX: Int, mbY: Int, mv: IntArray) {
        mvTopLeftX = mvTopX[mbX]
        mvTopLeftY = mvTopY[mbX]
        mvTopLeftR = mvTopR[mbX]
        mvTopX[mbX] = mv[0]
        mvTopY[mbX] = mv[1]
        mvTopR[mbX] = mv[2]
        mvLeftX = mv[0]
        mvLeftY = mv[1]
        mvLeftR = mv[2]
    }

    private fun estimateFullPix(ref: Picture, patch: ByteArray, mbX: Int, mbY: Int, mvpx: Int, mvpy: Int): IntArray {
        val searchPatch = ByteArray((maxSearchRange * 2 + 16) * (maxSearchRange * 2 + 16))
        val startX = mbX shl 4 /* + (mvpx >> 2) */
        val startY = mbY shl 4 /* + (mvpy >> 2) */
        val patchTlX = Math.max(startX - maxSearchRange, 0)
        val patchTlY = Math.max(startY - maxSearchRange, 0)
        val patchBrX = Math.min(startX + maxSearchRange + 16, ref.getPlaneWidth(0))
        val patchBrY = Math.min(startY + maxSearchRange + 16, ref.getPlaneHeight(0))
        val centerX = startX - patchTlX
        val centerY = startY - patchTlY
        val patchW = patchBrX - patchTlX
        val patchH = patchBrY - patchTlY
        MBEncoderHelper.takeSafe(ref.getPlaneData(0), ref.getPlaneWidth(0), ref.getPlaneHeight(0), patchTlX, patchTlY,
                searchPatch, patchW, patchH)
        var bestMvX = centerX
        var bestMvY = centerY
        var bestScore = sad(searchPatch, patchW, patch, bestMvX, bestMvY)
        // Diagonal search
        for (i in 0 until maxSearchRange) {
            val score1 = if (bestMvX > 0) sad(searchPatch, patchW, patch, bestMvX - 1, bestMvY) else Int.MAX_VALUE
            val score2 = if (bestMvX < patchW - 1) sad(searchPatch, patchW, patch, bestMvX + 1, bestMvY) else Int.MAX_VALUE
            val score3 = if (bestMvY > 0) sad(searchPatch, patchW, patch, bestMvX, bestMvY - 1) else Int.MAX_VALUE
            val score4 = if (bestMvY < patchH - 1) sad(searchPatch, patchW, patch, bestMvX, bestMvY + 1) else Int.MAX_VALUE
            val min = Math.min(Math.min(Math.min(score1, score2), score3), score4)
            if (min > bestScore) break
            bestScore = min
            when {
                score1 == min -> --bestMvX
                score2 == min -> ++bestMvX
                score3 == min -> --bestMvY
                else -> ++bestMvY
            }
        }
        return intArrayOf(bestMvX - centerX shl 2 /* + mvpx */, bestMvY - centerY shl 2 /* + mvpy */)
    }

    private fun sad(big: ByteArray, bigStride: Int, small: ByteArray, offX: Int, offY: Int): Int {
        var score = 0
        var bigOff = offY * bigStride + offX
        var smallOff = 0
        for (i in 0..15) {
            var j = 0
            while (j < 16) {
                score += MathUtil.abs(big[bigOff] - small[smallOff])
                j++
                ++bigOff
                ++smallOff
            }
            bigOff += bigStride - 16
        }
        return score
    }

    init {
        mvTopX = IntArray(sps.picWidthInMbsMinus1 + 1)
        mvTopY = IntArray(sps.picWidthInMbsMinus1 + 1)
        mvTopR = IntArray(sps.picWidthInMbsMinus1 + 1)
        this.maxSearchRange = maxSearchRange
    }
}