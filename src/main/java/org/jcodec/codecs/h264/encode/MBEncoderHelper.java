package org.jcodec.codecs.h264.encode;

import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.common.model.Picture8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class MBEncoderHelper {

    public static final void takeSubtract(byte[] planeData, int planeWidth, int planeHeight, int x, int y, int[] coeff,
            byte[] pred, int blkW, int blkH) {
        if (x + blkW < planeWidth && y + blkH < planeHeight)
            takeSubtractSafe(planeData, planeWidth, planeHeight, x, y, coeff, pred, blkW, blkH);
        else
            takeSubtractUnsafe(planeData, planeWidth, planeHeight, x, y, coeff, pred, blkW, blkH);

    }

    public static final void takeSubtractSafe(byte[] planeData, int planeWidth, int planeHeight, int x, int y,
            int[] coeff, byte[] pred, int blkW, int blkH) {
        for (int i = 0, srcOff = y * planeWidth + x, dstOff = 0; i < blkH; i++, srcOff += planeWidth) {
            for (int j = 0, srcOff1 = srcOff; j < blkW; j += 4, dstOff += 4, srcOff1 += 4) {
                coeff[dstOff] = planeData[srcOff1] - pred[dstOff];
                coeff[dstOff + 1] = planeData[srcOff1 + 1] - pred[dstOff + 1];
                coeff[dstOff + 2] = planeData[srcOff1 + 2] - pred[dstOff + 2];
                coeff[dstOff + 3] = planeData[srcOff1 + 3] - pred[dstOff + 3];
            }
        }
    }
    
    public static final void take(byte[] planeData, int planeWidth, int planeHeight, int x, int y, byte[] patch,
            int blkW, int blkH) {
        if (x + blkW < planeWidth && y + blkH < planeHeight)
            takeSafe(planeData, planeWidth, planeHeight, x, y, patch, blkW, blkH);
        else
            takeExtendBorder(planeData, planeWidth, planeHeight, x, y, patch, blkW, blkH);

    }

    public static final void takeSafe(byte[] planeData, int planeWidth, int planeHeight, int x, int y, byte[] patch,
            int blkW, int blkH) {
        for (int i = 0, srcOff = y * planeWidth + x, dstOff = 0; i < blkH; i++, srcOff += planeWidth) {
            for (int j = 0, srcOff1 = srcOff; j < blkW; ++j, ++dstOff, ++srcOff1) {
                patch[dstOff] = planeData[srcOff1];
            }
        }
    }
    
    public static final void takeExtendBorder(byte[] planeData, int planeWidth, int planeHeight, int x, int y, byte[] patch,
            int blkW, int blkH) {
        int outOff = 0;

        int i;
        for (i = y; i < Math.min(y + blkH, planeHeight); i++) {
            int off = i * planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + blkW, planeWidth); j++, outOff++, off++) {
                patch[outOff] = planeData[off];
            }
            --off;
            for (; j < x + blkW; j++, outOff++) {
                patch[outOff] = planeData[off];
            }
        }
        for (; i < y + blkH; i++) {
            int off = planeHeight * planeWidth - planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + blkW, planeWidth); j++, outOff++, off++) {
                patch[outOff] = planeData[off];
            }
            --off;
            for (; j < x + blkW; j++, outOff++) {
                patch[outOff] = planeData[off];
            }
        }
    }
    
    public static final void takeSafe(byte[] planeData, int planeWidth, int planeHeight, int x, int y, int[] coeff,
            int blkW, int blkH) {
        for (int i = 0, srcOff = y * planeWidth + x, dstOff = 0; i < blkH; i++, srcOff += planeWidth) {
            for (int j = 0, srcOff1 = srcOff; j < blkW; ++j, ++dstOff, ++srcOff1) {
                coeff[dstOff] = planeData[srcOff1];
            }
        }
    }

    public static final void takeSubtractUnsafe(byte[] planeData, int planeWidth, int planeHeight, int x, int y,
            int[] coeff, byte[] pred, int blkW, int blkH) {
        int outOff = 0;

        int i;
        for (i = y; i < Math.min(y + blkH, planeHeight); i++) {
            int off = i * planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + blkW, planeWidth); j++, outOff++, off++) {
                coeff[outOff] = planeData[off] - pred[outOff];
            }
            --off;
            for (; j < x + blkW; j++, outOff++) {
                coeff[outOff] = planeData[off] - pred[outOff];
            }
        }
        for (; i < y + blkH; i++) {
            int off = planeHeight * planeWidth - planeWidth + Math.min(x, planeWidth);
            int j;
            for (j = x; j < Math.min(x + blkW, planeWidth); j++, outOff++, off++) {
                coeff[outOff] = planeData[off] - pred[outOff];
            }
            --off;
            for (; j < x + blkW; j++, outOff++) {
                coeff[outOff] = planeData[off] - pred[outOff];
            }
        }
    }

    public static final void putBlk(byte[] planeData, int[] block, byte[] pred, int log2stride, int blkX, int blkY,
            int blkW, int blkH) {
        int stride = 1 << log2stride;
        for (int line = 0, srcOff = 0, dstOff = (blkY << log2stride) + blkX; line < blkH; line++) {
            int dstOff1 = dstOff;
            for (int row = 0; row < blkW; row += 4) {
                planeData[dstOff1] = (byte) clip(block[srcOff] + pred[srcOff], -128, 127);
                planeData[dstOff1 + 1] = (byte) clip(block[srcOff + 1] + pred[srcOff + 1], -128, 127);
                planeData[dstOff1 + 2] = (byte) clip(block[srcOff + 2] + pred[srcOff + 2], -128, 127);
                planeData[dstOff1 + 3] = (byte) clip(block[srcOff + 3] + pred[srcOff + 3], -128, 127);
                srcOff += 4;
                dstOff1 += 4;
            }
            dstOff += stride;
        }
    }

    public static final void putBlk(Picture8Bit dest, Picture8Bit src, int x, int y) {
        if (dest.getColor() != src.getColor())
            throw new RuntimeException("Incompatible color");
        for (int c = 0; c < dest.getColor().nComp; c++) {
            pubBlkOnePlane(dest.getPlaneData(c), dest.getPlaneWidth(c), src.getPlaneData(c), src.getPlaneWidth(c),
                    src.getPlaneHeight(c), x >> dest.getColor().compWidth[c], y >> dest.getColor().compHeight[c]);
        }
    }

    private static void pubBlkOnePlane(byte[] dest, int destWidth, byte[] src, int srcWidth, int srcHeight, int x, int y) {
        int destOff = y * destWidth + x;
        int srcOff = 0;
        for (int i = 0; i < srcHeight; i++) {
            for (int j = 0; j < srcWidth; j++, ++destOff, ++srcOff)
                dest[destOff] = src[srcOff];
            destOff += destWidth - srcWidth;
        }
    }
}
