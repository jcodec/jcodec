package org.jcodec.common.dct

import java.nio.IntBuffer

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author Alex Zhukov
 */
class IntDCT : DCT {
    override fun decode(orig: IntArray): IntArray {
        val inptr = IntBuffer.wrap(orig)
        val workspace = IntBuffer.allocate(64)
        val outptr = IntBuffer.allocate(64)
        doDecode(inptr, workspace, outptr)
        return outptr.array()
    }

    companion object {
        @JvmField
        val INSTANCE = IntDCT()
        private const val DCTSIZE = 8
        private const val PASS1_BITS = 2
        private const val MAXJSAMPLE = 255
        private const val CENTERJSAMPLE = 128

        /** 2 bits wider than legal samples  */
        private const val RANGE_MASK = MAXJSAMPLE * 4 + 3
        @JvmStatic
        fun doDecode(inptr: IntBuffer, workspace: IntBuffer,
                     outptr: IntBuffer): IntBuffer {
            pass1(inptr, workspace.duplicate())
            pass2(outptr, workspace.duplicate())
            return outptr
        }

        /**
         * Pass 2: process rows from work array, store into output array.
         *
         * Note that we must descale the results by a factor of 8 == 23, and also
         * undo the PASS1_BITS scaling.
         */
        private fun pass2(outptr: IntBuffer, wsptr: IntBuffer) {
            var wsptr = wsptr
            for (ctr in 0 until DCTSIZE) {
                /* Even part: reverse the even part of the forward DCT. */
                /* The rotator is sqrt(2)c(-6). */
                var z2 = wsptr[2]
                var z3 = wsptr[6]
                var z1 = MULTIPLY(z2 + z3, FIX_0_541196100)
                var tmp2 = z1 + MULTIPLY(z3, -FIX_1_847759065)
                var tmp3 = z1 + MULTIPLY(z2, FIX_0_765366865)
                var tmp0 = wsptr[0] + wsptr[4] shl CONST_BITS
                var tmp1 = wsptr[0] - wsptr[4] shl CONST_BITS
                val tmp10 = tmp0 + tmp3
                val tmp13 = tmp0 - tmp3
                val tmp11 = tmp1 + tmp2
                val tmp12 = tmp1 - tmp2

                /*
             * Odd part per figure 8; the matrix is unitary and hence its
             * transpose is its inverse. i0..i3 are y7,y5,y3,y1 respectively.
             */tmp0 = wsptr[7]
                tmp1 = wsptr[5]
                tmp2 = wsptr[3]
                tmp3 = wsptr[1]
                z1 = tmp0 + tmp3
                z2 = tmp1 + tmp2
                z3 = tmp0 + tmp2
                var z4 = tmp1 + tmp3
                val z5 = MULTIPLY(z3 + z4, FIX_1_175875602) /* sqrt(2) c3 */
                tmp0 = MULTIPLY(tmp0, FIX_0_298631336) /* sqrt(2) (-c1+c3+c5-c7) */
                tmp1 = MULTIPLY(tmp1, FIX_2_053119869) /* sqrt(2) ( c1+c3-c5+c7) */
                tmp2 = MULTIPLY(tmp2, FIX_3_072711026) /* sqrt(2) ( c1+c3+c5-c7) */
                tmp3 = MULTIPLY(tmp3, FIX_1_501321110) /* sqrt(2) ( c1+c3-c5-c7) */
                z1 = MULTIPLY(z1, -FIX_0_899976223) /* sqrt(2) (c7-c3) */
                z2 = MULTIPLY(z2, -FIX_2_562915447) /* sqrt(2) (-c1-c3) */
                z3 = MULTIPLY(z3, -FIX_1_961570560) /* sqrt(2) (-c3-c5) */
                z4 = MULTIPLY(z4, -FIX_0_390180644) /* sqrt(2) (c5-c3) */
                z3 += z5
                z4 += z5
                tmp0 += z1 + z3
                tmp1 += z2 + z4
                tmp2 += z2 + z3
                tmp3 += z1 + z4

                /* Final output stage: inputs are tmp10..tmp13, tmp0..tmp3 */
                val D = CONST_BITS + PASS1_BITS + 3
                outptr.put(range_limit(DESCALE(tmp10 + tmp3, D) and RANGE_MASK))
                outptr.put(range_limit(DESCALE(tmp11 + tmp2, D) and RANGE_MASK))
                outptr.put(range_limit(DESCALE(tmp12 + tmp1, D) and RANGE_MASK))
                outptr.put(range_limit(DESCALE(tmp13 + tmp0, D) and RANGE_MASK))
                outptr.put(range_limit(DESCALE(tmp13 - tmp0, D) and RANGE_MASK))
                outptr.put(range_limit(DESCALE(tmp12 - tmp1, D) and RANGE_MASK))
                outptr.put(range_limit(DESCALE(tmp11 - tmp2, D) and RANGE_MASK))
                outptr.put(range_limit(DESCALE(tmp10 - tmp3, D) and RANGE_MASK))
                wsptr = doAdvance(wsptr, DCTSIZE) /* advance pointer to next row */
            }
        }

        @JvmStatic
        fun range_limit(i: Int): Int {
            return idct_sample_range_limit[i + 256]
        }

        private val sample_range_limit = IntBuffer
                .allocate(5 * (MAXJSAMPLE + 1) + CENTERJSAMPLE)
        private val idct_sample_range_limit = IntBuffer
                .allocate(sample_range_limit.capacity() - 128)

        /* Allocate and fill in the sample_range_limit table */
        private fun prepare_range_limit_table() {
            sample_range_limit.position(256)
            for (i in 0..127) {
                sample_range_limit.put(i)
            }
            for (i in -128..-1) {
                sample_range_limit.put(i)
            }
            for (i in 0 until 256 + 128) {
                sample_range_limit.put(-1)
            }
            for (i in 0 until 256 + 128) {
                sample_range_limit.put(0)
            }
            for (i in 0..127) {
                sample_range_limit.put(i)
            }
            for (i in 0 until idct_sample_range_limit.capacity()) {
                idct_sample_range_limit.put(sample_range_limit[i + 128] and 0xff)
            }
        }

        private fun shortcut(inptr: IntBuffer, wsptr: IntBuffer): Boolean {
            /*
         * Due to quantization, we will usually find that many of the input
         * coefficients are zero, especially the AC terms. We can exploit this
         * by short-circuiting the IDCT calculation for any column in which all
         * the AC terms are zero. In that case each output is equal to the DC
         * coefficient (with scale factor as needed). With typical images and
         * quantization tables, half or more of the column DCT calculations can
         * be simplified this way.
         */
            var inptr = inptr
            var wsptr = wsptr
            if (inptr[DCTSIZE * 1] == 0 && inptr[DCTSIZE * 2] == 0 && inptr[DCTSIZE * 3] == 0 && inptr[DCTSIZE * 4] == 0 && inptr[DCTSIZE * 5] == 0 && inptr[DCTSIZE * 6] == 0 && inptr[DCTSIZE * 7] == 0) {
                /* AC terms all zero */
                val dcval = inptr[DCTSIZE * 0] shl PASS1_BITS
                wsptr.put(DCTSIZE * 0, dcval)
                wsptr.put(DCTSIZE * 1, dcval)
                wsptr.put(DCTSIZE * 2, dcval)
                wsptr.put(DCTSIZE * 3, dcval)
                wsptr.put(DCTSIZE * 4, dcval)
                wsptr.put(DCTSIZE * 5, dcval)
                wsptr.put(DCTSIZE * 6, dcval)
                wsptr.put(DCTSIZE * 7, dcval)
                inptr = advance(inptr)
                wsptr = advance(wsptr)
                return true
            }
            return false
        }

        /**
         * Pass 1: process columns from input, store into work array.
         *
         * Note results are scaled up by sqrt(8) compared to a true IDCT;
         * furthermore, we scale the results by 2xPASS1_BITS.
         */
        private fun pass1(inptr: IntBuffer, wsptr: IntBuffer) {
            var inptr = inptr
            var wsptr = wsptr
            for (ctr in DCTSIZE downTo 1) {
                // if (shortcircuit(inptr, wsptr))
                // continue;
                /* Even part: reverse the even part of the forward DCT. */
                /* The rotator is sqrt(2)c(-6). */
                var z2 = inptr[DCTSIZE * 2]
                var z3 = inptr[DCTSIZE * 6]
                var z1 = MULTIPLY(z2 + z3, FIX_0_541196100)
                var tmp2 = z1 + MULTIPLY(z3, -FIX_1_847759065)
                var tmp3 = z1 + MULTIPLY(z2, FIX_0_765366865)
                z2 = inptr[DCTSIZE * 0]
                z3 = inptr[DCTSIZE * 4]
                var tmp0 = z2 + z3 shl CONST_BITS
                var tmp1 = z2 - z3 shl CONST_BITS
                val tmp10 = tmp0 + tmp3
                val tmp13 = tmp0 - tmp3
                val tmp11 = tmp1 + tmp2
                val tmp12 = tmp1 - tmp2
                /*
             * Odd part per figure 8; the matrix is unitary and hence its
             * transpose is its inverse. i0..i3 are y7,y5,y3,y1 respectively.
             */tmp0 = inptr[DCTSIZE * 7]
                tmp1 = inptr[DCTSIZE * 5]
                tmp2 = inptr[DCTSIZE * 3]
                tmp3 = inptr[DCTSIZE * 1]
                z1 = tmp0 + tmp3
                z2 = tmp1 + tmp2
                z3 = tmp0 + tmp2
                var z4 = tmp1 + tmp3
                val z5 = MULTIPLY(z3 + z4, FIX_1_175875602) /* sqrt(2) c3 */
                tmp0 = MULTIPLY(tmp0, FIX_0_298631336) /* sqrt(2) (-c1+c3+c5-c7) */
                tmp1 = MULTIPLY(tmp1, FIX_2_053119869) /* sqrt(2) ( c1+c3-c5+c7) */
                tmp2 = MULTIPLY(tmp2, FIX_3_072711026) /* sqrt(2) ( c1+c3+c5-c7) */
                tmp3 = MULTIPLY(tmp3, FIX_1_501321110) /* sqrt(2) ( c1+c3-c5-c7) */
                z1 = MULTIPLY(z1, -FIX_0_899976223) /* sqrt(2) (c7-c3) */
                z2 = MULTIPLY(z2, -FIX_2_562915447) /* sqrt(2) (-c1-c3) */
                z3 = MULTIPLY(z3, -FIX_1_961570560) /* sqrt(2) (-c3-c5) */
                z4 = MULTIPLY(z4, -FIX_0_390180644) /* sqrt(2) (c5-c3) */
                z3 += z5
                z4 += z5
                tmp0 += z1 + z3
                tmp1 += z2 + z4
                tmp2 += z2 + z3
                tmp3 += z1 + z4

                /* Final output stage: inputs are tmp10..tmp13, tmp0..tmp3 */
                val D = CONST_BITS - PASS1_BITS
                wsptr.put(DCTSIZE * 0, DESCALE(tmp10 + tmp3, D))
                wsptr.put(DCTSIZE * 7, DESCALE(tmp10 - tmp3, D))
                wsptr.put(DCTSIZE * 1, DESCALE(tmp11 + tmp2, D))
                wsptr.put(DCTSIZE * 6, DESCALE(tmp11 - tmp2, D))
                wsptr.put(DCTSIZE * 2, DESCALE(tmp12 + tmp1, D))
                wsptr.put(DCTSIZE * 5, DESCALE(tmp12 - tmp1, D))
                wsptr.put(DCTSIZE * 3, DESCALE(tmp13 + tmp0, D))
                wsptr.put(DCTSIZE * 4, DESCALE(tmp13 - tmp0, D))
                inptr = advance(inptr)
                wsptr = advance(wsptr)
            }
        }

        /**
         * advance pointers to next column
         */
        private fun advance(ptr: IntBuffer): IntBuffer {
            return doAdvance(ptr, 1)
        }

        private fun doAdvance(ptr: IntBuffer, size: Int): IntBuffer {
            ptr.position(ptr.position() + size)
            return ptr.slice()
        }

        @JvmStatic
        fun DESCALE(x: Int, n: Int): Int {
            return RIGHT_SHIFT(x + (1 shl n - 1), n)
        }

        private fun RIGHT_SHIFT(x: Int, shft: Int): Int {
            return x shr shft
        }

        private fun MULTIPLY(i: Int, j: Int): Int {
            return i * j
        }

        private const val CONST_BITS = 13
        private const val ONE_HALF = 1 shl CONST_BITS - 1
        private fun FIX(x: Double): Int {
            return (x * (1 shl CONST_BITS) + 0.5).toInt()
        }

        private val FIX_0_298631336 = FIX(0.298631336)
        private val FIX_0_390180644 = FIX(0.390180644)
        private val FIX_0_541196100 = FIX(0.541196100)
        private val FIX_0_765366865 = FIX(0.765366865)
        private val FIX_0_899976223 = FIX(0.899976223)
        private val FIX_1_175875602 = FIX(1.175875602)
        private val FIX_1_501321110 = FIX(1.501321110)
        private val FIX_1_847759065 = FIX(1.847759065)
        private val FIX_1_961570560 = FIX(1.961570560)
        private val FIX_2_053119869 = FIX(2.053119869)
        private val FIX_2_562915447 = FIX(2.562915447)
        private val FIX_3_072711026 = FIX(3.072711026)

        init {
            prepare_range_limit_table()
        }
    }
}