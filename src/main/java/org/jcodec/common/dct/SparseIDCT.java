package org.jcodec.common.dct;

import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SparseIDCT {

    public final static int[][] TAB = new int[64][];

    static {
        TAB[0] = new int[64];
        Arrays.fill(TAB[0], 128);

        for (int i = 1; i < 64; i++) {
            TAB[i] = new int[64];
            TAB[i][i] = 1024;
            SimpleIDCT10Bit.idct10(TAB[i], 0);
        }
    }

    public static final void dc(int[] block, int log2stride, int x, int y, int step, int dc) {
        dc /= 8;
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
            block[off] += ((TAB[ind][coeff] * level) / 1024);
            block[off + 1] += ((TAB[ind][coeff + 1] * level) / 1024);
            block[off + 2] += ((TAB[ind][coeff + 2] * level) / 1024);
            block[off + 3] += ((TAB[ind][coeff + 3] * level) / 1024);
            block[off + 4] += ((TAB[ind][coeff + 4] * level) / 1024);
            block[off + 5] += ((TAB[ind][coeff + 5] * level) / 1024);
            block[off + 6] += ((TAB[ind][coeff + 6] * level) / 1024);
            block[off + 7] += ((TAB[ind][coeff + 7] * level) / 1024);

            off += stride;
        }
    }
}