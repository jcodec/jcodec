package org.jcodec.common.dct;
import java.nio.ShortBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Alex Zhukov
 *
 */
public class FfmpegIntDct {
    private static final int DCTSIZE = 8;
    private static final int DCTSIZE_6 = DCTSIZE * 6;
    private static final int DCTSIZE_5 = DCTSIZE * 5;
    private static final int DCTSIZE_4 = DCTSIZE * 4;
    private static final int DCTSIZE_3 = DCTSIZE * 3;
    private static final int DCTSIZE_2 = DCTSIZE * 2;
    private static final int DCTSIZE_1 = DCTSIZE * 1;
    private static final int DCTSIZE_7 = DCTSIZE * 7;
    private static final int DCTSIZE_0 = DCTSIZE * 0;
    private static final int PASS1_BITS = 2;
    private static final int CONST_BITS = 13;
    private static final int D1 = CONST_BITS - PASS1_BITS;
    private static final int D2 = CONST_BITS + PASS1_BITS + 3;
    private static final int ONEHALF_11 = (1 << (D1 - 1));
    private static final int ONEHALF_18 = (1 << (D2 - 1));

    public short[] decode(short[] orig) {
        ShortBuffer data = ShortBuffer.wrap(orig);
        pass1(data);
        pass2(data);
        return orig;
    }

    private static ShortBuffer advance(ShortBuffer dataptr, int size) {
        dataptr.position(dataptr.position() + size);
        return dataptr.slice();
    }

    /*
     * Unlike our decoder where we approximate the FIXes, we need to use exact
     * ones here or successive P-frames will drift too much with Reference frame
     * coding
     */
    private final static short FIX_0_211164243 = 1730;
    private final static short FIX_0_275899380 = 2260;
    private final static short FIX_0_298631336 = 2446;
    private final static short FIX_0_390180644 = 3196;
    private final static short FIX_0_509795579 = 4176;
    private final static short FIX_0_541196100 = 4433;
    private final static short FIX_0_601344887 = 4926;
    private final static short FIX_0_765366865 = 6270;
    private final static short FIX_0_785694958 = 6436;
    private final static short FIX_0_899976223 = 7373;
    private final static short FIX_1_061594337 = 8697;
    private final static short FIX_1_111140466 = 9102;
    private final static short FIX_1_175875602 = 9633;
    private final static short FIX_1_306562965 = 10703;
    private final static short FIX_1_387039845 = 11363;
    private final static short FIX_1_451774981 = 11893;
    private final static short FIX_1_501321110 = 12299;
    private final static short FIX_1_662939225 = 13623;
    private final static short FIX_1_847759065 = 15137;
    private final static short FIX_1_961570560 = 16069;
    private final static short FIX_2_053119869 = 16819;
    private final static short FIX_2_172734803 = 17799;
    private final static short FIX_2_562915447 = 20995;
    private final static short FIX_3_072711026 = 25172;

