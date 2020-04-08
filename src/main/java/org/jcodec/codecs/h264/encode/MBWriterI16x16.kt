package org.jcodec.codecs.h264.encode

import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.H264Encoder.NonRdVector
import org.jcodec.codecs.h264.decode.CoeffTransformer
import org.jcodec.codecs.h264.decode.CoeffTransformer.dequantizeAC
import org.jcodec.codecs.h264.decode.CoeffTransformer.dequantizeDC2x2
import org.jcodec.codecs.h264.decode.CoeffTransformer.dequantizeDC4x2
import org.jcodec.codecs.h264.decode.CoeffTransformer.dequantizeDC4x4
import org.jcodec.codecs.h264.decode.CoeffTransformer.fdct4x4
import org.jcodec.codecs.h264.decode.CoeffTransformer.fvdDC2x2
import org.jcodec.codecs.h264.decode.CoeffTransformer.fvdDC4x2
import org.jcodec.codecs.h264.decode.CoeffTransformer.fvdDC4x4
import org.jcodec.codecs.h264.decode.CoeffTransformer.idct4x4
import org.jcodec.codecs.h264.decode.CoeffTransformer.invDC2x2
import org.jcodec.codecs.h264.decode.CoeffTransformer.invDC4x2
import org.jcodec.codecs.h264.decode.CoeffTransformer.invDC4x4
import org.jcodec.codecs.h264.decode.CoeffTransformer.quantizeAC
import org.jcodec.codecs.h264.decode.CoeffTransformer.quantizeDC2x2
import org.jcodec.codecs.h264.decode.CoeffTransformer.quantizeDC4x2
import org.jcodec.codecs.h264.decode.CoeffTransformer.quantizeDC4x4
import org.jcodec.codecs.h264.decode.CoeffTransformer.reorderDC4x4
import org.jcodec.codecs.h264.decode.Intra16x16PredictionBuilder.lumaPred
import org.jcodec.codecs.h264.io.CAVLC
import org.jcodec.codecs.h264.io.model.MBType
import org.jcodec.codecs.h264.io.write.CAVLCWriter.writeSE
import org.jcodec.codecs.h264.io.write.CAVLCWriter.writeUE
import org.jcodec.common.io.BitWriter
import org.jcodec.common.io.VLC
import org.jcodec.common.model.Picture
import org.jcodec.common.tools.MathUtil

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Encodes macroblock as I16x16
 *
 * @author Stanislav Vitvitskyy
 */
class MBWriterI16x16 {
    fun encodeMacroblock(ctx: EncodingContext, pic: Picture, mbX: Int, mbY: Int, out: BitWriter, outMB: EncodedMB,
                         qp: Int, params: NonRdVector) {
        writeUE(out, params.chrPred)
        writeSE(out, qp - ctx.prevQp) // MB QP delta
        outMB.type = MBType.I_16x16
        outMB.qp = qp
        luma(ctx, pic, mbX, mbY, out, qp, outMB.pixels, params.lumaPred)
        chroma(ctx, pic, mbX, mbY, out, qp, outMB.pixels, params.chrPred)
        ctx.prevQp = qp
    }

    private fun chroma(ctx: EncodingContext, pic: Picture, mbX: Int, mbY: Int, out: BitWriter, qp: Int, outMB: Picture, chrPred: Int) {
        val x = mbX shl 3
        val y = mbY shl 3
        val ac1 = Array(4) { IntArray(16) }
        val ac2 = Array(4) { IntArray(16) }
        val pred1 = Array(4) { ByteArray(16) }
        val pred2 = Array(4) { ByteArray(16) }
        predictChroma(ctx, pic, ac1, pred1, 1, x, y)
        predictChroma(ctx, pic, ac2, pred2, 2, x, y)
        chromaResidual(mbX, mbY, out, qp, ac1, ac2, ctx.cavlc!![1]!!, ctx.cavlc!![2]!!, MBType.I_16x16, MBType.I_16x16)
        putChroma(outMB.data[1], 1, x, y, ac1, pred1)
        putChroma(outMB.data[2], 2, x, y, ac2, pred2)
    }

    private fun luma(ctx: EncodingContext, pic: Picture, mbX: Int, mbY: Int, out: BitWriter, qp: Int, outMB: Picture, predType: Int) {
        val x = mbX shl 4
        val y = mbY shl 4
        val ac = Array(16) { IntArray(16) }
        val pred = Array(16) { ByteArray(16) }
        lumaPred(predType, x != 0, y != 0, ctx.leftRow[0], ctx.topLine[0], ctx.topLeft[0], x, pred)
        transform(pic, 0, ac, pred, x, y)
        val dc = extractDC(ac)
        writeDC(ctx.cavlc!![0]!!, mbX, mbY, out, qp, mbX shl 2, mbY shl 2, dc, MBType.I_16x16, MBType.I_16x16)
        writeAC(ctx.cavlc!![0]!!, mbX, mbY, out, mbX shl 2, mbY shl 2, ac, qp, MBType.I_16x16, MBType.I_16x16, DUMMY)
        restorePlane(dc, ac, qp)
        for (blk in ac.indices) {
            MBEncoderHelper.putBlk(outMB.getPlaneData(0), ac[blk], pred[blk], 4, H264Const.BLK_X[blk], H264Const.BLK_Y[blk], 4, 4)
        }
    }

