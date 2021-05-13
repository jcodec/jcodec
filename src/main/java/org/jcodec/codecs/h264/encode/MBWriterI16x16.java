package org.jcodec.codecs.h264.encode;

import static org.jcodec.codecs.h264.H264Const.BLK_DISP_MAP;
import static org.jcodec.codecs.h264.H264Const.BLK_X;
import static org.jcodec.codecs.h264.H264Const.BLK_Y;
import static org.jcodec.codecs.h264.H264Const.MB_DISP_OFF_LEFT;
import static org.jcodec.codecs.h264.H264Const.MB_DISP_OFF_TOP;
import static org.jcodec.codecs.h264.H264Const.QP_SCALE_CR;
import static org.jcodec.codecs.h264.decode.CoeffTransformer.reorderDC4x4;
import static org.jcodec.codecs.h264.io.model.MBType.I_16x16;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Encoder.NonRdVector;
import org.jcodec.codecs.h264.decode.ChromaPredictionBuilder;
import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.jcodec.codecs.h264.decode.Intra16x16PredictionBuilder;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.write.CAVLCWriter;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Encodes macroblock as I16x16
 * 
 * @author Stanislav Vitvitskyy
 */
public class MBWriterI16x16 {
    public boolean encodeMacroblock(EncodingContext ctx, Picture pic, int mbX, int mbY, BitWriter out, EncodedMB outMB,
            int qp, NonRdVector params) {
        CAVLCWriter.writeUE(out, params.chrPred);
        CAVLCWriter.writeSE(out, qp - ctx.prevQp); // MB QP delta

        outMB.setType(MBType.I_16x16);
        outMB.setQp(qp);

        boolean cbp = false;
        int[] nc = new int[16];
        luma(ctx, pic, mbX, mbY, out, qp, outMB.getPixels(), params.lumaPred16x16, nc);
        for (int dInd = 0; dInd < 16; dInd++) {
            cbp |= nc[dInd] != 0;
        }
        chroma(ctx, pic, mbX, mbY, I_16x16, out, qp, outMB.getPixels(), params.chrPred);
        ctx.prevQp = qp;
        return cbp;
    }

    private static int DUMMY[] = new int[16];

    static int calcQpChroma(int qp, int crQpOffset) {
        return QP_SCALE_CR[MathUtil.clip(qp + crQpOffset, 0, 51)];
    }

    public static void chroma(EncodingContext ctx, Picture pic, int mbX, int mbY, MBType curMBType, BitWriter out,
            int qp, Picture outMB, int chrPred) {
        int x = mbX << 3;
        int y = mbY << 3;
        int[][] ac1 = new int[4][16];
        int[][] ac2 = new int[4][16];
        byte[][] pred1 = new byte[4][16];
        byte[][] pred2 = new byte[4][16];

        predictChroma(ctx, pic, ac1, pred1, 1, x, y, chrPred);
        predictChroma(ctx, pic, ac2, pred2, 2, x, y, chrPred);

        chromaResidual(mbX, mbY, out, qp, ac1, ac2, ctx.cavlc[1], ctx.cavlc[2], ctx.leftMBType, ctx.topMBType[mbX],
                curMBType);

        putChroma(outMB.getData()[1], 1, x, y, ac1, pred1);
        putChroma(outMB.getData()[2], 2, x, y, ac2, pred2);
    }

    public static void chromaResidual(int mbX, int mbY, BitWriter out, int qp, int[][] ac1, int[][] ac2, CAVLC cavlc1,
            CAVLC cavlc2, MBType leftMBType, MBType topMBType, MBType curMBType) {
        int crQpOffset = 0;
        int chrQp = calcQpChroma(qp, crQpOffset);

        transformChroma(ac1);
        transformChroma(ac2);

        int[] dc1 = extractDC(ac1);
        int[] dc2 = extractDC(ac2);

        writeDC(cavlc1, mbX, mbY, out, chrQp, mbX << 1, mbY << 1, dc1, leftMBType, topMBType);
        writeDC(cavlc2, mbX, mbY, out, chrQp, mbX << 1, mbY << 1, dc2, leftMBType, topMBType);

        writeAC(cavlc1, mbX, mbY, out, mbX << 1, mbY << 1, ac1, chrQp, leftMBType, topMBType, curMBType, DUMMY);
        writeAC(cavlc2, mbX, mbY, out, mbX << 1, mbY << 1, ac2, chrQp, leftMBType, topMBType, curMBType, DUMMY);

        restorePlane(dc1, ac1, chrQp);
        restorePlane(dc2, ac2, chrQp);
    }

