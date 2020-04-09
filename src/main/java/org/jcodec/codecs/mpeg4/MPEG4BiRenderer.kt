package org.jcodec.codecs.mpeg4

import org.jcodec.common.model.Picture

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object MPEG4BiRenderer {
    @JvmStatic
    fun renderBi(ctx: MPEG4DecodingContext, refs: Array<Picture>, fcodeForward: Int, fcodeBackward: Int,
                 mb: Macroblock) {
        when (mb.mode) {
            MPEG4Consts.MODE_DIRECT, MPEG4Consts.MODE_DIRECT_NONE_MV -> renderBiDir(ctx, refs, mb, true)
            MPEG4Consts.MODE_INTERPOLATE -> renderBiDir(ctx, refs, mb, false)
            MPEG4Consts.MODE_BACKWARD -> MPEG4Renderer.renderInter(ctx, refs, mb, fcodeBackward, 0, true)
            MPEG4Consts.MODE_FORWARD -> MPEG4Renderer.renderInter(ctx, refs, mb, fcodeForward, 1, true)
        }
    }

    private fun renderBiDir(ctx: MPEG4DecodingContext, refs: Array<Picture>, mb: Macroblock, direct: Boolean) {
        val cbp = mb.cbp
        MPEG4Renderer.validateVector(mb.mvs, ctx, mb.x, mb.y)
        MPEG4Renderer.validateVector(mb.bmvs, ctx, mb.x, mb.y)
        renderOneDir(ctx, mb, direct, refs[1], mb.mvs, 0)
        renderOneDir(ctx, mb, direct, refs[0], mb.bmvs, 3)
        mergePred(mb)
        if (cbp != 0) {
            for (i in 0..5) {
                val block = mb.block[i]
                if (mb.cbp and (1 shl 5 - i) != 0) {
                    MPEG4DCT.idctAdd(mb.pred, block, i, ctx.interlacing && mb.fieldDCT)
                }
            }
        }
    }

    private fun mergePred(mb: Macroblock) {
        for (i in 0..255) {
            mb.pred[0][i] = (mb.pred[0][i] + mb.pred[3][i] + 1 shr 1).toByte()
        }
        for (pl in 1..2) {
            for (i in 0..63) {
                mb.pred[pl][i] = (mb.pred[pl][i] + mb.pred[pl + 3][i] + 1 shr 1).toByte()
            }
        }
    }

    private fun renderOneDir(ctx: MPEG4DecodingContext, mb: Macroblock, direct: Boolean, forward: Picture,
                             mvs: Array<Macroblock.Vector>, pred: Int) {
        val mbX = 16 * mb.x
        val mbY = 16 * mb.y
        val codedW = ctx.mbWidth shl 4
        val codedH = ctx.mbHeight shl 4
        val codedWcr = ctx.mbWidth shl 3
        val codedHcr = ctx.mbHeight shl 3
        if (ctx.quarterPel) {
            if (!direct) {
                MPEG4Interpolator.interpolate16x16QP(mb.pred[pred], forward.getPlaneData(0), mbX, mbY, codedW, codedH, mvs[0].x, mvs[0].y,
                        forward.width, false)
            } else {
                MPEG4Interpolator.interpolate8x8QP(mb.pred[pred], 0, forward.getPlaneData(0), mbX, mbY, codedW, codedH, mvs[0].x,
                        mvs[0].y, forward.width, false)
                MPEG4Interpolator.interpolate8x8QP(mb.pred[pred], 8, forward.getPlaneData(0), mbX + 8, mbY, codedW, codedH, mvs[1].x,
                        mvs[1].y, forward.width, false)
                MPEG4Interpolator.interpolate8x8QP(mb.pred[pred], 128, forward.getPlaneData(0), mbX, mbY + 8, codedW, codedH, mvs[2].x,
                        mvs[2].y, forward.width, false)
                MPEG4Interpolator.interpolate8x8QP(mb.pred[pred], 136, forward.getPlaneData(0), mbX + 8, mbY + 8, codedW, codedH,
                        mvs[3].x, mvs[3].y, forward.width, false)
            }
        } else {
            MPEG4Interpolator.interpolate8x8Planar(mb.pred[pred], 0, 16, forward.getPlaneData(0), mbX, mbY, codedW, codedH, mvs[0].x,
                    mvs[0].y, forward.width, false)
            MPEG4Interpolator.interpolate8x8Planar(mb.pred[pred], 8, 16, forward.getPlaneData(0), mbX + 8, mbY, codedW, codedH, mvs[1].x,
                    mvs[1].y, forward.width, false)
            MPEG4Interpolator.interpolate8x8Planar(mb.pred[pred], 128, 16, forward.getPlaneData(0), mbX, mbY + 8, codedW, codedH,
                    mvs[2].x, mvs[2].y, forward.width, false)
            MPEG4Interpolator.interpolate8x8Planar(mb.pred[pred], 136, 16, forward.getPlaneData(0), mbX + 8, mbY + 8, codedW, codedH,
                    mvs[3].x, mvs[3].y, forward.width, false)
        }
        val mx_chr: Int
        val my_chr: Int
        if (!direct) {
            mx_chr = MPEG4Renderer.calcChromaMv(ctx, mvs[0].x)
            my_chr = MPEG4Renderer.calcChromaMv(ctx, mvs[0].y)
        } else {
            mx_chr = MPEG4Renderer.calcChromaMvAvg(ctx, mvs, true)
            my_chr = MPEG4Renderer.calcChromaMvAvg(ctx, mvs, false)
        }
        MPEG4Interpolator.interpolate8x8Planar(mb.pred[pred + 1], 0, 8, forward.getPlaneData(1), 8 * mb.x, 8 * mb.y, codedWcr, codedHcr,
                mx_chr, my_chr, forward.getPlaneWidth(1), false)
        MPEG4Interpolator.interpolate8x8Planar(mb.pred[pred + 2], 0, 8, forward.getPlaneData(2), 8 * mb.x, 8 * mb.y, codedWcr, codedHcr,
                mx_chr, my_chr, forward.getPlaneWidth(2), false)
    }
}