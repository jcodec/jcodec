package org.jcodec.codecs.h264.encode;

import static org.jcodec.common.tools.MathUtil.clip;

public class MBEncoderHelper {

    public static final void takeSubtract(int[] planeData, int planeWidth, int planeHeight, int x, int y, int[] coeff,
            int[] pred, int blkW, int blkH) {
        if (x + blkW < planeWidth && y + blkH < planeHeight)
            takeSubtractSafe(planeData, planeWidth, planeHeight, x, y, coeff, pred, blkW, blkH);
        else
            takeSubtractUnsafe(planeData, planeWidth, planeHeight, x, y, coeff, pred, blkW, blkH);

    }

    public static final void takeSubtractSafe(int[] planeData, int planeWidth, int planeHeight, int x, int y,
            int[] coeff, int[] pred, int blkW, int blkH) {
        for (int i = 0, srcOff = y * planeWidth + x, dstOff = 0; i < blkH; i++, srcOff += planeWidth) {
            for (int j = 0, srcOff1 = srcOff; j < blkW; j += 4, dstOff += 4, srcOff1 += 4) {
                coeff[dstOff] = planeData[srcOff1] - pred[dstOff];
                coeff[dstOff + 1] = planeData[srcOff1 + 1] - pred[dstOff + 1];
                coeff[dstOff + 2] = planeData[srcOff1 + 2] - pred[dstOff + 2];
                coeff[dstOff + 3] = planeData[srcOff1 + 3] - pred[dstOff + 3];
            }
        }
    }

    public static final void takeSubtractUnsafe(int[] planeData, int planeWidth, int planeHeight, int x, int y,
            int[] coeff, int[] pred, int blkW, int blkH) {
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

    public static final void putBlk(int[] planeData, int[] block, int[] pred, int log2stride, int blkX, int blkY,
            int blkW, int blkH) {
        int stride = 1 << log2stride;
        for (int line = 0, srcOff = 0, dstOff = (blkY << log2stride) + blkX; line < blkH; line++) {
            int dstOff1 = dstOff;
            for (int row = 0; row < blkW; row += 4) {
                planeData[dstOff1] = clip(block[srcOff] + pred[srcOff], 0, 255);
                planeData[dstOff1 + 1] = clip(block[srcOff + 1] + pred[srcOff + 1], 0, 255);
                planeData[dstOff1 + 2] = clip(block[srcOff + 2] + pred[srcOff + 2], 0, 255);
                planeData[dstOff1 + 3] = clip(block[srcOff + 3] + pred[srcOff + 3], 0, 255);
                srcOff += 4;
                dstOff1 += 4;
            }
            dstOff += stride;
        }
    }

}
