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

    public final static int[][] COEFFS = new int[64][];

    static {
        for (int i = 1; i < 64; i++) {
            COEFFS[i] = new int[64];
            COEFFS[i][i] = 1024;
            SimpleIDCT10Bit.idct10(COEFFS[i], 0);
        }
    }

    public static final void dc(int[] block, int dc) {
        Arrays.fill(block, dc >> 3);
    }

    public static final void ac(int[] block, int ind, int level) {
        for (int i = 0; i < 64; i++) {
            block[i] += (COEFFS[ind][i] * level) >> 10;
        }
    }
}