    /* Pass 1: process rows. */
    /* Note results are scaled up by sqrt(8) compared to a true IDCT; */
    /* furthermore, we scale the results by 2PASS1_BITS. */
    private final static void pass1(ShortBuffer data) {
        int z1;
        int tmp2;
        int tmp3;
        int tmp0;
        int tmp1;
        int tmp10;
        int tmp13;
        int tmp11;
        int tmp12;
        int z2;
        int z3;
        int z4;
        int z5;

        ShortBuffer dataptr = data.duplicate();

        for (int rowctr = DCTSIZE - 1; rowctr >= 0; rowctr--) {
            /*
             * Due to quantization, we will usually find that many of the input
             * coefficients are zero, especially the AC terms. We can exploit
             * this by short-circuiting the IDCT calculation for any row in
             * which all the AC terms are zero. In that case each output is
             * equal to the DC coefficient (with scale factor as needed). With
             * typical images and quantization tables, half or more of the row
             * DCT calculations can be simplified this way.
             */

            // register int *idataptr = (int*)dataptr;
            /*
             * WARNING: we do the same permutation as MMX idct to simplify the
             * video core
             */
            int d0 = dataptr.get(0);
            int d2 = dataptr.get(1);
            int d4 = dataptr.get(2);
            int d6 = dataptr.get(3);
            int d1 = dataptr.get(4);
            int d3 = dataptr.get(5);
            int d5 = dataptr.get(6);
            int d7 = dataptr.get(7);

            if ((d1 | d2 | d3 | d4 | d5 | d6 | d7) == 0) {
                /* AC terms all zero */
                if (d0 != 0) {
                    /* Compute a 32 bit value to assign. */
                    int dcval = (int) (d0 << PASS1_BITS);
                    for (int i = 0; i < 8; i++) {
                        dataptr.put(i, (short) dcval);
                    }
                }
                /*
                 * advance pointer to next row
                 */
                dataptr = advance(dataptr, DCTSIZE);
                continue;
            }

            /* Even part: reverse the even part of the forward DCT. */
            /* The rotator is sqrt(2)c(-6). */
            if (d6 != 0) {
                if (d2 != 0) {
                    /* d0 != 0, d2 != 0, d4 != 0, d6 != 0 */
                    z1 = MULTIPLY(d2 + d6, FIX_0_541196100);
                    tmp2 = z1 + MULTIPLY(-d6, FIX_1_847759065);
                    tmp3 = z1 + MULTIPLY(d2, FIX_0_765366865);

                    tmp0 = (d0 + d4) << CONST_BITS;
                    tmp1 = (d0 - d4) << CONST_BITS;

                    tmp10 = tmp0 + tmp3;
                    tmp13 = tmp0 - tmp3;
                    tmp11 = tmp1 + tmp2;
                    tmp12 = tmp1 - tmp2;
                } else {
                    /* d0 != 0, d2 == 0, d4 != 0, d6 != 0 */
                    tmp2 = MULTIPLY(-d6, FIX_1_306562965);
                    tmp3 = MULTIPLY(d6, FIX_0_541196100);

                    tmp0 = (d0 + d4) << CONST_BITS;
                    tmp1 = (d0 - d4) << CONST_BITS;

                    tmp10 = tmp0 + tmp3;
                    tmp13 = tmp0 - tmp3;
                    tmp11 = tmp1 + tmp2;
                    tmp12 = tmp1 - tmp2;
                }
            } else {
                if (d2 != 0) {
                    /* d0 != 0, d2 != 0, d4 != 0, d6 == 0 */
                    tmp2 = MULTIPLY(d2, FIX_0_541196100);
                    tmp3 = MULTIPLY(d2, FIX_1_306562965);

                    tmp0 = (d0 + d4) << CONST_BITS;
                    tmp1 = (d0 - d4) << CONST_BITS;

                    tmp10 = tmp0 + tmp3;
                    tmp13 = tmp0 - tmp3;
                    tmp11 = tmp1 + tmp2;
                    tmp12 = tmp1 - tmp2;
                } else {
                    /* d0 != 0, d2 == 0, d4 != 0, d6 == 0 */
                    tmp10 = tmp13 = (d0 + d4) << CONST_BITS;
                    tmp11 = tmp12 = (d0 - d4) << CONST_BITS;
                }
            }

            /*
             * Odd part per figure 8; the matrix is unitary and hence its
             * transpose is its inverse. i0..i3 are y7,y5,y3,y1 respectively.
             */

            if (d7 != 0) {
                if (d5 != 0) {
                    if (d3 != 0) {
                        if (d1 != 0) {
                            /* d1 != 0, d3 != 0, d5 != 0, d7 != 0 */
                            z1 = d7 + d1;
                            z2 = d5 + d3;
                            z3 = d7 + d3;
                            z4 = d5 + d1;
                            z5 = MULTIPLY(z3 + z4, FIX_1_175875602);

                            tmp0 = MULTIPLY(d7, FIX_0_298631336);
                            tmp1 = MULTIPLY(d5, FIX_2_053119869);
                            tmp2 = MULTIPLY(d3, FIX_3_072711026);
                            tmp3 = MULTIPLY(d1, FIX_1_501321110);
                            z1 = MULTIPLY(-z1, FIX_0_899976223);
                            z2 = MULTIPLY(-z2, FIX_2_562915447);
                            z3 = MULTIPLY(-z3, FIX_1_961570560);
                            z4 = MULTIPLY(-z4, FIX_0_390180644);

                            z3 += z5;
                            z4 += z5;

                            tmp0 += z1 + z3;
                            tmp1 += z2 + z4;
                            tmp2 += z2 + z3;
                            tmp3 += z1 + z4;
                        } else {
                            /* d1 == 0, d3 != 0, d5 != 0, d7 != 0 */
                            z2 = d5 + d3;
                            z3 = d7 + d3;
                            z5 = MULTIPLY(z3 + d5, FIX_1_175875602);

                            tmp0 = MULTIPLY(d7, FIX_0_298631336);
                            tmp1 = MULTIPLY(d5, FIX_2_053119869);
                            tmp2 = MULTIPLY(d3, FIX_3_072711026);
                            z1 = MULTIPLY(-d7, FIX_0_899976223);
                            z2 = MULTIPLY(-z2, FIX_2_562915447);
                            z3 = MULTIPLY(-z3, FIX_1_961570560);
                            z4 = MULTIPLY(-d5, FIX_0_390180644);

                            z3 += z5;
                            z4 += z5;

                            tmp0 += z1 + z3;
                            tmp1 += z2 + z4;
                            tmp2 += z2 + z3;
                            tmp3 = z1 + z4;
                        }
                    } else {
                        if (d1 != 0) {
                            /* d1 != 0, d3 == 0, d5 != 0, d7 != 0 */
                            z1 = d7 + d1;
                            z4 = d5 + d1;
                            z5 = MULTIPLY(d7 + z4, FIX_1_175875602);

                            tmp0 = MULTIPLY(d7, FIX_0_298631336);
                            tmp1 = MULTIPLY(d5, FIX_2_053119869);
                            tmp3 = MULTIPLY(d1, FIX_1_501321110);
                            z1 = MULTIPLY(-z1, FIX_0_899976223);
                            z2 = MULTIPLY(-d5, FIX_2_562915447);
                            z3 = MULTIPLY(-d7, FIX_1_961570560);
                            z4 = MULTIPLY(-z4, FIX_0_390180644);

                            z3 += z5;
                            z4 += z5;

                            tmp0 += z1 + z3;
                            tmp1 += z2 + z4;
                            tmp2 = z2 + z3;
                            tmp3 += z1 + z4;
                        } else {
                            /* d1 == 0, d3 == 0, d5 != 0, d7 != 0 */
                            tmp0 = MULTIPLY(-d7, FIX_0_601344887);
                            z1 = MULTIPLY(-d7, FIX_0_899976223);
                            z3 = MULTIPLY(-d7, FIX_1_961570560);
                            tmp1 = MULTIPLY(-d5, FIX_0_509795579);
                            z2 = MULTIPLY(-d5, FIX_2_562915447);
                            z4 = MULTIPLY(-d5, FIX_0_390180644);
                            z5 = MULTIPLY(d5 + d7, FIX_1_175875602);

                            z3 += z5;
                            z4 += z5;

                            tmp0 += z3;
                            tmp1 += z4;
                            tmp2 = z2 + z3;
                            tmp3 = z1 + z4;
                        }
                    }
                } else {
                    if (d3 != 0) {
                        if (d1 != 0) {
                            /* d1 != 0, d3 != 0, d5 == 0, d7 != 0 */
                            z1 = d7 + d1;
                            z3 = d7 + d3;
                            z5 = MULTIPLY(z3 + d1, FIX_1_175875602);

                            tmp0 = MULTIPLY(d7, FIX_0_298631336);
                            tmp2 = MULTIPLY(d3, FIX_3_072711026);
                            tmp3 = MULTIPLY(d1, FIX_1_501321110);
                            z1 = MULTIPLY(-z1, FIX_0_899976223);
                            z2 = MULTIPLY(-d3, FIX_2_562915447);
                            z3 = MULTIPLY(-z3, FIX_1_961570560);
                            z4 = MULTIPLY(-d1, FIX_0_390180644);

                            z3 += z5;
                            z4 += z5;

                            tmp0 += z1 + z3;
                            tmp1 = z2 + z4;
                            tmp2 += z2 + z3;
                            tmp3 += z1 + z4;
                        } else {
                            /* d1 == 0, d3 != 0, d5 == 0, d7 != 0 */
                            z3 = d7 + d3;

                            tmp0 = MULTIPLY(-d7, FIX_0_601344887);
                            z1 = MULTIPLY(-d7, FIX_0_899976223);
                            tmp2 = MULTIPLY(d3, FIX_0_509795579);
                            z2 = MULTIPLY(-d3, FIX_2_562915447);
                            z5 = MULTIPLY(z3, FIX_1_175875602);
                            z3 = MULTIPLY(-z3, FIX_0_785694958);

                            tmp0 += z3;
                            tmp1 = z2 + z5;
                            tmp2 += z3;
                            tmp3 = z1 + z5;
                        }
                    } else {
                        if (d1 != 0) {
                            /* d1 != 0, d3 == 0, d5 == 0, d7 != 0 */
                            z1 = d7 + d1;
                            z5 = MULTIPLY(z1, FIX_1_175875602);

                            z1 = MULTIPLY(z1, FIX_0_275899380);
                            z3 = MULTIPLY(-d7, FIX_1_961570560);
                            tmp0 = MULTIPLY(-d7, FIX_1_662939225);
                            z4 = MULTIPLY(-d1, FIX_0_390180644);
                            tmp3 = MULTIPLY(d1, FIX_1_111140466);

                            tmp0 += z1;
                            tmp1 = z4 + z5;
                            tmp2 = z3 + z5;
                            tmp3 += z1;
                        } else {
                            /* d1 == 0, d3 == 0, d5 == 0, d7 != 0 */
                            tmp0 = MULTIPLY(-d7, FIX_1_387039845);
                            tmp1 = MULTIPLY(d7, FIX_1_175875602);
                            tmp2 = MULTIPLY(-d7, FIX_0_785694958);
                            tmp3 = MULTIPLY(d7, FIX_0_275899380);
                        }
                    }
                }
            } else {
                if (d5 != 0) {
                    if (d3 != 0) {
                        if (d1 != 0) {
                            /* d1 != 0, d3 != 0, d5 != 0, d7 == 0 */
                            z2 = d5 + d3;
                            z4 = d5 + d1;
                            z5 = MULTIPLY(d3 + z4, FIX_1_175875602);

                            tmp1 = MULTIPLY(d5, FIX_2_053119869);
                            tmp2 = MULTIPLY(d3, FIX_3_072711026);
                            tmp3 = MULTIPLY(d1, FIX_1_501321110);
                            z1 = MULTIPLY(-d1, FIX_0_899976223);
                            z2 = MULTIPLY(-z2, FIX_2_562915447);
                            z3 = MULTIPLY(-d3, FIX_1_961570560);
                            z4 = MULTIPLY(-z4, FIX_0_390180644);

                            z3 += z5;
                            z4 += z5;

                            tmp0 = z1 + z3;
                            tmp1 += z2 + z4;
                            tmp2 += z2 + z3;
                            tmp3 += z1 + z4;
                        } else {
                            /* d1 == 0, d3 != 0, d5 != 0, d7 == 0 */
                            z2 = d5 + d3;

                            z5 = MULTIPLY(z2, FIX_1_175875602);
                            tmp1 = MULTIPLY(d5, FIX_1_662939225);
                            z4 = MULTIPLY(-d5, FIX_0_390180644);
                            z2 = MULTIPLY(-z2, FIX_1_387039845);
                            tmp2 = MULTIPLY(d3, FIX_1_111140466);
                            z3 = MULTIPLY(-d3, FIX_1_961570560);

                            tmp0 = z3 + z5;
                            tmp1 += z2;
                            tmp2 += z2;
                            tmp3 = z4 + z5;
                        }
                    } else {
                        if (d1 != 0) {
                            /* d1 != 0, d3 == 0, d5 != 0, d7 == 0 */
                            z4 = d5 + d1;

                            z5 = MULTIPLY(z4, FIX_1_175875602);
                            z1 = MULTIPLY(-d1, FIX_0_899976223);
                            tmp3 = MULTIPLY(d1, FIX_0_601344887);
                            tmp1 = MULTIPLY(-d5, FIX_0_509795579);
                            z2 = MULTIPLY(-d5, FIX_2_562915447);
                            z4 = MULTIPLY(z4, FIX_0_785694958);

                            tmp0 = z1 + z5;
                            tmp1 += z4;
                            tmp2 = z2 + z5;
                            tmp3 += z4;
                        } else {
                            /* d1 == 0, d3 == 0, d5 != 0, d7 == 0 */
                            tmp0 = MULTIPLY(d5, FIX_1_175875602);
                            tmp1 = MULTIPLY(d5, FIX_0_275899380);
                            tmp2 = MULTIPLY(-d5, FIX_1_387039845);
                            tmp3 = MULTIPLY(d5, FIX_0_785694958);
                        }
                    }
                } else {
                    if (d3 != 0) {
                        if (d1 != 0) {
                            /* d1 != 0, d3 != 0, d5 == 0, d7 == 0 */
                            z5 = d1 + d3;
                            tmp3 = MULTIPLY(d1, FIX_0_211164243);
                            tmp2 = MULTIPLY(-d3, FIX_1_451774981);
                            z1 = MULTIPLY(d1, FIX_1_061594337);
                            z2 = MULTIPLY(-d3, FIX_2_172734803);
                            z4 = MULTIPLY(z5, FIX_0_785694958);
                            z5 = MULTIPLY(z5, FIX_1_175875602);

                            tmp0 = z1 - z4;
                            tmp1 = z2 + z4;
                            tmp2 += z5;
                            tmp3 += z5;
                        } else {
                            /* d1 == 0, d3 != 0, d5 == 0, d7 == 0 */
                            tmp0 = MULTIPLY(-d3, FIX_0_785694958);
                            tmp1 = MULTIPLY(-d3, FIX_1_387039845);
                            tmp2 = MULTIPLY(-d3, FIX_0_275899380);
                            tmp3 = MULTIPLY(d3, FIX_1_175875602);
                        }
                    } else {
                        if (d1 != 0) {
                            /* d1 != 0, d3 == 0, d5 == 0, d7 == 0 */
                            tmp0 = MULTIPLY(d1, FIX_0_275899380);
                            tmp1 = MULTIPLY(d1, FIX_0_785694958);
                            tmp2 = MULTIPLY(d1, FIX_1_175875602);
                            tmp3 = MULTIPLY(d1, FIX_1_387039845);
                        } else {
                            /* d1 == 0, d3 == 0, d5 == 0, d7 == 0 */
                            tmp0 = tmp1 = tmp2 = tmp3 = 0;
                        }
                    }
                }
            }
            /* Final output stage: inputs are tmp10..tmp13, tmp0..tmp3 */

            dataptr.put(0, DESCALE11(tmp10 + tmp3));
            dataptr.put(7, DESCALE11(tmp10 - tmp3));
            dataptr.put(1, DESCALE11(tmp11 + tmp2));
            dataptr.put(6, DESCALE11(tmp11 - tmp2));
            dataptr.put(2, DESCALE11(tmp12 + tmp1));
            dataptr.put(5, DESCALE11(tmp12 - tmp1));
            dataptr.put(3, DESCALE11(tmp13 + tmp0));
            dataptr.put(4, DESCALE11(tmp13 - tmp0));

            dataptr = advance(dataptr, DCTSIZE); /* advance pointer to next row */
        }

    }

