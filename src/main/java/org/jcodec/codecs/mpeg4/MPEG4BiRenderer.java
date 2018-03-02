package org.jcodec.codecs.mpeg4;

import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_BACKWARD;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_DIRECT;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_DIRECT_NONE_MV;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_FORWARD;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_INTERPOLATE;
import static org.jcodec.codecs.mpeg4.MPEG4DCT.idctAdd;
import static org.jcodec.codecs.mpeg4.MPEG4Interpolator.interpolate16x16QP;
import static org.jcodec.codecs.mpeg4.MPEG4Interpolator.interpolate8x8Planar;
import static org.jcodec.codecs.mpeg4.MPEG4Interpolator.interpolate8x8QP;
import static org.jcodec.codecs.mpeg4.MPEG4Renderer.calcChromaMv;
import static org.jcodec.codecs.mpeg4.MPEG4Renderer.calcChromaMvAvg;
import static org.jcodec.codecs.mpeg4.MPEG4Renderer.renderInter;
import static org.jcodec.codecs.mpeg4.MPEG4Renderer.validateVector;

import org.jcodec.codecs.mpeg4.Macroblock.Vector;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MPEG4BiRenderer {

    public static void renderBi(MPEG4DecodingContext ctx, Picture[] refs, int fcodeForward, int fcodeBackward,
            Macroblock mb) {
        switch (mb.mode) {
        case MODE_DIRECT:
        case MODE_DIRECT_NONE_MV:
            renderBiDir(ctx, refs, mb, true);
            break;

        case MODE_INTERPOLATE:
            renderBiDir(ctx, refs, mb, false);
            break;

        case MODE_BACKWARD:
            renderInter(ctx, refs, mb, fcodeBackward, 0, true);
            break;

        case MODE_FORWARD:
            renderInter(ctx, refs, mb, fcodeForward, 1, true);
            break;
        default:
        }
    }

    private static void renderBiDir(MPEG4DecodingContext ctx, Picture[] refs, Macroblock mb, boolean direct) {
        final int cbp = mb.cbp;

        validateVector(mb.mvs, ctx, mb.x, mb.y);
        validateVector(mb.bmvs, ctx, mb.x, mb.y);

        renderOneDir(ctx, mb, direct, refs[1], mb.mvs, 0);
        renderOneDir(ctx, mb, direct, refs[0], mb.bmvs, 3);

        mergePred(mb);

        if (cbp != 0) {
            for (int i = 0; i < 6; i++) {
                short[] block = mb.block[i];
                if ((mb.cbp & (1 << (5 - i))) != 0) {
                    idctAdd(mb.pred, block, i, ctx.interlacing && mb.fieldDCT);
                }
            }
        }
    }

    private static void mergePred(Macroblock mb) {
        for (int i = 0; i < 256; i++) {
            mb.pred[0][i] = (byte) ((mb.pred[0][i] + mb.pred[3][i] + 1) >> 1);
        }
        for (int pl = 1; pl < 3; pl++) {
            for (int i = 0; i < 64; i++) {
                mb.pred[pl][i] = (byte) ((mb.pred[pl][i] + mb.pred[pl + 3][i] + 1) >> 1);
            }
        }
    }

    private static void renderOneDir(MPEG4DecodingContext ctx, Macroblock mb, boolean direct, Picture forward,
            Vector[] mvs, int pred) {
        int mbX = 16 * mb.x;
        int mbY = 16 * mb.y;
        int codedW = ctx.mbWidth << 4;
        int codedH = ctx.mbHeight << 4;
        int codedWcr = ctx.mbWidth << 3;
        int codedHcr = ctx.mbHeight << 3;

        if (ctx.quarterPel) {
            if (!direct) {
                interpolate16x16QP(mb.pred[pred], forward.getPlaneData(0), mbX, mbY, codedW, codedH, mvs[0].x, mvs[0].y,
                        forward.getWidth(), false);
            } else {
                interpolate8x8QP(mb.pred[pred], 0, forward.getPlaneData(0), mbX, mbY, codedW, codedH, mvs[0].x,
                        mvs[0].y, forward.getWidth(), false);
                interpolate8x8QP(mb.pred[pred], 8, forward.getPlaneData(0), mbX + 8, mbY, codedW, codedH, mvs[1].x,
                        mvs[1].y, forward.getWidth(), false);
                interpolate8x8QP(mb.pred[pred], 128, forward.getPlaneData(0), mbX, mbY + 8, codedW, codedH, mvs[2].x,
                        mvs[2].y, forward.getWidth(), false);
                interpolate8x8QP(mb.pred[pred], 136, forward.getPlaneData(0), mbX + 8, mbY + 8, codedW, codedH,
                        mvs[3].x, mvs[3].y, forward.getWidth(), false);
            }
        } else {
            interpolate8x8Planar(mb.pred[pred], 0, 16, forward.getPlaneData(0), mbX, mbY, codedW, codedH, mvs[0].x,
                    mvs[0].y, forward.getWidth(), false);
            interpolate8x8Planar(mb.pred[pred], 8, 16, forward.getPlaneData(0), mbX + 8, mbY, codedW, codedH, mvs[1].x,
                    mvs[1].y, forward.getWidth(), false);
            interpolate8x8Planar(mb.pred[pred], 128, 16, forward.getPlaneData(0), mbX, mbY + 8, codedW, codedH,
                    mvs[2].x, mvs[2].y, forward.getWidth(), false);
            interpolate8x8Planar(mb.pred[pred], 136, 16, forward.getPlaneData(0), mbX + 8, mbY + 8, codedW, codedH,
                    mvs[3].x, mvs[3].y, forward.getWidth(), false);
        }

        int mx_chr, my_chr;
        if (!direct) {
            mx_chr = calcChromaMv(ctx, mvs[0].x);
            my_chr = calcChromaMv(ctx, mvs[0].y);
        } else {
            mx_chr = calcChromaMvAvg(ctx, mvs, true);
            my_chr = calcChromaMvAvg(ctx, mvs, false);
        }
        interpolate8x8Planar(mb.pred[pred + 1], 0, 8, forward.getPlaneData(1), 8 * mb.x, 8 * mb.y, codedWcr, codedHcr,
                mx_chr, my_chr, forward.getPlaneWidth(1), false);
        interpolate8x8Planar(mb.pred[pred + 2], 0, 8, forward.getPlaneData(2), 8 * mb.x, 8 * mb.y, codedWcr, codedHcr,
                mx_chr, my_chr, forward.getPlaneWidth(2), false);
    }
}
