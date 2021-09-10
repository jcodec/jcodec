package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.VarianceFNs;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;

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
public class Sad {
    public static final VarianceFNs.SDF vpx_sad16x16 = new VarianceFNs.SDF() {
        @Override
        public long call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride) {
            return sad(src_ptr, src_stride, ref_ptr, ref_stride, 16, 16);
        }
    };
    public static final VarianceFNs.SDF vpx_sad16x8 = new VarianceFNs.SDF() {
        @Override
        public long call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride) {
            return sad(src_ptr, src_stride, ref_ptr, ref_stride, 16, 8);
        }
    };

    public static final VarianceFNs.SDF vpx_sad8x16 = new VarianceFNs.SDF() {
        @Override
        public long call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride) {
            return sad(src_ptr, src_stride, ref_ptr, ref_stride, 8, 16);
        }
    };

    public static final VarianceFNs.SDF vpx_sad8x8 = new VarianceFNs.SDF() {
        @Override
        public long call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride) {
            return sad(src_ptr, src_stride, ref_ptr, ref_stride, 8, 8);
        }
    };

    public static final VarianceFNs.SDF vpx_sad4x4 = new VarianceFNs.SDF() {
        @Override
        public long call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride) {
            return sad(src_ptr, src_stride, ref_ptr, ref_stride, 4, 4);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad16x16x3 = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 16, 16, 3);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad16x8x3 = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 16, 8, 3);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad8x16x3 = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 8, 16, 3);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad8x8x3 = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 8, 8, 3);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad4x4x3 = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 4, 4, 3);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad16x16x8 = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 16, 16, 8);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad16x8x8 = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 16, 8, 8);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad8x16x8 = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 8, 16, 8);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad8x8x8 = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 8, 8, 8);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad4x4x8 = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 4, 4, 8);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad16x16x4d = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 16, 16, 4);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad16x8x4d = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 16, 8, 4);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad8x16x4d = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 8, 16, 4);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad8x8x4d = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 8, 8, 4);
        }
    };

    public static final VarianceFNs.SDXF vpx_sad4x4x4d = new VarianceFNs.SDXF() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr,
                int ref_stride, int[] sad_array) {
            sadToArray(src_ptr, src_stride, ref_ptr, ref_stride, sad_array, 4, 4, 4);
        }
    };

    static int sad(final PositionableIntArrPointer src_ptr, final int src_stride,
            final PositionableIntArrPointer ref_ptr, final int ref_stride, final int width, final int height) {
        int sad = 0;
        for (int y = 0; y < height; y++) {
            final int basesrc = y * src_stride;
            final int refsrc = y * ref_stride;
            for (int x = 0; x < width; x++)
                sad += Math.abs(src_ptr.getRel(basesrc + x) - ref_ptr.getRel(refsrc + x));
        }
        return sad;
    }

    static void sadToArray(final PositionableIntArrPointer src_ptr, final int src_stride,
            final PositionableIntArrPointer ref_ptr, final int ref_stride, final int[] sad_array, final int width,
            final int height, final int k) {
        for (int i = 0; i < k; i++) {
            sad_array[i] = sad(src_ptr, src_stride, PositionableIntArrPointer.makePositionableAndInc(ref_ptr, i),
                    ref_stride, width, height);
        }
    }

}