    private void luma(EncodingContext ctx, Picture pic, int mbX, int mbY, BitWriter out, int qp, Picture outMB,
            int predType, int[] nc) {
        int x = mbX << 4;
        int y = mbY << 4;
        int[][] ac = new int[16][16];
        byte[][] pred = new byte[16][16];

        Intra16x16PredictionBuilder.lumaPred(predType, x != 0, y != 0, ctx.leftRow[0], ctx.topLine[0], ctx.topLeft[0],
                x, pred);

        transform(pic, 0, ac, pred, x, y);
        int[] dc = extractDC(ac);
        writeDC(ctx.cavlc[0], mbX, mbY, out, qp, mbX << 2, mbY << 2, dc, ctx.leftMBType, ctx.topMBType[mbX]);
        writeACLum(ctx.cavlc[0], mbX, mbY, out, mbX << 2, mbY << 2, ac, qp, ctx.leftMBType, ctx.topMBType[mbX], I_16x16,
                nc);

        restorePlane(dc, ac, qp);

        for (int blk = 0; blk < ac.length; blk++) {
            MBEncoderHelper.putBlk(outMB.getPlaneData(0), ac[blk], pred[blk], 4, BLK_X[blk], BLK_Y[blk], 4, 4);
        }
    }

    private static void putChroma(byte[] mb, int comp, int x, int y, int[][] ac, byte[][] pred) {
        MBEncoderHelper.putBlk(mb, ac[0], pred[0], 3, 0, 0, 4, 4);

        MBEncoderHelper.putBlk(mb, ac[1], pred[1], 3, 4, 0, 4, 4);

        MBEncoderHelper.putBlk(mb, ac[2], pred[2], 3, 0, 4, 4, 4);

        MBEncoderHelper.putBlk(mb, ac[3], pred[3], 3, 4, 4, 4, 4);
    }

    private static void restorePlane(int[] dc, int[][] ac, int qp) {
        if (dc.length == 4) {
            CoeffTransformer.invDC2x2(dc);
            CoeffTransformer.dequantizeDC2x2(dc, qp, null);
        } else if (dc.length == 8) {
            CoeffTransformer.invDC4x2(dc);
            CoeffTransformer.dequantizeDC4x2(dc, qp);
        } else {
            CoeffTransformer.invDC4x4(dc);
            CoeffTransformer.dequantizeDC4x4(dc, qp, null);
            reorderDC4x4(dc);
        }
        for (int i = 0; i < ac.length; i++) {
            CoeffTransformer.dequantizeAC(ac[i], qp, null);
            ac[i][0] = dc[i];
            CoeffTransformer.idct4x4(ac[i]);
        }
    }

    private static int[] extractDC(int[][] ac) {
        int[] dc = new int[ac.length];
        for (int i = 0; i < ac.length; i++) {
            dc[i] = ac[i][0];
            ac[i][0] = 0;
        }
        return dc;
    }

    private static void writeAC(CAVLC cavlc, int mbX, int mbY, BitWriter out, int mbLeftBlk, int mbTopBlk, int[][] ac,
            int qp, MBType leftMBType, MBType topMBType, MBType curMBType, int[] nc) {
        for (int bInd = 0; bInd < ac.length; bInd++) {
            CoeffTransformer.quantizeAC(ac[bInd], qp);
            int blkOffLeft = MB_DISP_OFF_LEFT[bInd];
            int blkOffTop = MB_DISP_OFF_TOP[bInd];
            nc[BLK_DISP_MAP[bInd]] = CAVLC
                    .totalCoeff(cavlc.writeACBlock(out, mbLeftBlk + blkOffLeft, mbTopBlk + blkOffTop,
                            blkOffLeft == 0 ? leftMBType : curMBType, blkOffTop == 0 ? topMBType : curMBType, ac[bInd],
                            H264Const.totalZeros16, 1, 15, CoeffTransformer.zigzag4x4));
        }
    }

