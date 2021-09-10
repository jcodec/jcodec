package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.VarianceFNs;
import org.jcodec.codecs.vpx.vp8.data.VarianceResults;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;
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
public class Variance {

    public static final VarianceFNs.VF vpx_variance16x16 = new VarianceFNs.VF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, VarianceResults sse) {
            Variance.variance(src_ptr, src_stride, ref_ptr, ref_stride, sse, 16, 16);
        }
    };
    public static final VarianceFNs.VF vpx_variance16x8 = new VarianceFNs.VF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, VarianceResults sse) {
            Variance.variance(src_ptr, src_stride, ref_ptr, ref_stride, sse, 16, 8);
        }
    };
    public static final VarianceFNs.VF vpx_variance8x16 = new VarianceFNs.VF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, VarianceResults sse) {
            Variance.variance(src_ptr, src_stride, ref_ptr, ref_stride, sse, 8, 16);
        }
    };
    public static final VarianceFNs.VF vpx_variance8x8 = new VarianceFNs.VF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, VarianceResults sse) {
            Variance.variance(src_ptr, src_stride, ref_ptr, ref_stride, sse, 8, 8);
        }
    };
    public static final VarianceFNs.VF vpx_variance4x4 = new VarianceFNs.VF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, VarianceResults sse) {
            Variance.variance(src_ptr, src_stride, ref_ptr, ref_stride, sse, 4, 4);
        }
    };

    public static final VarianceFNs.VF vpx_mse16x16 = new VarianceFNs.VF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, VarianceResults sse) {
            variance(src_ptr, src_stride, ref_ptr, ref_stride, sse, 16, 16, skipVar);
        }
    };
    public static final VarianceFNs.VF vpx_mse16x8 = new VarianceFNs.VF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, VarianceResults sse) {
            variance(src_ptr, src_stride, ref_ptr, ref_stride, sse, 16, 8, skipVar);
        }
    };
    public static final VarianceFNs.VF vpx_mse8x16 = new VarianceFNs.VF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, VarianceResults sse) {
            variance(src_ptr, src_stride, ref_ptr, ref_stride, sse, 8, 16, skipVar);
        }
    };
    public static final VarianceFNs.VF vpx_mse8x8 = new VarianceFNs.VF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, VarianceResults sse) {
            variance(src_ptr, src_stride, ref_ptr, ref_stride, sse, 8, 8, skipVar);
        }
    };
    public static final VarianceFNs.VF vpx_mse4x4 = new VarianceFNs.VF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, VarianceResults sse) {
            variance(src_ptr, src_stride, ref_ptr, ref_stride, sse, 4, 4, skipVar);
        }
    };

    static int vpx_get_mb_ss(final ReadOnlyIntArrPointer src_ptr) {
        int sum = 0;

        for (int i = 0; i < 256; ++i) {
            final int t = src_ptr.getRel(i);
            sum += t * t;
        }

        return sum;
    }

    static int vpx_get4x4sse_cs(final PositionableIntArrPointer src_ptr, final int src_stride,
            final PositionableIntArrPointer ref_ptr, final int ref_stride) {
        VarianceResults sse = new VarianceResults();
        variance(src_ptr, src_stride, ref_ptr, ref_stride, sse, 4, 4, skipVar);
        return sse.sse;
    }

    static void variance(final PositionableIntArrPointer src_ptr, final int src_stride,
            final PositionableIntArrPointer ref_ptr, final int ref_stride, final VarianceResults ret, final int w,
            final int h) {
        variance(src_ptr, src_stride, ref_ptr, ref_stride, ret, w, h, calcVar);
    }

    interface VarCalc {
        void call(VarianceResults ret, int w, int h, long sum);
    }

    private static final VarCalc calcVar = new VarCalc() {
        public void call(VarianceResults ret, int w, int h, long sum) {
            ret.variance = ret.sse - sum * sum / (w * h);
        }
    };

    private static final VarCalc skipVar = new VarCalc() {
        public void call(VarianceResults ret, int w, int h, long sum) {
            ret.variance = Long.MAX_VALUE;
        }
    };

    private static void variance(final PositionableIntArrPointer src_ptr, final int src_stride,
            final PositionableIntArrPointer ref_ptr, final int ref_stride, final VarianceResults ret, final int w,
            final int h, VarCalc varcalc) {
        long sum = 0;
        ret.sse = 0;
        for (int i = 0; i < h; ++i) {
            final int baseSrc = i * src_stride;
            final int baseRef = i * ref_stride;
            for (int j = 0; j < w; ++j) {
                final int diff = src_ptr.getRel(baseSrc + j) - ref_ptr.getRel(baseRef + j);
                sum += diff;
                ret.sse += diff * diff;
            }
        }

        varcalc.call(ret, w, h, sum);
    }
}
