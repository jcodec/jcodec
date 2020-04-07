package org.jcodec.codecs.h264.encode

import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.H264Encoder.NonRdVector
import org.jcodec.codecs.h264.decode.BlockInterpolator
import org.jcodec.codecs.h264.decode.BlockInterpolator.Companion.getBlockChroma
import org.jcodec.codecs.h264.decode.CoeffTransformer
import org.jcodec.codecs.h264.decode.CoeffTransformer.dequantizeAC
import org.jcodec.codecs.h264.decode.CoeffTransformer.fdct4x4
import org.jcodec.codecs.h264.decode.CoeffTransformer.idct4x4
import org.jcodec.codecs.h264.decode.CoeffTransformer.quantizeAC
import org.jcodec.codecs.h264.encode.MBWriterI16x16.Companion.chromaResidual
import org.jcodec.codecs.h264.io.model.MBType
import org.jcodec.codecs.h264.io.model.SeqParameterSet
import org.jcodec.codecs.h264.io.write.CAVLCWriter.writeSE
import org.jcodec.codecs.h264.io.write.CAVLCWriter.writeTE
import org.jcodec.codecs.h264.io.write.CAVLCWriter.writeUE
import org.jcodec.common.io.BitWriter
import org.jcodec.common.model.Picture
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Encodes macroblock as P16x16
 *
 * @author Stanislav Vitvitskyy
 */
class MBWriterP16x16(private val sps: SeqParameterSet, private val ref: Picture?) {
    private val interpolator: BlockInterpolator
    fun encodeMacroblock(ctx: EncodingContext, pic: Picture, mbX: Int, mbY: Int, out: BitWriter, outMB: EncodedMB,
                         qp: Int, params: NonRdVector) {
        if (sps.numRefFrames > 1) {
            val refIdx = decideRef()
            writeTE(out, refIdx, sps.numRefFrames - 1)
        }
        val partBlkSize = 4 // 16x16
        val refIdx = 1
        val trAvb = mbY > 0 && mbX < sps.picWidthInMbsMinus1
        val tlAvb = mbX > 0 && mbY > 0
        val ax = ctx.mvLeftX[0]
        val ay = ctx.mvLeftY[0]
        val ar = ctx.mvLeftR[0] == refIdx
        val bx = ctx.mvTopX[mbX shl 2]
        val by = ctx.mvTopY[mbX shl 2]
        val br = ctx.mvTopR[mbX shl 2] == refIdx
        val cx = if (trAvb) ctx.mvTopX[(mbX shl 2) + partBlkSize] else 0
        val cy = if (trAvb) ctx.mvTopY[(mbX shl 2) + partBlkSize] else 0
        val cr = if (trAvb) ctx.mvTopR[(mbX shl 2) + partBlkSize] == refIdx else false
        val dx = if (tlAvb) ctx.mvTopLeftX else 0
        val dy = if (tlAvb) ctx.mvTopLeftY else 0
        val dr = if (tlAvb) ctx.mvTopLeftR == refIdx else false
        val mvpx = H264EncoderUtils.median(ax, ar, bx, br, cx, cr, dx, dr, mbX > 0, mbY > 0, trAvb, tlAvb)
        val mvpy = H264EncoderUtils.median(ay, ar, by, br, cy, cr, dy, dr, mbX > 0, mbY > 0, trAvb, tlAvb)

        // Motion estimation for the current macroblock
        writeSE(out, params.mv[0] - mvpx) // mvdx
        writeSE(out, params.mv[1] - mvpy) // mvdy
        val mbRef = Picture.create(16, 16, sps.chromaFormatIdc)
        val mb = arrayOf(IntArray(256), IntArray(64), IntArray(64))
        interpolator.getBlockLuma(ref!!, mbRef, 0, (mbX shl 6) + params.mv[0], (mbY shl 6) + params.mv[1], 16, 16)
        getBlockChroma(ref.getPlaneData(1), ref.getPlaneWidth(1), ref.getPlaneHeight(1),
                mbRef.getPlaneData(1), 0, mbRef.getPlaneWidth(1), (mbX shl 6) + params.mv[0], (mbY shl 6) + params.mv[1], 8, 8)
        getBlockChroma(ref.getPlaneData(2), ref.getPlaneWidth(2), ref.getPlaneHeight(2),
                mbRef.getPlaneData(2), 0, mbRef.getPlaneWidth(2), (mbX shl 6) + params.mv[0], (mbY shl 6) + params.mv[1], 8, 8)
        MBEncoderHelper.takeSubtract(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX shl 4,
                mbY shl 4, mb[0], mbRef.getPlaneData(0), 16, 16)
        MBEncoderHelper.takeSubtract(pic.getPlaneData(1), pic.getPlaneWidth(1), pic.getPlaneHeight(1), mbX shl 3,
                mbY shl 3, mb[1], mbRef.getPlaneData(1), 8, 8)
        MBEncoderHelper.takeSubtract(pic.getPlaneData(2), pic.getPlaneWidth(2), pic.getPlaneHeight(2), mbX shl 3,
                mbY shl 3, mb[2], mbRef.getPlaneData(2), 8, 8)
        val codedBlockPattern = codedBlockPattern
        writeUE(out, H264Const.CODED_BLOCK_PATTERN_INTER_COLOR_INV[codedBlockPattern])
        writeSE(out, qp - ctx.prevQp)
        luma(ctx, pic, mb[0], mbX, mbY, out, qp, outMB.nc)
        chroma(ctx, pic, mb[1], mb[2], mbX, mbY, out, qp)
        MBEncoderHelper.putBlk(outMB.pixels.getPlaneData(0), mb[0], mbRef.getPlaneData(0), 4, 0, 0, 16, 16)
        MBEncoderHelper.putBlk(outMB.pixels.getPlaneData(1), mb[1], mbRef.getPlaneData(1), 3, 0, 0, 8, 8)
        MBEncoderHelper.putBlk(outMB.pixels.getPlaneData(2), mb[2], mbRef.getPlaneData(2), 3, 0, 0, 8, 8)
        Arrays.fill(outMB.mx, params.mv[0])
        Arrays.fill(outMB.my, params.mv[1])
        Arrays.fill(outMB.mr, refIdx)
        outMB.type = MBType.P_16x16
        outMB.qp = qp
        ctx.prevQp = qp
    }