    private static void writeACLum(CAVLC cavlc, int mbX, int mbY, BitWriter out, int mbLeftBlk, int mbTopBlk,
            int[][] ac, int qp, MBType leftMBType, MBType topMBType, MBType curMBType, int[] nc) {
        boolean code = false;
        for (int bInd = 0; bInd < 16; bInd++) {
            CoeffTransformer.quantizeAC(ac[bInd], qp);
            code |= hasNz(ac[bInd]);
        }
        if (code) {
            for (int bInd = 0; bInd < 16; bInd++) {
                int blkOffLeft = MB_DISP_OFF_LEFT[bInd];
                int blkOffTop = MB_DISP_OFF_TOP[bInd];
                nc[BLK_DISP_MAP[bInd]] = CAVLC
                        .totalCoeff(cavlc.writeACBlock(out, mbLeftBlk + blkOffLeft, mbTopBlk + blkOffTop,
                                blkOffLeft == 0 ? leftMBType : curMBType, blkOffTop == 0 ? topMBType : curMBType,
                                ac[bInd], H264Const.totalZeros16, 1, 15, CoeffTransformer.zigzag4x4));
            }
        } else {
            for (int bInd = 0; bInd < 16; bInd++) {
                int blkOffLeft = MB_DISP_OFF_LEFT[bInd];
                int blkOffTop = MB_DISP_OFF_TOP[bInd];
                cavlc.setZeroCoeff(mbLeftBlk + blkOffLeft, mbTopBlk + blkOffTop);
            }
        }
    }

    public static boolean hasNz(int[] ac) {
        int val = 0;
        for (int i = 0; i < 16; i++)
            val |= ac[i];
        return val != 0;
    }

    private static void writeDC(CAVLC cavlc, int mbX, int mbY, BitWriter out, int qp, int mbLeftBlk, int mbTopBlk,
            int[] dc, MBType leftMBType, MBType topMBType) {
        if (dc.length == 4) {
            CoeffTransformer.quantizeDC2x2(dc, qp);
            CoeffTransformer.fvdDC2x2(dc);
            cavlc.writeChrDCBlock(out, dc, H264Const.totalZeros4, 0, dc.length, new int[] { 0, 1, 2, 3 });
        } else if (dc.length == 8) {
            CoeffTransformer.quantizeDC4x2(dc, qp);
            CoeffTransformer.fvdDC4x2(dc);
            cavlc.writeChrDCBlock(out, dc, H264Const.totalZeros8, 0, dc.length, new int[] { 0, 1, 2, 3, 4, 5, 6, 7 });
        } else {
            reorderDC4x4(dc);
            CoeffTransformer.quantizeDC4x4(dc, qp);
            CoeffTransformer.fvdDC4x4(dc);
            // TODO: calc here
            cavlc.writeLumaDCBlock(out, mbLeftBlk, mbTopBlk, leftMBType, topMBType, dc, H264Const.totalZeros16, 0, 16,
                    CoeffTransformer.zigzag4x4);
        }
    }

    private static void transformChroma(int[][] ac) {
        for (int i = 0; i < 4; i++) {
            CoeffTransformer.fdct4x4(ac[i]);
        }
    }

    private static void predictChroma(EncodingContext ctx, Picture pic, int[][] ac, byte[][] pred, int comp, int x,
            int y, int mode) {
        ChromaPredictionBuilder.buildPred(mode, x >> 3, x != 0, y != 0, ctx.leftRow[comp], ctx.topLine[comp], ctx.topLeft[comp], pred);

        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x, y,
                ac[0], pred[0], 4, 4);

        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + 4,
                y, ac[1], pred[1], 4, 4);

        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x,
                y + 4, ac[2], pred[2], 4, 4);

        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + 4,
                y + 4, ac[3], pred[3], 4, 4);
    }

    private void transform(Picture pic, int comp, int[][] ac, byte[][] pred, int x, int y) {
        for (int i = 0; i < ac.length; i++) {
            int[] coeff = ac[i];
            MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp),
                    x + BLK_X[i], y + BLK_Y[i], coeff, pred[i], 4, 4);
            CoeffTransformer.fdct4x4(coeff);
        }
    }

    public int getCbpChroma(Picture pic, int mbX, int mbY) {
        return 2;
    }
}