    private fun putChroma(mb: ByteArray, comp: Int, x: Int, y: Int, ac: Array<IntArray>, pred: Array<ByteArray>) {
        MBEncoderHelper.putBlk(mb, ac[0], pred[0], 3, 0, 0, 4, 4)
        MBEncoderHelper.putBlk(mb, ac[1], pred[1], 3, 4, 0, 4, 4)
        MBEncoderHelper.putBlk(mb, ac[2], pred[2], 3, 0, 4, 4, 4)
        MBEncoderHelper.putBlk(mb, ac[3], pred[3], 3, 4, 4, 4, 4)
    }

    private fun predictChroma(ctx: EncodingContext, pic: Picture, ac: Array<IntArray>, pred: Array<ByteArray>, comp: Int, x: Int, y: Int) {
        chromaPredBlk0(ctx, comp, x, y, pred[0])
        chromaPredBlk1(ctx, comp, x, y, pred[1])
        chromaPredBlk2(ctx, comp, x, y, pred[2])
        chromaPredBlk3(ctx, comp, x, y, pred[3])
        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x, y,
                ac[0], pred[0], 4, 4)
        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + 4,
                y, ac[1], pred[1], 4, 4)
        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x,
                y + 4, ac[2], pred[2], 4, 4)
        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + 4,
                y + 4, ac[3], pred[3], 4, 4)
    }

    private fun chromaPredOne(pix: ByteArray, x: Int): Int {
        return pix[x] + pix[x + 1] + pix[x + 2] + pix[x + 3] + 2 shr 2
    }

    private fun chromaPredTwo(pix1: ByteArray, pix2: ByteArray, x: Int, y: Int): Int {
        return (pix1[x] + pix1[x + 1] + pix1[x + 2] + pix1[x + 3] + pix2[y] + pix2[y + 1] + pix2[y + 2] + pix2[y + 3]
                + 4) shr 3
    }

    private fun chromaPredBlk0(ctx: EncodingContext, comp: Int, x: Int, y: Int, pred: ByteArray) {
        val dc: Int
        val predY = y and 0x7
        dc = if (x != 0 && y != 0) chromaPredTwo(ctx.leftRow[comp], ctx.topLine[comp], predY, x) else if (x != 0) chromaPredOne(ctx.leftRow[comp], predY) else if (y != 0) chromaPredOne(ctx.topLine[comp], x) else 0
        for (i in pred.indices) {
            pred[i] = (pred[i].toInt() + dc).toByte()
        }
    }

    private fun chromaPredBlk1(ctx: EncodingContext, comp: Int, x: Int, y: Int, pred: ByteArray) {
        val dc: Int
        val predY = y and 0x7
        dc = if (y != 0) chromaPredOne(ctx.topLine[comp], x + 4) else if (x != 0) chromaPredOne(ctx.leftRow[comp], predY) else 0
        for (i in pred.indices) {
            pred[i] = (pred[i].toInt() + dc).toByte()
        }
    }

    private fun chromaPredBlk2(ctx: EncodingContext, comp: Int, x: Int, y: Int, pred: ByteArray) {
        val dc: Int
        val predY = y and 0x7
        dc = if (x != 0) chromaPredOne(ctx.leftRow[comp], predY + 4) else if (y != 0) chromaPredOne(ctx.topLine[comp], x) else 0
        for (i in pred.indices) {
            pred[i] = (pred[i].toInt() + dc).toByte()
        }
    }

    private fun chromaPredBlk3(ctx: EncodingContext, comp: Int, x: Int, y: Int, pred: ByteArray) {
        val dc: Int
        val predY = y and 0x7
        dc = if (x != 0 && y != 0) chromaPredTwo(ctx.leftRow[comp], ctx.topLine[comp], predY + 4, x + 4) else if (x != 0) chromaPredOne(ctx.leftRow[comp], predY + 4) else if (y != 0) chromaPredOne(ctx.topLine[comp], x + 4) else 0
        for (i in pred.indices) {
            pred[i] = (pred[i].toInt() + dc).toByte()
        }
    }

    private fun transform(pic: Picture, comp: Int, ac: Array<IntArray>, pred: Array<ByteArray>, x: Int, y: Int) {
        for (i in ac.indices) {
            val coeff = ac[i]
            MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp),
                    x + H264Const.BLK_X[i], y + H264Const.BLK_Y[i], coeff, pred[i], 4, 4)
            fdct4x4(coeff)
        }
    }

    fun getCbpChroma(pic: Picture?, mbX: Int, mbY: Int): Int {
        return 2
    }

    fun getCbpLuma(pic: Picture?, mbX: Int, mbY: Int): Int {
        return 15
    }

    companion object {
        private val DUMMY = IntArray(16)
        fun calcQpChroma(qp: Int, crQpOffset: Int): Int {
            return H264Const.QP_SCALE_CR[MathUtil.clip(qp + crQpOffset, 0, 51)]
        }

        @JvmStatic
        fun chromaResidual(mbX: Int, mbY: Int, out: BitWriter, qp: Int, ac1: Array<IntArray>, ac2: Array<IntArray>, cavlc1: CAVLC,
                           cavlc2: CAVLC, leftMBType: MBType, topMBType: MBType) {
            val crQpOffset = 0
            val chrQp = calcQpChroma(qp, crQpOffset)
            transformChroma(ac1)
            transformChroma(ac2)
            val dc1 = extractDC(ac1)
            val dc2 = extractDC(ac2)
            writeDC(cavlc1, mbX, mbY, out, chrQp, mbX shl 1, mbY shl 1, dc1, leftMBType, topMBType)
            writeDC(cavlc2, mbX, mbY, out, chrQp, mbX shl 1, mbY shl 1, dc2, leftMBType, topMBType)
            writeAC(cavlc1, mbX, mbY, out, mbX shl 1, mbY shl 1, ac1, chrQp, leftMBType, topMBType, DUMMY)
            writeAC(cavlc2, mbX, mbY, out, mbX shl 1, mbY shl 1, ac2, chrQp, leftMBType, topMBType, DUMMY)
            restorePlane(dc1, ac1, chrQp)
            restorePlane(dc2, ac2, chrQp)
        }

        private fun restorePlane(dc: IntArray, ac: Array<IntArray>, qp: Int) {
            if (dc.size == 4) {
                invDC2x2(dc)
                dequantizeDC2x2(dc, qp, null)
            } else if (dc.size == 8) {
                invDC4x2(dc)
                dequantizeDC4x2(dc, qp)
            } else {
                invDC4x4(dc)
                dequantizeDC4x4(dc, qp, null)
                reorderDC4x4(dc)
            }
            for (i in ac.indices) {
                dequantizeAC(ac[i], qp, null)
                ac[i][0] = dc[i]
                idct4x4(ac[i])
            }
        }

        private fun extractDC(ac: Array<IntArray>): IntArray {
            val dc = IntArray(ac.size)
            for (i in ac.indices) {
                dc[i] = ac[i][0]
                ac[i][0] = 0
            }
            return dc
        }

        private fun writeAC(cavlc: CAVLC, mbX: Int, mbY: Int, out: BitWriter, mbLeftBlk: Int, mbTopBlk: Int, ac: Array<IntArray>,
                            qp: Int, leftMBType: MBType, topMBType: MBType, nc: IntArray) {
            for (i in ac.indices) {
                quantizeAC(ac[i], qp)
                nc[H264Const.BLK_INV_MAP[i]] = CAVLC.totalCoeff(cavlc.writeACBlock(out, mbLeftBlk + H264Const.MB_BLK_OFF_LEFT[i], mbTopBlk + H264Const.MB_BLK_OFF_TOP[i],
                        leftMBType, topMBType, ac[i], H264Const.totalZeros16 as Array<VLC?>, 1, 15, CoeffTransformer.zigzag4x4))
            }
        }

        private fun writeDC(cavlc: CAVLC, mbX: Int, mbY: Int, out: BitWriter, qp: Int, mbLeftBlk: Int, mbTopBlk: Int,
                            dc: IntArray, leftMBType: MBType, topMBType: MBType) {
            if (dc.size == 4) {
                quantizeDC2x2(dc, qp)
                fvdDC2x2(dc)
                cavlc.writeChrDCBlock(out, dc, H264Const.totalZeros4 as Array<VLC?>, 0, dc.size, intArrayOf(0, 1, 2, 3))
            } else if (dc.size == 8) {
                quantizeDC4x2(dc, qp)
                fvdDC4x2(dc)
                cavlc.writeChrDCBlock(out, dc, H264Const.totalZeros8 as Array<VLC?>, 0, dc.size, intArrayOf(0, 1, 2, 3, 4, 5, 6, 7))
            } else {
                reorderDC4x4(dc)
                quantizeDC4x4(dc, qp)
                fvdDC4x4(dc)
                // TODO: calc here
                cavlc.writeLumaDCBlock(out, mbLeftBlk, mbTopBlk, leftMBType, topMBType, dc, H264Const.totalZeros16 as Array<VLC?>, 0, 16,
                        CoeffTransformer.zigzag4x4)
            }
        }

        private fun transformChroma(ac: Array<IntArray>) {
            for (i in 0..3) {
                fdct4x4(ac[i])
            }
        }
    }
}