package org.jcodec.common.dct;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SparseIDCT {

    public final static int[][] COEFFS = new int[64][];

    static {
        for (int i = 1; i < 64; i++) {
            COEFFS[i] = new int[64];
            COEFFS[i][i] = 1024;
            SimpleIDCT10Bit.idct10(COEFFS[i], 0);
        }
    }

    public static final void dc(int[] block, int log2stride, int x, int y, int step, int dc) {
        dc >>= 3;
        int off = (y << log2stride) + x, stride = 1 << (log2stride + step);
        for (int i = 0; i < 8; i++) {
            block[off + 0] += dc;
            block[off + 1] += dc;
            block[off + 2] += dc;
            block[off + 3] += dc;
            block[off + 4] += dc;
            block[off + 5] += dc;
            block[off + 6] += dc;
            block[off + 7] += dc;

            off += stride;
        }
    }

    public static final void ac(int[] block, int log2stride, int x, int y, int step, int ind, int level) {
        int off = (y << log2stride) + x, stride = 1 << (log2stride + step);
        for (int i = 0, coeff = 0; i < 8; i++, coeff += 8) {
            block[off] += (COEFFS[ind][coeff] * level) >> 10;
            block[off + 1] += (COEFFS[ind][coeff + 1] * level) >> 10;
            block[off + 2] += (COEFFS[ind][coeff + 2] * level) >> 10;
            block[off + 3] += (COEFFS[ind][coeff + 3] * level) >> 10;
            block[off + 4] += (COEFFS[ind][coeff + 4] * level) >> 10;
            block[off + 5] += (COEFFS[ind][coeff + 5] * level) >> 10;
            block[off + 6] += (COEFFS[ind][coeff + 6] * level) >> 10;
            block[off + 7] += (COEFFS[ind][coeff + 7] * level) >> 10;

            off += stride;
        }
    }
}
