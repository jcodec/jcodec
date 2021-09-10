package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.Block;
import org.jcodec.codecs.vpx.vp8.data.MV;
import org.jcodec.codecs.vpx.vp8.data.VarianceFNs;
import org.jcodec.codecs.vpx.vp8.data.VarianceResults;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.ReadOnlyIntArrPointer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
class SearchForBetterMV {

    public static final MV[] neighbors = { new MV(0, -2), new MV(0, 2), new MV(-2, 0), new MV(2, 0) };
    public static final MV[] diagonals = { new MV(-2, -2), new MV(-2, 2), new MV(2, -2), new MV(2, 2) };

    static final int[] iters = { 4, 4 }; // half first then quarter

    int tr, tc;
    int minc, maxc;
    int minr, maxr;
    int rr, rc;
    int error_per_bit;
    VarianceFNs vfp;
    ReadOnlyIntArrPointer[] mvcost;
    FullAccessIntArrPointer y, z;
    Block b;
    int y_stride = 32, offset;
    long besterr;
    short br, bc;
    VarianceResults sse = new VarianceResults(), sse1 = new VarianceResults();
    long[] results = new long[neighbors.length];

    void moveToBest() {
        tr = br;
        tc = bc;
    }

    void lookAround() {
        for (short divisor = 1; divisor < 3; divisor++) { // divisor 1 => half pel, 2 => quarter pel
            for (int i = 0; i < iters[divisor - 1]; i++) {
                for (int j = 0; j < neighbors.length; j++) {
                    results[j] = checkbetter(neighbors[j], divisor);
                }
                int whichdir = (results[0] < results[1] ? 0 : 1) + (results[2] < results[3] ? 0 : 2);
                checkbetter(diagonals[whichdir], divisor);
                if (tr == br && tc == bc)
                    break;
                moveToBest();
            }
        }
    }

    void saveIfBetter(long v, short r, short c) {
        if (v < besterr) {
            besterr = v;
            br = r;
            bc = c;
            sse1.variance = sse.variance;
            sse1.sse = sse.sse;
        }
    }

    long actualCheck(FullAccessIntArrPointer y, int xO, int yO, short r, short c) {
        vfp.svf.call(y, y_stride, xO, yO, z, b.src_stride, sse);
        long v = sse.variance + MComp.mv_err_cost(r - rr, c - rc, mvcost, error_per_bit);
        saveIfBetter(v, r, c);
        return v;
    }

    long checkbetter(MV direction, short divisor) {
        short c = (short) (tc - direction.col / divisor);
        short r = (short) (tr - direction.row / divisor);
        long v;
        if (c >= minc && c <= maxc && r >= minr && r <= maxr) {
            v = actualCheck(y.shallowCopyWithPosInc((r >> 2) * y_stride + (c >> 2) - offset), (c & 3) << 1,
                    (r & 3) << 1, r, c);
        } else {
            v = Integer.MAX_VALUE;
        }
        return v;
    }
}