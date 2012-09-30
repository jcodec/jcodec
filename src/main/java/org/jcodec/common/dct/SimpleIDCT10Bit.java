package org.jcodec.common.dct;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class SimpleIDCT10Bit {

    public static int W1 = 90901;
    public static int W2 = 85627;
    public static int W3 = 77062;
    public static int W4 = 65535;
    public static int W5 = 51491;
    public static int W6 = 35468;
    public static int W7 = 18081;
    public static int ROW_SHIFT = 15;
    public static int COL_SHIFT = 20;

    public static final void idct10(int[] buf, int off) {
        for (int i = 0; i < 8; i++)
            idctRow(buf, off + (i << 3));
        for (int i = 0; i < 8; i++)
            idctCol(buf, off + i);
    }

    private static final void idctCol(int[] buf, int off) {
        int a0, a1, a2, a3, b0, b1, b2, b3;

        a0 = W4 * (buf[off + 8 * 0] + ((1 << (COL_SHIFT - 1)) / W4));
        a1 = a0;
        a2 = a0;
        a3 = a0;

        a0 += W2 * buf[off + 8 * 2];
        a1 += W6 * buf[off + 8 * 2];
        a2 += -W6 * buf[off + 8 * 2];
        a3 += -W2 * buf[off + 8 * 2];

        b0 = W1 * buf[off + 8 * 1];
        b1 = W3 * buf[off + 8 * 1];
        b2 = W5 * buf[off + 8 * 1];
        b3 = W7 * buf[off + 8 * 1];

        b0 += W3 * buf[off + 8 * 3];
        b1 += -W7 * buf[off + 8 * 3];
        b2 += -W1 * buf[off + 8 * 3];
        b3 += -W5 * buf[off + 8 * 3];

        if (buf[off + 8 * 4] != 0) {
            a0 += W4 * buf[off + 8 * 4];
            a1 += -W4 * buf[off + 8 * 4];
            a2 += -W4 * buf[off + 8 * 4];
            a3 += W4 * buf[off + 8 * 4];
        }

        if (buf[off + 8 * 5] != 0) {
            b0 += W5 * buf[off + 8 * 5];
            b1 += -W1 * buf[off + 8 * 5];
            b2 += W7 * buf[off + 8 * 5];
            b3 += W3 * buf[off + 8 * 5];
        }

        if (buf[off + 8 * 6] != 0) {
            a0 += W6 * buf[off + 8 * 6];
            a1 += -W2 * buf[off + 8 * 6];
            a2 += W2 * buf[off + 8 * 6];
            a3 += -W6 * buf[off + 8 * 6];
        }

        if (buf[off + 8 * 7] != 0) {
            b0 += W7 * buf[off + 8 * 7];
            b1 += -W5 * buf[off + 8 * 7];
            b2 += W3 * buf[off + 8 * 7];
            b3 += -W1 * buf[off + 8 * 7];
        }

        buf[off] = ((a0 + b0) >> COL_SHIFT);
        buf[off + 8] = ((a1 + b1) >> COL_SHIFT);
        buf[off + 16] = ((a2 + b2) >> COL_SHIFT);
        buf[off + 24] = ((a3 + b3) >> COL_SHIFT);
        buf[off + 32] = ((a3 - b3) >> COL_SHIFT);
        buf[off + 40] = ((a2 - b2) >> COL_SHIFT);
        buf[off + 48] = ((a1 - b1) >> COL_SHIFT);
        buf[off + 56] = ((a0 - b0) >> COL_SHIFT);
    }

    private static final void idctRow(int[] buf, int off) {
        int a0, a1, a2, a3, b0, b1, b2, b3;

        a0 = (W4 * buf[off]) + (1 << (ROW_SHIFT - 1));
        a1 = a0;
        a2 = a0;
        a3 = a0;

        a0 += W2 * buf[off + 2];
        a1 += W6 * buf[off + 2];
        a2 -= W6 * buf[off + 2];
        a3 -= W2 * buf[off + 2];

        b0 = W1 * buf[off + 1];
        b0 += W3 * buf[off + 3];
        b1 = W3 * buf[off + 1];
        b1 += -W7 * buf[off + 3];
        b2 = W5 * buf[off + 1];
        b2 += -W1 * buf[off + 3];
        b3 = W7 * buf[off + 1];
        b3 += -W5 * buf[off + 3];

        if (buf[off + 4] != 0 || buf[off + 5] != 0 || buf[off + 6] != 0 || buf[off + 7] != 0) {
            a0 += W4 * buf[off + 4] + W6 * buf[off + 6];
            a1 += -W4 * buf[off + 4] - W2 * buf[off + 6];
            a2 += -W4 * buf[off + 4] + W2 * buf[off + 6];
            a3 += W4 * buf[off + 4] - W6 * buf[off + 6];

            b0 += W5 * buf[off + 5];
            b0 += W7 * buf[off + 7];

            b1 += -W1 * buf[off + 5];
            b1 += -W5 * buf[off + 7];

            b2 += W7 * buf[off + 5];
            b2 += W3 * buf[off + 7];

            b3 += W3 * buf[off + 5];
            b3 += -W1 * buf[off + 7];
        }

        buf[off + 0] = (a0 + b0) >> ROW_SHIFT;
        buf[off + 7] = (a0 - b0) >> ROW_SHIFT;
        buf[off + 1] = (a1 + b1) >> ROW_SHIFT;
        buf[off + 6] = (a1 - b1) >> ROW_SHIFT;
        buf[off + 2] = (a2 + b2) >> ROW_SHIFT;
        buf[off + 5] = (a2 - b2) >> ROW_SHIFT;
        buf[off + 3] = (a3 + b3) >> ROW_SHIFT;
        buf[off + 4] = (a3 - b3) >> ROW_SHIFT;
    }

    private static void fdctRow(int[] buf, int off) {
        
    }
}