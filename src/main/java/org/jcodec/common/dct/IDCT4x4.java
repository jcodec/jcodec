package org.jcodec.common.dct;


/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class IDCT4x4 {

    public static void idct(int[] blk, int off) {
        int i;

        for (i = 0; i < 4; i++) {
            idct4row(blk, off + (i << 2));
        }

        for (i = 0; i < 4; i++) {
            idct4col_add(blk, off + i);
        }
    }

    public static final int CN_SHIFT = 12;

    public static final int C_FIX(double x) {
        return ((int) ((x) * 1.414213562 * (1 << CN_SHIFT) + 0.5));
    }

    public static final int C1 = C_FIX(0.6532814824);
    public static final int C2 = C_FIX(0.2705980501);
    public static final int C3 = C_FIX(0.5);;
    public static final int C_SHIFT = (4 + 2 + 12);

    private static void idct4col_add(int[] blk, int off) {
        int c0, c1, c2, c3, a0, a1, a2, a3;

        a0 = blk[off];
        a1 = blk[off + 4];
        a2 = blk[off + 8];
        a3 = blk[off + 12];
        c0 = (a0 + a2) * C3 + (1 << (C_SHIFT - 1));
        c2 = (a0 - a2) * C3 + (1 << (C_SHIFT - 1));
        c1 = a1 * C1 + a3 * C2;
        c3 = a1 * C2 - a3 * C1;

        blk[off] = ((c0 + c1) >> C_SHIFT);
        blk[off + 4] = ((c2 + c3) >> C_SHIFT);
        blk[off + 8] = ((c2 - c3) >> C_SHIFT);
        blk[off + 12] = ((c0 - c1) >> C_SHIFT);
    }

    public static final int RN_SHIFT = 15;

    public static final int R_FIX(double x) {
        return ((int) ((x) * 1.414213562 * (1 << RN_SHIFT) + 0.5));
    }

    public static final int R1 = R_FIX(0.6532814824);
    public static final int R2 = R_FIX(0.2705980501);
    public static final int R3 = R_FIX(0.5);
    public static final int R_SHIFT = 11;

    private static void idct4row(int[] blk, int off) {
        int c0, c1, c2, c3, a0, a1, a2, a3;

        a0 = blk[off];
        a1 = blk[off + 1];
        a2 = blk[off + 2];
        a3 = blk[off + 3];
        c0 = (a0 + a2) * R3 + (1 << (R_SHIFT - 1));
        c2 = (a0 - a2) * R3 + (1 << (R_SHIFT - 1));
        c1 = a1 * R1 + a3 * R2;
        c3 = a1 * R2 - a3 * R1;
        blk[off] = (c0 + c1) >> R_SHIFT;
        blk[off + 1] = (c2 + c3) >> R_SHIFT;
        blk[off + 2] = (c2 - c3) >> R_SHIFT;
        blk[off + 3] = (c0 - c1) >> R_SHIFT;
    }
}
