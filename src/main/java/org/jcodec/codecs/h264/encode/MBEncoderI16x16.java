package org.jcodec.codecs.h264.encode;

import static org.jcodec.codecs.h264.H264Const.BLK_X;
import static org.jcodec.codecs.h264.H264Const.BLK_Y;
import static org.jcodec.codecs.h264.H264Const.MB_BLK_OFF_LEFT;
import static org.jcodec.codecs.h264.H264Const.MB_BLK_OFF_TOP;
import static org.jcodec.codecs.h264.decode.CoeffTransformer.reorderDC4x4;
import static org.jcodec.codecs.h264.io.model.MBType.I_16x16;
import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.write.CAVLCWriter;
import org.jcodec.common.ArrayUtil;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.model.Picture;

public class MBEncoderI16x16 {
    
    private CAVLC[] cavlc;
    private int[][] leftRow;
    private int[][] topLine;
    
    
    public MBEncoderI16x16(CAVLC[] cavlc, int[][] leftRow, int[][] topLine) {
        this.cavlc = cavlc;
        this.leftRow = leftRow;
        this.topLine = topLine;
    }

    public void encodeMacroblock(Picture pic, int mbX, int mbY, BitWriter out, Picture outMB, int qp, int qpDelta) {
        CAVLCWriter.writeUE(out, 0); // Chroma prediction mode -- DC
        CAVLCWriter.writeSE(out, qpDelta); // MB QP delta

        luma(pic, mbX, mbY, out, qp, outMB);
        chroma(pic, mbX, mbY, out, qp, outMB);
    }

    private void chroma(Picture pic, int mbX, int mbY, BitWriter out, int qp, Picture outMB) {
        int cw = pic.getColor().compWidth[1];
        int ch = pic.getColor().compHeight[1];
        int x = mbX << (4 - cw);
        int y = mbY << (4 - ch);
        int[][] ac1 = transformChroma(pic, 1, qp, cw, ch, x, y, outMB);
        int[][] ac2 = transformChroma(pic, 2, qp, cw, ch, x, y, outMB);
        int[] dc1 = extractDC(ac1);
        int[] dc2 = extractDC(ac2);

        writeDC(1, mbX, mbY, out, qp, mbX << 1, mbY << 1, dc1);
        writeDC(2, mbX, mbY, out, qp, mbX << 1, mbY << 1, dc2);

        writeAC(1, mbX, mbY, out, mbX << 1, mbY << 1, ac1, qp);
        writeAC(2, mbX, mbY, out, mbX << 1, mbY << 1, ac2, qp);

        restorePlane(dc1, ac1, qp);
        putChroma(outMB.getData()[1], 1, x, y, ac1);
        restorePlane(dc2, ac2, qp);
        putChroma(outMB.getData()[2], 2, x, y, ac2);
    }

    private void luma(Picture pic, int mbX, int mbY, BitWriter out, int qp, Picture outMB) {
        int x = mbX << 4;
        int y = mbY << 4;
        int[][] ac = transform(pic, 0, qp, 0, 0, x, y);
        int[] dc = extractDC(ac);
        writeDC(0, mbX, mbY, out, qp, mbX << 2, mbY << 2, dc);
        writeAC(0, mbX, mbY, out, mbX << 2, mbY << 2, ac, qp);

        restorePlane(dc, ac, qp);
        putLuma(outMB.getPlaneData(0), lumaDCPred(x, y), ac, 4);
    }

    private void putChroma(int[] mb, int comp, int x, int y, int[][] ac) {
        putBlk(mb, chromaPredBlk0(comp, x, y), ac[0], 3, 0, 0);

        putBlk(mb, chromaPredBlk1(comp, x, y), ac[1], 3, 4, 0);

        putBlk(mb, chromaPredBlk2(comp, x, y), ac[2], 3, 0, 4);

        putBlk(mb, chromaPredBlk3(comp, x, y), ac[3], 3, 4, 4);
    }

    private void putLuma(int[] planeData, int pred, int[][] ac, int log2stride) {
        for (int blk = 0; blk < ac.length; blk++) {
            putBlk(planeData, pred, ac[blk], log2stride, BLK_X[blk], BLK_Y[blk]);
        }
    }