    private val codedBlockPattern: Int
        private get() = 47

    /**
     * Decides which reference to use
     *
     * @return
     */
    private fun decideRef(): Int {
        return 0
    }

    private fun luma(ctx: EncodingContext, pic: Picture, pix: IntArray, mbX: Int, mbY: Int, out: BitWriter, qp: Int, nc: IntArray) {
        val ac = Array(16) { IntArray(16) }
        for (i in ac.indices) {
            for (j in 0 until H264Const.PIX_MAP_SPLIT_4x4[i].size) {
                ac[i][j] = pix[H264Const.PIX_MAP_SPLIT_4x4[i][j]]
            }
            fdct4x4(ac[i])
        }
        writeAC(ctx, 0, mbX, mbY, out, mbX shl 2, mbY shl 2, ac, qp, nc)
        for (i in ac.indices) {
            dequantizeAC(ac[i], qp, null)
            idct4x4(ac[i])
            for (j in 0 until H264Const.PIX_MAP_SPLIT_4x4[i].size) pix[H264Const.PIX_MAP_SPLIT_4x4[i][j]] = ac[i][j]
        }
    }

    private fun chroma(ctx: EncodingContext, pic: Picture, pix1: IntArray, pix2: IntArray, mbX: Int, mbY: Int, out: BitWriter,
                       qp: Int) {
        val ac1 = Array(4) { IntArray(16) }
        val ac2 = Array(4) { IntArray(16) }
        for (i in ac1.indices) {
            for (j in 0 until H264Const.PIX_MAP_SPLIT_2x2[i].size) ac1[i][j] = pix1[H264Const.PIX_MAP_SPLIT_2x2[i][j]]
        }
        for (i in ac2.indices) {
            for (j in 0 until H264Const.PIX_MAP_SPLIT_2x2[i].size) ac2[i][j] = pix2[H264Const.PIX_MAP_SPLIT_2x2[i][j]]
        }
        chromaResidual(mbX, mbY, out, qp, ac1, ac2, ctx.cavlc[1], ctx.cavlc[2], MBType.P_16x16, MBType.P_16x16)
        for (i in ac1.indices) {
            for (j in 0 until H264Const.PIX_MAP_SPLIT_2x2[i].size) pix1[H264Const.PIX_MAP_SPLIT_2x2[i][j]] = ac1[i][j]
        }
        for (i in ac2.indices) {
            for (j in 0 until H264Const.PIX_MAP_SPLIT_2x2[i].size) pix2[H264Const.PIX_MAP_SPLIT_2x2[i][j]] = ac2[i][j]
        }
    }

    private fun writeAC(ctx: EncodingContext, comp: Int, mbX: Int, mbY: Int, out: BitWriter, mbLeftBlk: Int, mbTopBlk: Int,
                        ac: Array<IntArray>, qp: Int, nc: IntArray) {
        for (i in ac.indices) {
            val blkI = H264Const.BLK_INV_MAP[i]
            quantizeAC(ac[blkI], qp)
            val coeffToken = ctx.cavlc[comp].writeACBlock(out, mbLeftBlk + H264Const.MB_BLK_OFF_LEFT[i], mbTopBlk + H264Const.MB_BLK_OFF_TOP[i], MBType.P_16x16,
                    MBType.P_16x16, ac[blkI], H264Const.totalZeros16, 0, 16, CoeffTransformer.zigzag4x4)
            nc[blkI] = coeffToken shr 4 // total coeff
        }
    }

    init {
        interpolator = BlockInterpolator()
    }
}