package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Const.PartPred
import org.jcodec.codecs.h264.io.model.Frame
import org.jcodec.codecs.h264.io.model.SliceHeader
import org.jcodec.codecs.h264.io.model.SliceType
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Prediction merge and weight routines
 *
 * @author The JCodec project
 */
object PredictionMerger {
    fun mergePrediction(sh: SliceHeader, refIdxL0: Int, refIdxL1: Int, predType: PartPred?, comp: Int,
                        pred0: ByteArray, pred1: ByteArray?, off: Int, stride: Int, blkW: Int, blkH: Int, out: ByteArray, refs: Array<Array<Frame?>>, thisPoc: Int) {
        val pps = sh.pps
        if (sh.sliceType == SliceType.P) {
            weightPrediction(sh, refIdxL0, comp, pred0, off, stride, blkW, blkH, out)
        } else {
            if (!pps!!.isWeightedPredFlag || sh.pps!!.weightedBipredIdc == 0 || sh.pps!!.weightedBipredIdc == 2 && predType != PartPred.Bi) {
                mergeAvg(pred0, pred1, stride, predType, off, blkW, blkH, out)
            } else if (sh.pps!!.weightedBipredIdc == 1) {
                val w = sh.predWeightTable
                val w0 = if (refIdxL0 == -1) 0 else if (comp == 0) w!!.lumaWeight[0]!![refIdxL0] else w!!.chromaWeight[0]!![comp - 1][refIdxL0]
                val w1 = if (refIdxL1 == -1) 0 else if (comp == 0) w!!.lumaWeight[1]!![refIdxL1] else w!!.chromaWeight[1]!![comp - 1][refIdxL1]
                val o0 = if (refIdxL0 == -1) 0 else if (comp == 0) w!!.lumaOffset[0]!![refIdxL0] else w!!.chromaOffset[0]!![comp - 1][refIdxL0]
                val o1 = if (refIdxL1 == -1) 0 else if (comp == 0) w!!.lumaOffset[1]!![refIdxL1] else w!!.chromaOffset[1]!![comp - 1][refIdxL1]
                mergeWeight(pred0, pred1, stride, predType, off, blkW, blkH, if (comp == 0) w!!.lumaLog2WeightDenom else w!!.chromaLog2WeightDenom, w0, w1, o0, o1, out)
            } else {
                val tb = MathUtil.clip(thisPoc - refs[0][refIdxL0]!!.pOC, -128, 127)
                val td = MathUtil.clip(refs[1][refIdxL1]!!.pOC - refs[0][refIdxL0]!!.pOC, -128, 127)
                var w0 = 32
                var w1 = 32
                if (td != 0 && refs[0][refIdxL0]!!.isShortTerm && refs[1][refIdxL1]!!.isShortTerm) {
                    val tx = (16384 + Math.abs(td / 2)) / td
                    val dsf = MathUtil.clip(tb * tx + 32 shr 6, -1024, 1023) shr 2
                    if (dsf >= -64 && dsf <= 128) {
                        w1 = dsf
                        w0 = 64 - dsf
                    }
                }
                mergeWeight(pred0, pred1, stride, predType, off, blkW, blkH, 5, w0, w1, 0, 0, out)
            }
        }
    }

    fun weightPrediction(sh: SliceHeader, refIdxL0: Int, comp: Int, pred0: ByteArray, off: Int, stride: Int,
                         blkW: Int, blkH: Int, out: ByteArray) {
        val pps = sh.pps
        if (pps!!.isWeightedPredFlag && sh.predWeightTable != null) {
            val w = sh.predWeightTable
            weight(pred0, stride, off, blkW, blkH, if (comp == 0) w!!.lumaLog2WeightDenom else w!!.chromaLog2WeightDenom, if (comp == 0) w.lumaWeight[0]!![refIdxL0] else w.chromaWeight[0]!![comp - 1][refIdxL0], if (comp == 0) w.lumaOffset[0]!![refIdxL0] else w.chromaOffset[0]!![comp - 1][refIdxL0], out)
        } else {
            copyPrediction(pred0, stride, off, blkW, blkH, out)
        }
    }