    private void putBlk(int[] planeData, int pred, int[] block, int log2stride, int blkX, int blkY) {
        int stride = 1 << log2stride;
        for (int line = 0, srcOff = 0, dstOff = (blkY << log2stride) + blkX; line < 4; line++) {
            planeData[dstOff] = clip(block[srcOff] + pred, 0, 255);
            planeData[dstOff + 1] = clip(block[srcOff + 1] + pred, 0, 255);
            planeData[dstOff + 2] = clip(block[srcOff + 2] + pred, 0, 255);
            planeData[dstOff + 3] = clip(block[srcOff + 3] + pred, 0, 255);
            srcOff += 4;
            dstOff += stride;
        }
    }

    private void restorePlane(int[] dc, int[][] ac, int qp) {
        if (dc.length == 4) {
            CoeffTransformer.invDC2x2(dc);
            CoeffTransformer.dequantizeDC2x2(dc, qp);
        } else if (dc.length == 8) {
            CoeffTransformer.invDC4x2(dc);
            CoeffTransformer.dequantizeDC4x2(dc, qp);
        } else {
            CoeffTransformer.invDC4x4(dc);
            CoeffTransformer.dequantizeDC4x4(dc, qp);
            reorderDC4x4(dc);
        }
        for (int i = 0; i < ac.length; i++) {
            CoeffTransformer.dequantizeAC(ac[i], qp);
            ac[i][0] = dc[i];
            CoeffTransformer.idct4x4(ac[i]);
        }
    }

    private int[] extractDC(int[][] ac) {
        int[] dc = new int[ac.length];
        for (int i = 0; i < ac.length; i++) {
            dc[i] = ac[i][0];
            ac[i][0] = 0;
        }
        return dc;
    }

    private void writeAC(int comp, int mbX, int mbY, BitWriter out, int mbLeftBlk, int mbTopBlk, int[][] ac, int qp) {
        for (int i = 0; i < ac.length; i++) {
            CoeffTransformer.quantizeAC(ac[i], qp);
            // TODO: calc here
            cavlc[comp].writeACBlock(out, mbLeftBlk + MB_BLK_OFF_LEFT[i], mbTopBlk + MB_BLK_OFF_TOP[i], I_16x16,
                    I_16x16, ac[i], H264Const.totalZeros16, 1, 15, CoeffTransformer.zigzag4x4);
        }
    }

    private void writeDC(int comp, int mbX, int mbY, BitWriter out, int qp, int mbLeftBlk, int mbTopBlk, int[] dc) {
        if (dc.length == 4) {
            CoeffTransformer.quantizeDC2x2(dc, qp);
            CoeffTransformer.fvdDC2x2(dc);
            cavlc[comp].writeChrDCBlock(out, dc, H264Const.totalZeros4, 0, dc.length, new int[] { 0, 1, 2, 3 });
        } else if (dc.length == 8) {
            CoeffTransformer.quantizeDC4x2(dc, qp);
            CoeffTransformer.fvdDC4x2(dc);
            cavlc[comp].writeChrDCBlock(out, dc, H264Const.totalZeros8, 0, dc.length, new int[] { 0, 1, 2, 3, 4, 5, 6,
                    7 });
        } else {
            reorderDC4x4(dc);
            CoeffTransformer.quantizeDC4x4(dc, qp);
            CoeffTransformer.fvdDC4x4(dc);
            // TODO: calc here
            cavlc[comp].writeLumaDCBlock(out, mbLeftBlk, mbTopBlk, I_16x16, I_16x16, dc, H264Const.totalZeros16, 0, 16,
                    CoeffTransformer.zigzag4x4);
        }
    }

    private int[][] transformChroma(Picture pic, int comp, int qp, int cw, int ch, int x, int y, Picture outMB) {
        int[][] ac = new int[16 >> (cw + ch)][16];

        takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x, y, ac[0],
                chromaPredBlk0(comp, x, y));
        CoeffTransformer.fdct4x4(ac[0]);

        takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + 4, y, ac[1],
                chromaPredBlk1(comp, x, y));
        CoeffTransformer.fdct4x4(ac[1]);

        takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x, y + 4, ac[2],
                chromaPredBlk2(comp, x, y));
        CoeffTransformer.fdct4x4(ac[2]);

        takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + 4, y + 4, ac[3],
                chromaPredBlk3(comp, x, y));
        CoeffTransformer.fdct4x4(ac[3]);

        return ac;
    }

    private final int chromaPredOne(int[] pix, int x) {
        return (pix[x] + pix[x + 1] + pix[x + 2] + pix[x + 3] + 2) >> 2;
    }

    private final int chromaPredTwo(int[] pix1, int[] pix2, int x, int y) {
        return (pix1[x] + pix1[x + 1] + pix1[x + 2] + pix1[x + 3] + pix2[y] + pix2[y + 1] + pix2[y + 2] + pix2[y + 3] + 4) >> 3;
    }

    private int chromaPredBlk0(int comp, int x, int y) {
        int predY = y & 0x7;
        if (x != 0 && y != 0)
            return chromaPredTwo(leftRow[comp], topLine[comp], predY, x);
        else if (x != 0)
            return chromaPredOne(leftRow[comp], predY);
        else if (y != 0)
            return chromaPredOne(topLine[comp], x);
        else
            return 128;
    }

    private int chromaPredBlk1(int comp, int x, int y) {
        int predY = y & 0x7;
        if (y != 0)
            return chromaPredOne(topLine[comp], x + 4);
        else if (x != 0)
            return chromaPredOne(leftRow[comp], predY);
        else
            return 128;
    }

    private int chromaPredBlk2(int comp, int x, int y) {
        int predY = y & 0x7;
        if (x != 0)
            return chromaPredOne(leftRow[comp], predY + 4);
        else if (y != 0)
            return chromaPredOne(topLine[comp], x);
        else
            return 128;
    }

    private int chromaPredBlk3(int comp, int x, int y) {
        int predY = y & 0x7;
        if (x != 0 && y != 0)
            return chromaPredTwo(leftRow[comp], topLine[comp], predY + 4, x + 4);
        else if (x != 0)
            return chromaPredOne(leftRow[comp], predY + 4);
        else if (y != 0)
            return chromaPredOne(topLine[comp], x + 4);
        else
            return 128;
    }

    private int lumaDCPred(int x, int y) {
        if (x == 0 && y == 0)
            return 128;

        if (y == 0)
            return (ArrayUtil.sum(leftRow[0]) + 8) >> 4;
        if (x == 0)
            return (ArrayUtil.sum(topLine[0], x, 16) + 8) >> 4;

        return (ArrayUtil.sum(leftRow[0]) + ArrayUtil.sum(topLine[0], x, 16) + 16) >> 5;
    }

    private int[][] transform(Picture pic, int comp, int qp, int cw, int ch, int x, int y) {
        int dcc = lumaDCPred(x, y);

        int[][] ac = new int[16 >> (cw + ch)][16];
        for (int i = 0; i < ac.length; i++) {
            int[] coeff = ac[i];
            takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + BLK_X[i], y
                    + BLK_Y[i], coeff, dcc);
            CoeffTransformer.fdct4x4(coeff);
        }
        return ac;
    }

    private final void takeSubtract(int[] planeData, int planeWidth, int planeHeight, int x, int y, int[] coeff, int dc) {
        if (x + 4 < planeWidth && y + 4 < planeHeight)
            takeSubtractSafe(planeData, planeWidth, planeHeight, x, y, coeff, dc);
        else
            takeSubtractUnsafe(planeData, planeWidth, planeHeight, x, y, coeff, dc);

    }

    private final void takeSubtractSafe(int[] planeData, int planeWidth, int planeHeight, int x, int y, int[] coeff,
            int dc) {
        for (int i = 0, srcOff = y * planeWidth + x, dstOff = 0; i < 4; i++, srcOff += planeWidth, dstOff += 4) {
            coeff[dstOff] = planeData[srcOff] - dc;
            coeff[dstOff + 1] = planeData[srcOff + 1] - dc;
            coeff[dstOff + 2] = planeData[srcOff + 2] - dc;
            coeff[dstOff + 3] = planeData[srcOff + 3] - dc;
        }
    }

    private final void takeSubtractUnsafe(int[] planeData, int planeWidth, int planeHeight, int x, int y, int[] coeff,
            int dc) {
        int outOff = 0;

        int i;
        for (i = y; i < Math.min(y + 4, planeHeight); i++) {
            int off = i * planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + 4, planeWidth); j++)
                coeff[outOff++] = planeData[off++] - dc;
            --off;
            for (; j < x + 4; j++)
                coeff[outOff++] = planeData[off] - dc;
        }
        for (; i < y + 4; i++) {
            int off = planeHeight * planeWidth - planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + 4, planeWidth); j++)
                coeff[outOff++] = planeData[off++] - dc;
            --off;
            for (; j < x + 4; j++)
                coeff[outOff++] = planeData[off] - dc;
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
