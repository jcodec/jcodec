package org.jcodec.codecs.h264.encode;

import static org.jcodec.codecs.h264.H264Const.BLK_X;
import static org.jcodec.codecs.h264.H264Const.BLK_Y;
import static org.jcodec.codecs.h264.H264Const.MB_BLK_OFF_LEFT;
import static org.jcodec.codecs.h264.H264Const.MB_BLK_OFF_TOP;
import static org.jcodec.codecs.h264.decode.CoeffTransformer.reorderDC4x4;
import static org.jcodec.codecs.h264.encode.H264EncoderUtils.mse;
import static org.jcodec.codecs.h264.io.model.MBType.I_16x16;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.common.ArrayUtil;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Encodes macroblock as I16x16
 * 
 * @author Stanislav Vitvitskyy
 */
public class MBRDOI16x16 {

    private CAVLCRate[] cavlc;
    
    private byte[][] leftRow;
    private byte[][] topLine;
    private int lambda;

    public MBRDOI16x16(CAVLCRate[] cavlc, byte[][] leftRow, byte[][] topLine, int lambda) {
        this.cavlc = cavlc;
        this.lambda = lambda;
        this.leftRow = leftRow;
        this.topLine = topLine;
    }

    public int rateMacroblock(Picture pic, int mbX, int mbY, int qp, int qpDelta) {
        int rate = 0;
        rate += CAVLCRate.rateUE(0); // Chroma prediction mode -- DC
        rate += CAVLCRate.rateSE(qpDelta); // MB QP delta
        
        int[][] mb = new int[][] { new int[256], new int[64], new int[64] };
        int[][] mbo = new int[][] { new int[256], new int[64], new int[64] };

        rate += luma(pic, mbX, mbY, qp, mb, mbo, cavlc[0]);
        rate += chroma(pic, mbX, mbY, qp, mb, mbo);
        
        int dist = mse(mb[0], mbo[0], 16, 16);
        dist += mse(mb[1], mbo[1], 8, 8);
        dist += mse(mb[2], mbo[2], 8, 8);
        
        return rate + lambda * dist;
    }

    private static int DUMMY[] = new int[16];

    private int chroma(Picture pic, int mbX, int mbY, int qp, int[][] mb, int[][] mbo) {
        int x = mbX << 3;
        int y = mbY << 3;
        int[][] ac1 = new int[4][16];
        int[][] ac2 = new int[4][16];
        byte[][] pred1 = new byte[4][16];
        byte[][] pred2 = new byte[4][16];

        predictChroma(pic, ac1, pred1, 1,  x, y);
        predictChroma(pic, ac2, pred2, 2,  x, y);

        int rate = chromaResidual(mbX, mbY, qp, ac1, ac2, cavlc[1], cavlc[2], I_16x16, I_16x16);

        for (int i = 0; i < ac1.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                mbo[1][H264Const.PIX_MAP_SPLIT_2x2[i][j]] = ac1[i][j];
        }
        for (int i = 0; i < ac2.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                mbo[2][H264Const.PIX_MAP_SPLIT_2x2[i][j]] = ac2[i][j];
        }
        
        return rate;
    }

    public static int chromaResidual(int mbX, int mbY, int qp, int[][] ac1,
            int[][] ac2, CAVLCRate cavlc1, CAVLCRate cavlc2, MBType leftMBType, MBType topMBType) {
        int rate = 0;

        transformChroma(ac1);
        transformChroma(ac2);

        int[] dc1 = extractDC(ac1);
        int[] dc2 = extractDC(ac2);

        rate += rateDC(cavlc1, mbX, mbY, qp, mbX << 1, mbY << 1, dc1, leftMBType, topMBType);
        rate += rateDC(cavlc2, mbX, mbY, qp, mbX << 1, mbY << 1, dc2, leftMBType, topMBType);

        rate += rateAC(cavlc1, mbX, mbY, mbX << 1, mbY << 1, ac1, qp, leftMBType, topMBType, DUMMY);
        rate += rateAC(cavlc2, mbX, mbY, mbX << 1, mbY << 1, ac2, qp, leftMBType, topMBType, DUMMY);

        restorePlane(dc1, ac1, qp);
        restorePlane(dc2, ac2, qp);
        
        return rate;
    }

