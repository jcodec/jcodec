package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Utils.Mv
import org.jcodec.codecs.h264.H264Utils.MvList
import org.jcodec.common.ArrayUtil
import org.jcodec.common.logging.Logger
import org.jcodec.common.model.Picture
import org.jcodec.common.tools.MathUtil

object MBlockDecoderUtils {
    private const val debug = false
    val NULL_VECTOR = Mv.packMv(0, 0, -1)
    @JvmStatic
    fun debugPrint(vararg arguments: Any) {
        if (debug && arguments.size > 0) {
            if (arguments.size == 1) {
                Logger.debug("" + arguments[0])
            } else {
                val fmt = arguments[0] as String
                ArrayUtil.shiftLeft1(arguments)
                Logger.debug(String.format(fmt, *arguments))
            }
        }
    }

    fun collectPredictors(sharedState: DecoderState, outMB: Picture, mbX: Int) {
        sharedState.topLeft[0][0] = sharedState.topLine[0][(mbX shl 4) + 15]
        sharedState.topLeft[0][1] = outMB.getPlaneData(0)[63]
        sharedState.topLeft[0][2] = outMB.getPlaneData(0)[127]
        sharedState.topLeft[0][3] = outMB.getPlaneData(0)[191]
        System.arraycopy(outMB.getPlaneData(0), 240, sharedState.topLine[0], mbX shl 4, 16)
        copyCol(outMB.getPlaneData(0), 16, 15, 16, sharedState.leftRow[0])
        collectChromaPredictors(sharedState, outMB, mbX)
    }

    fun collectChromaPredictors(sharedState: DecoderState, outMB: Picture, mbX: Int) {
        sharedState.topLeft[1][0] = sharedState.topLine[1][(mbX shl 3) + 7]
        sharedState.topLeft[2][0] = sharedState.topLine[2][(mbX shl 3) + 7]
        System.arraycopy(outMB.getPlaneData(1), 56, sharedState.topLine[1], mbX shl 3, 8)
        System.arraycopy(outMB.getPlaneData(2), 56, sharedState.topLine[2], mbX shl 3, 8)
        copyCol(outMB.getPlaneData(1), 8, 7, 8, sharedState.leftRow[1])
        copyCol(outMB.getPlaneData(2), 8, 7, 8, sharedState.leftRow[2])
    }

    private fun copyCol(planeData: ByteArray, n: Int, off: Int, stride: Int, out: ByteArray) {
        var off = off
        var i = 0
        while (i < n) {
            out[i] = planeData[off]
            i++
            off += stride
        }
    }

    fun saveMvsIntra(di: DeblockerInput, mbX: Int, mbY: Int) {
        var j = 0
        var blkOffY = mbY shl 2
        var blkInd = 0
        while (j < 4) {
            var i = 0
            var blkOffX = mbX shl 2
            while (i < 4) {
                di.mvs.setMv(blkOffX, blkOffY, 0, NULL_VECTOR)
                di.mvs.setMv(blkOffX, blkOffY, 1, NULL_VECTOR)
                i++
                blkOffX++
                blkInd++
            }
            j++
            blkOffY++
        }
    }

    fun mergeResidual(mb: Picture, residual: Array<Array<IntArray>>, blockLUT: Array<IntArray>, posLUT: Array<IntArray>) {
        for (comp in 0..2) {
            val to = mb.getPlaneData(comp)
            for (i in to.indices) {
                to[i] = MathUtil.clip(to[i] + residual[comp][blockLUT[comp][i]][posLUT[comp][i]], -128, 127).toByte()
            }
        }
    }

    fun saveVect(mv: MvList, list: Int, from: Int, to: Int, vect: Int) {
        for (i in from until to) {
            mv.setMv(i, list, vect)
        }
    }

    /**
     * Calculates median prediction
     * @param a, b, c and d are packed motion vectors
     */
    fun calcMVPredictionMedian(a: Int, b: Int, c: Int, d: Int, aAvb: Boolean, bAvb: Boolean, cAvb: Boolean,
                               dAvb: Boolean, ref: Int, comp: Int): Int {
        var a = a
        var b = b
        var c = c
        var bAvb = bAvb
        var cAvb = cAvb
        if (!cAvb) {
            c = d
            cAvb = dAvb
        }
        if (aAvb && !bAvb && !cAvb) {
            c = a
            b = c
            cAvb = aAvb
            bAvb = cAvb
        }
        a = if (aAvb) a else NULL_VECTOR
        b = if (bAvb) b else NULL_VECTOR
        c = if (cAvb) c else NULL_VECTOR
        if (Mv.mvRef(a) == ref && Mv.mvRef(b) != ref && Mv.mvRef(c) != ref) return Mv.mvC(a, comp) else if (Mv.mvRef(b) == ref && Mv.mvRef(a) != ref && Mv.mvRef(c) != ref) return Mv.mvC(b, comp) else if (Mv.mvRef(c) == ref && Mv.mvRef(a) != ref && Mv.mvRef(b) != ref) return Mv.mvC(c, comp)
        return (Mv.mvC(a, comp) + Mv.mvC(b, comp) + Mv.mvC(c, comp) - min(Mv.mvC(a, comp), Mv.mvC(b, comp), Mv.mvC(c, comp))
                - max(Mv.mvC(a, comp), Mv.mvC(b, comp), Mv.mvC(c, comp)))
    }

    fun max(x: Int, x2: Int, x3: Int): Int {
        return if (x > x2) if (x > x3) x else x3 else if (x2 > x3) x2 else x3
    }

    fun min(x: Int, x2: Int, x3: Int): Int {
        return if (x < x2) if (x < x3) x else x3 else if (x2 < x3) x2 else x3
    }

    fun saveMvs(di: DeblockerInput, x: MvList, mbX: Int, mbY: Int) {
        var j = 0
        var blkOffY = mbY shl 2
        var blkInd = 0
        while (j < 4) {
            var i = 0
            var blkOffX = mbX shl 2
            while (i < 4) {
                di.mvs.setMv(blkOffX, blkOffY, 0, x.getMv(blkInd, 0))
                di.mvs.setMv(blkOffX, blkOffY, 1, x.getMv(blkInd, 1))
                i++
                blkOffX++
                blkInd++
            }
            j++
            blkOffY++
        }
    }

    fun savePrediction8x8(sharedState: DecoderState, mbX: Int, x: MvList?) {
        sharedState.mvTopLeft.copyPair(0, sharedState.mvTop, (mbX shl 2) + 3)
        sharedState.mvLeft.copyPair(0, x, 3)
        sharedState.mvLeft.copyPair(1, x, 7)
        sharedState.mvLeft.copyPair(2, x, 11)
        sharedState.mvLeft.copyPair(3, x, 15)
        sharedState.mvTop.copyPair(mbX shl 2, x, 12)
        sharedState.mvTop.copyPair((mbX shl 2) + 1, x, 13)
        sharedState.mvTop.copyPair((mbX shl 2) + 2, x, 14)
        sharedState.mvTop.copyPair((mbX shl 2) + 3, x, 15)
    }

    fun saveVectIntra(sharedState: DecoderState, mbX: Int) {
        val xx = mbX shl 2
        sharedState.mvTopLeft.copyPair(0, sharedState.mvTop, xx + 3)
        saveVect(sharedState.mvTop, 0, xx, xx + 4, NULL_VECTOR)
        saveVect(sharedState.mvLeft, 0, 0, 4, NULL_VECTOR)
        saveVect(sharedState.mvTop, 1, xx, xx + 4, NULL_VECTOR)
        saveVect(sharedState.mvLeft, 1, 0, 4, NULL_VECTOR)
    }
}