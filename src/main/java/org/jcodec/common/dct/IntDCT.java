package org.jcodec.common.dct;

import java.nio.IntBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Alex Zhukov
 *
 */
public class IntDCT extends DCT {
    public final static IntDCT INSTANCE = new IntDCT();
    private static final int DCTSIZE = 8;
    private static final int PASS1_BITS = 2;
    private static final int MAXJSAMPLE = 255;
    private static final int CENTERJSAMPLE = 128;
    /** 2 bits wider than legal samples */
    private static final int RANGE_MASK = (MAXJSAMPLE * 4 + 3);

    public int[] decode(int[] orig) {
        IntBuffer inptr = IntBuffer.wrap(orig);
        IntBuffer workspace = IntBuffer.allocate(64);
        IntBuffer outptr = IntBuffer.allocate(64);
        doDecode(inptr, workspace, outptr);
        return outptr.array();
    }

    protected IntBuffer doDecode(IntBuffer inptr, IntBuffer workspace,
            IntBuffer outptr) {
        pass1(inptr, workspace.duplicate());
        pass2(outptr, workspace.duplicate());
        return outptr;
    }

    /**
     * Pass 2: process rows from work array, store into output array.
     * 
     * Note that we must descale the results by a factor of 8 == 23, and also
     * undo the PASS1_BITS scaling.
     */
    private static void pass2(IntBuffer outptr, IntBuffer wsptr) {
        for (int ctr = 0; ctr < DCTSIZE; ctr++) {
            /* Even part: reverse the even part of the forward DCT. */
            /* The rotator is sqrt(2)c(-6). */

            int z2 = wsptr.get(2);
            int z3 = wsptr.get(6);

            int z1 = MULTIPLY(z2 + z3, FIX_0_541196100);
            int tmp2 = z1 + MULTIPLY(z3, -FIX_1_847759065);
            int tmp3 = z1 + MULTIPLY(z2, FIX_0_765366865);

            int tmp0 = ((int) wsptr.get(0) + (int) wsptr.get(4)) << CONST_BITS;
            int tmp1 = ((int) wsptr.get(0) - (int) wsptr.get(4)) << CONST_BITS;

            int tmp10 = tmp0 + tmp3;
            int tmp13 = tmp0 - tmp3;
            int tmp11 = tmp1 + tmp2;
            int tmp12 = tmp1 - tmp2;

            /*
             * Odd part per figure 8; the matrix is unitary and hence its
             * transpose is its inverse. i0..i3 are y7,y5,y3,y1 respectively.
             */

            tmp0 = (int) wsptr.get(7);
            tmp1 = (int) wsptr.get(5);
            tmp2 = (int) wsptr.get(3);
            tmp3 = (int) wsptr.get(1);

            z1 = tmp0 + tmp3;
            z2 = tmp1 + tmp2;
            z3 = tmp0 + tmp2;
            int z4 = tmp1 + tmp3;
            int z5 = MULTIPLY(z3 + z4, FIX_1_175875602); /* sqrt(2) c3 */

            tmp0 = MULTIPLY(tmp0, FIX_0_298631336); /* sqrt(2) (-c1+c3+c5-c7) */
            tmp1 = MULTIPLY(tmp1, FIX_2_053119869); /* sqrt(2) ( c1+c3-c5+c7) */
            tmp2 = MULTIPLY(tmp2, FIX_3_072711026); /* sqrt(2) ( c1+c3+c5-c7) */
            tmp3 = MULTIPLY(tmp3, FIX_1_501321110); /* sqrt(2) ( c1+c3-c5-c7) */
            z1 = MULTIPLY(z1, -FIX_0_899976223); /* sqrt(2) (c7-c3) */
            z2 = MULTIPLY(z2, -FIX_2_562915447); /* sqrt(2) (-c1-c3) */
            z3 = MULTIPLY(z3, -FIX_1_961570560); /* sqrt(2) (-c3-c5) */
            z4 = MULTIPLY(z4, -FIX_0_390180644); /* sqrt(2) (c5-c3) */

            z3 += z5;
            z4 += z5;

            tmp0 += z1 + z3;
            tmp1 += z2 + z4;
            tmp2 += z2 + z3;
            tmp3 += z1 + z4;

            /* Final output stage: inputs are tmp10..tmp13, tmp0..tmp3 */

            int D = CONST_BITS + PASS1_BITS + 3;
            outptr.put(range_limit(DESCALE(tmp10 + tmp3, D) & RANGE_MASK));
            outptr.put(range_limit(DESCALE(tmp11 + tmp2, D) & RANGE_MASK));
            outptr.put(range_limit(DESCALE(tmp12 + tmp1, D) & RANGE_MASK));
            outptr.put(range_limit(DESCALE(tmp13 + tmp0, D) & RANGE_MASK));
            outptr.put(range_limit(DESCALE(tmp13 - tmp0, D) & RANGE_MASK));
            outptr.put(range_limit(DESCALE(tmp12 - tmp1, D) & RANGE_MASK));
            outptr.put(range_limit(DESCALE(tmp11 - tmp2, D) & RANGE_MASK));
            outptr.put(range_limit(DESCALE(tmp10 - tmp3, D) & RANGE_MASK));

            wsptr = advance(wsptr, DCTSIZE); /* advance pointer to next row */

        }
    }