    private fun mergeAvg(blk0: ByteArray, blk1: ByteArray?, stride: Int, p0: PartPred?, off: Int, blkW: Int, blkH: Int,
                         out: ByteArray) {
        if (p0 == PartPred.Bi) _mergePrediction(blk0, blk1, stride, p0, off, blkW, blkH, out) else if (p0 == PartPred.L0) copyPrediction(blk0, stride, off, blkW, blkH, out) else if (p0 == PartPred.L1) copyPrediction(blk1, stride, off, blkW, blkH, out)
    }

    private fun mergeWeight(blk0: ByteArray, blk1: ByteArray?, stride: Int, partPred: PartPred?, off: Int, blkW: Int,
                            blkH: Int, logWD: Int, w0: Int, w1: Int, o0: Int, o1: Int, out: ByteArray) {
        if (partPred == PartPred.L0) {
            weight(blk0, stride, off, blkW, blkH, logWD, w0, o0, out)
        } else if (partPred == PartPred.L1) {
            weight(blk1, stride, off, blkW, blkH, logWD, w1, o1, out)
        } else if (partPred == PartPred.Bi) {
            _weightPrediction(blk0, blk1, stride, off, blkW, blkH, logWD, w0, w1, o0, o1, out)
        }
    }

    private fun copyPrediction(_in: ByteArray?, stride: Int, off: Int, blkW: Int, blkH: Int, out: ByteArray) {
        var off = off
        var i = 0
        while (i < blkH) {
            var j = 0
            while (j < blkW) {
                out[off] = _in!![off]
                j++
                off++
            }
            i++
            off += stride - blkW
        }
    }

    private fun _mergePrediction(blk0: ByteArray, blk1: ByteArray?, stride: Int, p0: PartPred, off: Int, blkW: Int, blkH: Int,
                                 out: ByteArray) {
        var off = off
        var i = 0
        while (i < blkH) {
            var j = 0
            while (j < blkW) {
                out[off] = (blk0[off] + blk1!![off] + 1 shr 1).toByte()
                j++
                off++
            }
            i++
            off += stride - blkW
        }
    }

    private fun _weightPrediction(blk0: ByteArray, blk1: ByteArray?, stride: Int, off: Int, blkW: Int, blkH: Int, logWD: Int,
                                  w0: Int, w1: Int, o0: Int, o1: Int, out: ByteArray) {
        // Necessary to correctly scale in [-128, 127] range
        var off = off
        val round = (1 shl logWD) + (w0 + w1 shl 7)
        val sum = (o0 + o1 + 1 shr 1) - 128
        val logWDCP1 = logWD + 1
        var i = 0
        while (i < blkH) {
            var j = 0
            while (j < blkW) {
                out[off] = MathUtil.clip((blk0[off] * w0 + blk1!![off] * w1 + round shr logWDCP1) + sum, -128, 127).toByte()
                j++
                off++
            }
            i++
            off += stride - blkW
        }
    }

    private fun weight(blk0: ByteArray?, stride: Int, off: Int, blkW: Int, blkH: Int, logWD: Int, w: Int, o: Int, out: ByteArray) {
        var off = off
        var o = o
        var round = 1 shl logWD - 1
        if (logWD >= 1) {
            // Necessary to correctly scale _in [-128, 127] range,
            // i.e. x = ay / b; x,y _in [0, 255] is
            // x = a * (y + 128) / b - 128; x,y _in [-128, 127]
            o -= 128
            round += w shl 7
            var i = 0
            while (i < blkH) {
                var j = 0
                while (j < blkW) {
                    out[off] = MathUtil.clip(( blk0!![off] * w + round shr logWD) + o, -128, 127).toByte()
                    j++
                    off++
                }
                i++
                off += stride - blkW
            }
        } else {
            // Necessary to correctly scale in [-128, 127] range
            o += (w shl 7) - 128
            var i = 0
            while (i < blkH) {
                var j = 0
                while (j < blkW) {
                    out[off] = MathUtil.clip(blk0!![off] * w + o, -128, 127).toByte()
                    j++
                    off++
                }
                i++
                off += stride - blkW
            }
        }
    }
}