    private int luma(Picture pic, int mbX, int mbY, int qp, int[][] mb, int[][] mbo, CAVLCRate cavlc) {
        int x = mbX << 4;
        int y = mbY << 4;
        int[][] ac = new int[16][16];
        int[][] ac1 = new int[16][16];
        byte[][] pred = new byte[16][16];

        lumaDCPred(x, y, pred);
        transform(pic, 0, ac, pred, x, y);
        int[] dc = extractDC(ac);
        int rate = 0;
        rate += rateDC(cavlc, mbX, mbY, qp, mbX << 2, mbY << 2, dc, I_16x16, I_16x16);
        rate += rateAC(cavlc, mbX, mbY, mbX << 2, mbY << 2, ac, qp, I_16x16, I_16x16, DUMMY);

        restorePlane(dc, ac1, qp);

        for (int blk = 0; blk < ac.length; blk++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_4x4[blk].length; j++)
                mbo[0][H264Const.PIX_MAP_SPLIT_4x4[blk][j]] = ac1[blk][j];
        }
        return rate;
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
//            for(int j = 0; j < ac[i].length; j++)
//                ac[i][j] -= 128;
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

    private static int rateAC(CAVLCRate cavlc, int mbX, int mbY, int mbLeftBlk, int mbTopBlk, int[][] ac,
            int qp, MBType leftMBType, MBType topMBType, int[] nc) {
        int rate = 0;
        for (int i = 0; i < ac.length; i++) {
            CoeffTransformer.quantizeAC(ac[i], qp);
            rate += cavlc.rateACBlock(mbLeftBlk + MB_BLK_OFF_LEFT[i], mbTopBlk + MB_BLK_OFF_TOP[i], leftMBType,
                    topMBType, ac[i], H264Const.totalZeros16, 1, 15, CoeffTransformer.zigzag4x4);
        }
        return rate;
    }

    private static int rateDC(CAVLCRate cavlc, int mbX, int mbY, int qp, int mbLeftBlk, int mbTopBlk,
            int[] dc, MBType leftMBType, MBType topMBType) {
        if (dc.length == 4) {
            CoeffTransformer.quantizeDC2x2(dc, qp);
            CoeffTransformer.fvdDC2x2(dc);
            return cavlc.rateChrDCBlock(dc, H264Const.totalZeros4, 0, dc.length, new int[] { 0, 1, 2, 3 });
        } else if (dc.length == 8) {
            CoeffTransformer.quantizeDC4x2(dc, qp);
            CoeffTransformer.fvdDC4x2(dc);
            return cavlc.rateChrDCBlock(dc, H264Const.totalZeros8, 0, dc.length, new int[] { 0, 1, 2, 3, 4, 5, 6, 7 });
        } else {
            reorderDC4x4(dc);
            CoeffTransformer.quantizeDC4x4(dc, qp);
            CoeffTransformer.fvdDC4x4(dc);
            return cavlc.rateLumaDCBlock(mbLeftBlk, mbTopBlk, leftMBType, topMBType, dc, H264Const.totalZeros16, 0, 16,
                    CoeffTransformer.zigzag4x4);
        }
    }

    private static void transformChroma(int[][] ac) {
        for (int i = 0; i < 4; i++) {
            // shift back up
//            System.out.print("Chroma: ");
//            for (int j = 0; j < ac[i].length; j++) {
//                ac[i][j] += 128;
//                System.out.print(ac[i][j] + ",");
//            }
//            System.out.println();
            CoeffTransformer.fdct4x4(ac[i]);
        }
    }