    public static int range_limit(int i) {
        return idct_sample_range_limit.get(i + 256);
    }

    private final static IntBuffer sample_range_limit = IntBuffer
            .allocate((5 * (MAXJSAMPLE + 1) + CENTERJSAMPLE));
    private final static IntBuffer idct_sample_range_limit = IntBuffer
            .allocate(sample_range_limit.capacity() - 128);
    static {
        prepare_range_limit_table();
    }

    /* Allocate and fill in the sample_range_limit table */
    private static void prepare_range_limit_table() {
        sample_range_limit.position(256);
        for (int i = 0; i < 128; i++) {
            sample_range_limit.put(i);
        }
        for (int i = -128; i < 0; i++) {
            sample_range_limit.put(i);
        }
        for (int i = 0; i < 256 + 128; i++) {
            sample_range_limit.put(-1);
        }
        for (int i = 0; i < 256 + 128; i++) {
            sample_range_limit.put(0);
        }
        for (int i = 0; i < 128; i++) {
            sample_range_limit.put(i);
        }

        for (int i = 0; i < idct_sample_range_limit.capacity(); i++) {
            idct_sample_range_limit.put(sample_range_limit.get(i + 128) & 0xff);
        }

    }

    private static boolean shortcut(IntBuffer inptr, IntBuffer wsptr) {
        /*
         * Due to quantization, we will usually find that many of the input
         * coefficients are zero, especially the AC terms. We can exploit this
         * by short-circuiting the IDCT calculation for any column in which all
         * the AC terms are zero. In that case each output is equal to the DC
         * coefficient (with scale factor as needed). With typical images and
         * quantization tables, half or more of the column DCT calculations can
         * be simplified this way.
         */

        if (inptr.get(DCTSIZE * 1) == 0 && inptr.get(DCTSIZE * 2) == 0
                && inptr.get(DCTSIZE * 3) == 0 && inptr.get(DCTSIZE * 4) == 0
                && inptr.get(DCTSIZE * 5) == 0 && inptr.get(DCTSIZE * 6) == 0
                && inptr.get(DCTSIZE * 7) == 0) {
            /* AC terms all zero */
            int dcval = inptr.get(DCTSIZE * 0) << PASS1_BITS;

            wsptr.put(DCTSIZE * 0, dcval);
            wsptr.put(DCTSIZE * 1, dcval);
            wsptr.put(DCTSIZE * 2, dcval);
            wsptr.put(DCTSIZE * 3, dcval);
            wsptr.put(DCTSIZE * 4, dcval);
            wsptr.put(DCTSIZE * 5, dcval);
            wsptr.put(DCTSIZE * 6, dcval);
            wsptr.put(DCTSIZE * 7, dcval);

            inptr = advance(inptr);
            wsptr = advance(wsptr);
            return true;
        }
        return false;
    }