    private static int MULTIPLY(int x, short y) {
        return y * (short) x;
    }

    /**
     * Perform the inverse DCT on one block of coefficients.
     */
    private final static void pass2(ShortBuffer data) {
        int tmp0, tmp1, tmp2, tmp3;
        int tmp10, tmp11, tmp12, tmp13;
        int z1, z2, z3, z4, z5;
        int d0, d1, d2, d3, d4, d5, d6, d7;
        ShortBuffer dataptr = data.duplicate();

        /* Pass 2: process columns. */
        /* Note that we must descale the results by a factor of 8 == 23, */
        /* and also undo the PASS1_BITS scaling. */

        for (int rowctr = DCTSIZE - 1; rowctr >= 0; rowctr--) {
            /*
             * Columns of zeroes can be exploited in the same way as we did with
             * rows. However, the row calculation has created many nonzero AC
             * terms, so the simplification applies less often (typically 5% to
             * 10% of the time). On machines with very fast multiplication, it's
             * possible that the test takes more time than it's worth. In that
             * case this section may be commented out.
             */

            d0 = dataptr.get(DCTSIZE_0);
            d1 = dataptr.get(DCTSIZE_1);
            d2 = dataptr.get(DCTSIZE_2);
            d3 = dataptr.get(DCTSIZE_3);
            d4 = dataptr.get(DCTSIZE_4);
            d5 = dataptr.get(DCTSIZE_5);
            d6 = dataptr.get(DCTSIZE_6);
            d7 = dataptr.get(DCTSIZE_7);

            /* Even part: reverse the even part of the forward DCT. */
            /* The rotator is sqrt(2)c(-6). */
            if (d6 != 0) {
                if (d2 != 0) {
                    /* d0 != 0, d2 != 0, d4 != 0, d6 != 0 */
                    z1 = MULTIPLY(d2 + d6, FIX_0_541196100);
                    tmp2 = z1 + MULTIPLY(-d6, FIX_1_847759065);
                    tmp3 = z1 + MULTIPLY(d2, FIX_0_765366865);

                    tmp0 = (d0 + d4) << CONST_BITS;
                    tmp1 = (d0 - d4) << CONST_BITS;

                    tmp10 = tmp0 + tmp3;
                    tmp13 = tmp0 - tmp3;
                    tmp11 = tmp1 + tmp2;
                    tmp12 = tmp1 - tmp2;
                } else {
                    /* d0 != 0, d2 == 0, d4 != 0, d6 != 0 */
                    tmp2 = MULTIPLY(-d6, FIX_1_306562965);
                    tmp3 = MULTIPLY(d6, FIX_0_541196100);

                    tmp0 = (d0 + d4) << CONST_BITS;
                    tmp1 = (d0 - d4) << CONST_BITS;

                    tmp10 = tmp0 + tmp3;
                    tmp13 = tmp0 - tmp3;
                    tmp11 = tmp1 + tmp2;
                    tmp12 = tmp1 - tmp2;
                }
            } else {
                if (d2 != 0) {
                    /* d0 != 0, d2 != 0, d4 != 0, d6 == 0 */
                    tmp2 = MULTIPLY(d2, FIX_0_541196100);
                    tmp3 = MULTIPLY(d2, FIX_1_306562965);

                    tmp0 = (d0 + d4) << CONST_BITS;
                    tmp1 = (d0 - d4) << CONST_BITS;

                    tmp10 = tmp0 + tmp3;
                    tmp13 = tmp0 - tmp3;
                    tmp11 = tmp1 + tmp2;
                    tmp12 = tmp1 - tmp2;
                } else {
                    /* d0 != 0, d2 == 0, d4 != 0, d6 == 0 */
                    tmp10 = tmp13 = (d0 + d4) << CONST_BITS;
                    tmp11 = tmp12 = (d0 - d4) << CONST_BITS;
                }
            }

            /*
             * Odd part per figure 8; the matrix is unitary and hence its
             * transpose is its inverse. i0..i3 are y7,y5,y3,y1 respectively.
             */
            if (d7 != 0) {
                if (d5 != 0) {
                    if (d3 != 0) {
                        if (d1 != 0) {
                            /* d1 != 0, d3 != 0, d5 != 0, d7 != 0 */
                            z1 = d7 + d1;
                            z2 = d5 + d3;
                            z3 = d7 + d3;
                            z4 = d5 + d1;
                            z5 = MULTIPLY(z3 + z4, FIX_1_175875602);

                            tmp0 = MULTIPLY(d7, FIX_0_298631336);
                            tmp1 = MULTIPLY(d5, FIX_2_053119869);
                            tmp2 = MULTIPLY(d3, FIX_3_072711026);
                            tmp3 = MULTIPLY(d1, FIX_1_501321110);
                            z1 = MULTIPLY(-z1, FIX_0_899976223);
                            z2 = MULTIPLY(-z2, FIX_2_562915447);
                            z3 = MULTIPLY(-z3, FIX_1_961570560);
                            z4 = MULTIPLY(-z4, FIX_0_390180644);

                            z3 += z5;
                            z4 += z5;

                            tmp0 += z1 + z3;
                            tmp1 += z2 + z4;
                            tmp2 += z2 + z3;
                            tmp3 += z1 + z4;
                        } else {
                            /* d1 == 0, d3 != 0, d5 != 0, d7 != 0 */
                            z1 = d7;
                            z2 = d5 + d3;
                            z3 = d7 + d3;
                            z5 = MULTIPLY(z3 + d5, FIX_1_175875602);

                            tmp0 = MULTIPLY(d7, FIX_0_298631336);
                            tmp1 = MULTIPLY(d5, FIX_2_053119869);
                            tmp2 = MULTIPLY(d3, FIX_3_072711026);
                            z1 = MULTIPLY(-d7, FIX_0_899976223);
                            z2 = MULTIPLY(-z2, FIX_2_562915447);
                            z3 = MULTIPLY(-z3, FIX_1_961570560);
                            z4 = MULTIPLY(-d5, FIX_0_390180644);

                            z3 += z5;
                            z4 += z5;

                            tmp0 += z1 + z3;
                            tmp1 += z2 + z4;
                            tmp2 += z2 + z3;
                            tmp3 = z1 + z4;
                        }
                    } else {
                        if (d1 != 0) {
                            /* d1 != 0, d3 == 0, d5 != 0, d7 != 0 */
                            z1 = d7 + d1;
                            z2 = d5;
                            z3 = d7;
                            z4 = d5 + d1;
                            z5 = MULTIPLY(z3 + z4, FIX_1_175875602);

                            tmp0 = MULTIPLY(d7, FIX_0_298631336);
                            tmp1 = MULTIPLY(d5, FIX_2_053119869);
                            tmp3 = MULTIPLY(d1, FIX_1_501321110);
                            z1 = MULTIPLY(-z1, FIX_0_899976223);
                            z2 = MULTIPLY(-d5, FIX_2_562915447);
                            z3 = MULTIPLY(-d7, FIX_1_961570560);
                            z4 = MULTIPLY(-z4, FIX_0_390180644);

                            z3 += z5;
                            z4 += z5;

                            tmp0 += z1 + z3;
                            tmp1 += z2 + z4;
                            tmp2 = z2 + z3;
                            tmp3 += z1 + z4;
                        } else {
                            /* d1 == 0, d3 == 0, d5 != 0, d7 != 0 */
                            tmp0 = MULTIPLY(-d7, FIX_0_601344887);
                            z1 = MULTIPLY(-d7, FIX_0_899976223);
                            z3 = MULTIPLY(-d7, FIX_1_961570560);
                            tmp1 = MULTIPLY(-d5, FIX_0_509795579);
                            z2 = MULTIPLY(-d5, FIX_2_562915447);
                            z4 = MULTIPLY(-d5, FIX_0_390180644);
                            z5 = MULTIPLY(d5 + d7, FIX_1_175875602);

                            z3 += z5;
                            z4 += z5;

                            tmp0 += z3;
                            tmp1 += z4;
                            tmp2 = z2 + z3;
                            tmp3 = z1 + z4;
                        }
                    }
                } else {
                    if (d3 != 0) {
                        if (d1 != 0) {
                            /* d1 != 0, d3 != 0, d5 == 0, d7 != 0 */
                            z1 = d7 + d1;
                            z3 = d7 + d3;
                            z5 = MULTIPLY(z3 + d1, FIX_1_175875602);

                            tmp0 = MULTIPLY(d7, FIX_0_298631336);
                            tmp2 = MULTIPLY(d3, FIX_3_072711026);
                            tmp3 = MULTIPLY(d1, FIX_1_501321110);
                            z1 = MULTIPLY(-z1, FIX_0_899976223);
                            z2 = MULTIPLY(-d3, FIX_2_562915447);
                            z3 = MULTIPLY(-z3, FIX_1_961570560);
                            z4 = MULTIPLY(-d1, FIX_0_390180644);

                            z3 += z5;
                            z4 += z5;

                            tmp0 += z1 + z3;
                            tmp1 = z2 + z4;
                            tmp2 += z2 + z3;
                            tmp3 += z1 + z4;
                        } else {
                            /* d1 == 0, d3 != 0, d5 == 0, d7 != 0 */
                            z3 = d7 + d3;

                            tmp0 = MULTIPLY(-d7, FIX_0_601344887);
                            z1 = MULTIPLY(-d7, FIX_0_899976223);
                            tmp2 = MULTIPLY(d3, FIX_0_509795579);
                            z2 = MULTIPLY(-d3, FIX_2_562915447);
                            z5 = MULTIPLY(z3, FIX_1_175875602);
                            z3 = MULTIPLY(-z3, FIX_0_785694958);

                            tmp0 += z3;
                            tmp1 = z2 + z5;
                            tmp2 += z3;
                            tmp3 = z1 + z5;
                        }
                    } else {
                        if (d1 != 0) {
                            /* d1 != 0, d3 == 0, d5 == 0, d7 != 0 */
                            z1 = d7 + d1;
                            z5 = MULTIPLY(z1, FIX_1_175875602);

                            z1 = MULTIPLY(z1, FIX_0_275899380);
                            z3 = MULTIPLY(-d7, FIX_1_961570560);
                            tmp0 = MULTIPLY(-d7, FIX_1_662939225);
                            z4 = MULTIPLY(-d1, FIX_0_390180644);
                            tmp3 = MULTIPLY(d1, FIX_1_111140466);

                            tmp0 += z1;
                            tmp1 = z4 + z5;
                            tmp2 = z3 + z5;
                            tmp3 += z1;
                        } else {
                            /* d1 == 0, d3 == 0, d5 == 0, d7 != 0 */
                            tmp0 = MULTIPLY(-d7, FIX_1_387039845);
                            tmp1 = MULTIPLY(d7, FIX_1_175875602);
                            tmp2 = MULTIPLY(-d7, FIX_0_785694958);
                            tmp3 = MULTIPLY(d7, FIX_0_275899380);
                        }
                    }
                }
            } else {
                if (d5 != 0) {
                    if (d3 != 0) {
                        if (d1 != 0) {
                            /* d1 != 0, d3 != 0, d5 != 0, d7 == 0 */
                            z2 = d5 + d3;
                            z4 = d5 + d1;
                            z5 = MULTIPLY(d3 + z4, FIX_1_175875602);

                            tmp1 = MULTIPLY(d5, FIX_2_053119869);
                            tmp2 = MULTIPLY(d3, FIX_3_072711026);
                            tmp3 = MULTIPLY(d1, FIX_1_501321110);
                            z1 = MULTIPLY(-d1, FIX_0_899976223);
                            z2 = MULTIPLY(-z2, FIX_2_562915447);
                            z3 = MULTIPLY(-d3, FIX_1_961570560);
                            z4 = MULTIPLY(-z4, FIX_0_390180644);

                            z3 += z5;
                            z4 += z5;

                            tmp0 = z1 + z3;
                            tmp1 += z2 + z4;
                            tmp2 += z2 + z3;
                            tmp3 += z1 + z4;
                        } else {
                            /* d1 == 0, d3 != 0, d5 != 0, d7 == 0 */
                            z2 = d5 + d3;

                            z5 = MULTIPLY(z2, FIX_1_175875602);
                            tmp1 = MULTIPLY(d5, FIX_1_662939225);
                            z4 = MULTIPLY(-d5, FIX_0_390180644);
                            z2 = MULTIPLY(-z2, FIX_1_387039845);
                            tmp2 = MULTIPLY(d3, FIX_1_111140466);
                            z3 = MULTIPLY(-d3, FIX_1_961570560);

                            tmp0 = z3 + z5;
                            tmp1 += z2;
                            tmp2 += z2;
                            tmp3 = z4 + z5;
                        }
                    } else {
                        if (d1 != 0) {
                            /* d1 != 0, d3 == 0, d5 != 0, d7 == 0 */
                            z4 = d5 + d1;

                            z5 = MULTIPLY(z4, FIX_1_175875602);
                            z1 = MULTIPLY(-d1, FIX_0_899976223);
                            tmp3 = MULTIPLY(d1, FIX_0_601344887);
                            tmp1 = MULTIPLY(-d5, FIX_0_509795579);
                            z2 = MULTIPLY(-d5, FIX_2_562915447);
                            z4 = MULTIPLY(z4, FIX_0_785694958);

                            tmp0 = z1 + z5;
                            tmp1 += z4;
                            tmp2 = z2 + z5;
                            tmp3 += z4;
                        } else {
                            /* d1 == 0, d3 == 0, d5 != 0, d7 == 0 */
                            tmp0 = MULTIPLY(d5, FIX_1_175875602);
                            tmp1 = MULTIPLY(d5, FIX_0_275899380);
                            tmp2 = MULTIPLY(-d5, FIX_1_387039845);
                            tmp3 = MULTIPLY(d5, FIX_0_785694958);
                        }
                    }
                } else {
                    if (d3 != 0) {
                        if (d1 != 0) {
                            /* d1 != 0, d3 != 0, d5 == 0, d7 == 0 */
                            z5 = d1 + d3;
                            tmp3 = MULTIPLY(d1, FIX_0_211164243);
                            tmp2 = MULTIPLY(-d3, FIX_1_451774981);
                            z1 = MULTIPLY(d1, FIX_1_061594337);
                            z2 = MULTIPLY(-d3, FIX_2_172734803);
                            z4 = MULTIPLY(z5, FIX_0_785694958);
                            z5 = MULTIPLY(z5, FIX_1_175875602);

                            tmp0 = z1 - z4;
                            tmp1 = z2 + z4;
                            tmp2 += z5;
                            tmp3 += z5;
                        } else {
                            /* d1 == 0, d3 != 0, d5 == 0, d7 == 0 */
                            tmp0 = MULTIPLY(-d3, FIX_0_785694958);
                            tmp1 = MULTIPLY(-d3, FIX_1_387039845);
                            tmp2 = MULTIPLY(-d3, FIX_0_275899380);
                            tmp3 = MULTIPLY(d3, FIX_1_175875602);
                        }
                    } else {
                        if (d1 != 0) {
                            /* d1 != 0, d3 == 0, d5 == 0, d7 == 0 */
                            tmp0 = MULTIPLY(d1, FIX_0_275899380);
                            tmp1 = MULTIPLY(d1, FIX_0_785694958);
                            tmp2 = MULTIPLY(d1, FIX_1_175875602);
                            tmp3 = MULTIPLY(d1, FIX_1_387039845);
                        } else {
                            /* d1 == 0, d3 == 0, d5 == 0, d7 == 0 */
                            tmp0 = tmp1 = tmp2 = tmp3 = 0;
                        }
                    }
                }
            }

            /* Final output stage: inputs are tmp10..tmp13, tmp0..tmp3 */

            dataptr.put(DCTSIZE_0, DESCALE18(tmp10 + tmp3));
            dataptr.put(DCTSIZE_7, DESCALE18(tmp10 - tmp3));
            dataptr.put(DCTSIZE_1, DESCALE18(tmp11 + tmp2));
            dataptr.put(DCTSIZE_6, DESCALE18(tmp11 - tmp2));
            dataptr.put(DCTSIZE_2, DESCALE18(tmp12 + tmp1));
            dataptr.put(DCTSIZE_5, DESCALE18(tmp12 - tmp1));
            dataptr.put(DCTSIZE_3, DESCALE18(tmp13 + tmp0));
            dataptr.put(DCTSIZE_4, DESCALE18(tmp13 - tmp0));

            dataptr = advance(dataptr, 1); /* advance pointer to next column */
        }
    }

    private final static int DESCALE(int x, int n) {
        return ((x) + (1 << ((n) - 1))) >> n;
    }

    private final static short DESCALE11(int x) {
        return (short) ((x + ONEHALF_11) >> 11);
    }

    private final static short DESCALE18(int x) {
        return (short) ((x + ONEHALF_18) >> 18);
    }
}