    private void predictChroma(Picture pic, int[][] ac, byte[][] pred, int comp, int x, int y) {
        chromaPredBlk0(comp, x, y, pred[0]);
        chromaPredBlk1(comp, x, y, pred[1]);
        chromaPredBlk2(comp, x, y, pred[2]);
        chromaPredBlk3(comp, x, y, pred[3]);

        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x, y,
                ac[0], pred[0], 4, 4);

        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + 4,
                y, ac[1], pred[1], 4, 4);

        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x,
                y + 4, ac[2], pred[2], 4, 4);

        MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + 4,
                y + 4, ac[3], pred[3], 4, 4);
    }

    private final int chromaPredOne(byte[] pix, int x) {
        return (pix[x] + pix[x + 1] + pix[x + 2] + pix[x + 3] + 2) >> 2;
    }

    private final int chromaPredTwo(byte[] pix1, byte[] pix2, int x, int y) {
        return (pix1[x] + pix1[x + 1] + pix1[x + 2] + pix1[x + 3] + pix2[y] + pix2[y + 1] + pix2[y + 2] + pix2[y + 3] + 4) >> 3;
    }

    private void chromaPredBlk0(int comp, int x, int y, byte[] pred) {
        int dc, predY = y & 0x7;
        if (x != 0 && y != 0)
            dc = chromaPredTwo(leftRow[comp], topLine[comp], predY, x);
        else if (x != 0)
            dc = chromaPredOne(leftRow[comp], predY);
        else if (y != 0)
            dc = chromaPredOne(topLine[comp], x);
        else
            dc = 0;
        for (int i = 0; i < pred.length; i++)
            pred[i] += dc;
    }

    private void chromaPredBlk1(int comp, int x, int y, byte[] pred) {
        int dc, predY = y & 0x7;
        if (y != 0)
            dc = chromaPredOne(topLine[comp], x + 4);
        else if (x != 0)
            dc = chromaPredOne(leftRow[comp], predY);
        else
            dc = 0;
        for (int i = 0; i < pred.length; i++)
            pred[i] += dc;
    }

    private void chromaPredBlk2(int comp, int x, int y, byte[] pred) {
        int dc, predY = y & 0x7;
        if (x != 0)
            dc = chromaPredOne(leftRow[comp], predY + 4);
        else if (y != 0)
            dc = chromaPredOne(topLine[comp], x);
        else
            dc = 0;
        for (int i = 0; i < pred.length; i++)
            pred[i] += dc;
    }

    private void chromaPredBlk3(int comp, int x, int y, byte[] pred) {
        int dc, predY = y & 0x7;
        if (x != 0 && y != 0)
            dc = chromaPredTwo(leftRow[comp], topLine[comp], predY + 4, x + 4);
        else if (x != 0)
            dc = chromaPredOne(leftRow[comp], predY + 4);
        else if (y != 0)
            dc = chromaPredOne(topLine[comp], x + 4);
        else
            dc = 0;
        for (int i = 0; i < pred.length; i++)
            pred[i] += dc;
    }

    private void lumaDCPred(int x, int y, byte[][] pred) {
        int dc;
        if (x == 0 && y == 0)
            dc = 0;
        else if (y == 0)
            dc = (ArrayUtil.sumByte(leftRow[0]) + 8) >> 4;
        else if (x == 0)
            dc = (ArrayUtil.sumByte3(topLine[0], x, 16) + 8) >> 4;
        else
            dc = (ArrayUtil.sumByte(leftRow[0]) + ArrayUtil.sumByte3(topLine[0], x, 16) + 16) >> 5;

        for (int i = 0; i < pred.length; i++)
            for (int j = 0; j < pred[i].length; j++)
                pred[i][j] += dc;
    }

    private void transform(Picture pic, int comp, int[][] ac, byte[][] pred, int x, int y) {
        for (int i = 0; i < ac.length; i++) {
            int[] coeff = ac[i];
            MBEncoderHelper.takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x
                    + BLK_X[i], y + BLK_Y[i], coeff, pred[i], 4, 4);
            // shift back up
//            System.out.print("Luma: ");
//            for (int j = 0; j < coeff.length; j++) {
//                coeff[j] += 128;
//                System.out.print(coeff[j] + ",");
//            }
//            System.out.println();
            CoeffTransformer.fdct4x4(coeff);
        }
    }

    public int getPredMode(Picture pic, int mbX, int mbY) {
        return 2;
    }

    public int getCbpChroma(Picture pic, int mbX, int mbY) {
        return 2;
    }

    public int getCbpLuma(Picture pic, int mbX, int mbY) {
        return 15;
    }
}