    /**
     * Pass 1: process columns from input, store into work array.
     * 
     * Note results are scaled up by sqrt(8) compared to a true IDCT;
     * furthermore, we scale the results by 2xPASS1_BITS.
     */
    private static void pass1(IntBuffer inptr, IntBuffer wsptr) {

        for (int ctr = DCTSIZE; ctr > 0; ctr--) {
            // if (shortcircuit(inptr, wsptr))
            // continue;
            /* Even part: reverse the even part of the forward DCT. */
            /* The rotator is sqrt(2)c(-6). */

            int z2 = inptr.get(DCTSIZE * 2);
            int z3 = inptr.get(DCTSIZE * 6);
            int z1 = MULTIPLY(z2 + z3, FIX_0_541196100);
            int tmp2 = z1 + MULTIPLY(z3, -FIX_1_847759065);
            int tmp3 = z1 + MULTIPLY(z2, FIX_0_765366865);
            z2 = inptr.get(DCTSIZE * 0);
            z3 = inptr.get(DCTSIZE * 4);
            int tmp0 = (z2 + z3) << CONST_BITS;
            int tmp1 = (z2 - z3) << CONST_BITS;
            int tmp10 = tmp0 + tmp3;
            int tmp13 = tmp0 - tmp3;
            int tmp11 = tmp1 + tmp2;
            int tmp12 = tmp1 - tmp2;
            /*
             * Odd part per figure 8; the matrix is unitary and hence its
             * transpose is its inverse. i0..i3 are y7,y5,y3,y1 respectively.
             */

            tmp0 = inptr.get(DCTSIZE * 7);
            tmp1 = inptr.get(DCTSIZE * 5);
            tmp2 = inptr.get(DCTSIZE * 3);
            tmp3 = inptr.get(DCTSIZE * 1);
            z1 = tmp0 + tmp3;
            z2 = tmp1 + tmp2;
            z3 = tmp0 + tmp2;
            int z4 = tmp1 + tmp3;
            int z5 = MULTIPLY(z3 + z4, FIX_1_175875602); /* sqrt(2) c3 */

            tmp0 = MULTIPLY(tmp0, FIX_0_298631336); /* sqrt(2) (-c1+c3+c5-c7) */
            tmp1 = MULTIPLY(tmp1, FIX_2_053119869); /* sqrt(2) ( c1+c3-c5+c7) */
            tmp2 = MULTIPLY(tmp2, FIX_3_072711026); /* sqrt(2) ( c1+c3+c5-c7) */
            tmp3 = MULTIPLY(tmp3, FIX_1_501321110); /* sqrt(2) ( c1+c3-c5-c7) */
            z1 = MULTIPLY(z1, -FIX_0_899976223); /* sqrt(2) (c7-c3) */
            z2 = MULTIPLY(z2, -FIX_2_562915447); /* sqrt(2) (-c1-c3) */
            z3 = MULTIPLY(z3, -FIX_1_961570560); /* sqrt(2) (-c3-c5) */
            z4 = MULTIPLY(z4, -FIX_0_390180644); /* sqrt(2) (c5-c3) */
            z3 += z5;
            z4 += z5;

            tmp0 += z1 + z3;
            tmp1 += z2 + z4;
            tmp2 += z2 + z3;
            tmp3 += z1 + z4;

            /* Final output stage: inputs are tmp10..tmp13, tmp0..tmp3 */

            int D = CONST_BITS - PASS1_BITS;
            wsptr.put(DCTSIZE * 0, DESCALE(tmp10 + tmp3, D));
            wsptr.put(DCTSIZE * 7, DESCALE(tmp10 - tmp3, D));
            wsptr.put(DCTSIZE * 1, DESCALE(tmp11 + tmp2, D));
            wsptr.put(DCTSIZE * 6, DESCALE(tmp11 - tmp2, D));
            wsptr.put(DCTSIZE * 2, DESCALE(tmp12 + tmp1, D));
            wsptr.put(DCTSIZE * 5, DESCALE(tmp12 - tmp1, D));
            wsptr.put(DCTSIZE * 3, DESCALE(tmp13 + tmp0, D));
            wsptr.put(DCTSIZE * 4, DESCALE(tmp13 - tmp0, D));
            inptr = advance(inptr);
            wsptr = advance(wsptr);
        }
    }

    /**
     * advance pointers to next column
     */
    private static IntBuffer advance(IntBuffer ptr) {
        return advance(ptr, 1);
    }

    private static IntBuffer advance(IntBuffer ptr, int size) {
        ptr.position(ptr.position() + size);
        return ptr.slice();
    }

    static int DESCALE(int x, int n) {
        return RIGHT_SHIFT((x) + (1 << ((n) - 1)), n);
    }

    private static int RIGHT_SHIFT(int x, int shft) {
        return x >> shft;
    }

    private static final int MULTIPLY(int i, int j) {
        return i * j;
    }

    private final static int FIX_0_298631336 = FIX(0.298631336);
    private final static int FIX_0_390180644 = FIX(0.390180644);
    private final static int FIX_0_541196100 = FIX(0.541196100);
    private final static int FIX_0_765366865 = FIX(0.765366865);
    private final static int FIX_0_899976223 = FIX(0.899976223);
    private final static int FIX_1_175875602 = FIX(1.175875602);
    private final static int FIX_1_501321110 = FIX(1.501321110);
    private final static int FIX_1_847759065 = FIX(1.847759065);
    private final static int FIX_1_961570560 = FIX(1.961570560);
    private final static int FIX_2_053119869 = FIX(2.053119869);
    private final static int FIX_2_562915447 = FIX(2.562915447);
    private final static int FIX_3_072711026 = FIX(3.072711026);

    private static final int CONST_BITS = 13;
    private static final int ONE_HALF = (1 << (CONST_BITS - 1));

    private final static int FIX(double x) {
        return ((int) ((x) * (1 << CONST_BITS) + 0.5));
    }

}
