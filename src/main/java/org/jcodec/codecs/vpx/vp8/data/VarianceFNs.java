package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
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
public class VarianceFNs {

    public static interface SDF {
        long call(PositionableIntArrPointer src_ptr, int src_stride, // both ptrs are uint8*
                PositionableIntArrPointer ref_ptr, int ref_stride);
    }

    public static interface VF {
        void call(PositionableIntArrPointer src_ptr, // both ptrs are uint8*
                int src_stride, PositionableIntArrPointer ref_ptr, int ref_stride, VarianceResults sse); // sse was
                                                                                                         // uint*
    }

    public static interface SVF {
        void call(PositionableIntArrPointer src_ptr, // both ptrs are uint8*
                int src_stride, int xoff, int yoff, PositionableIntArrPointer ref_ptr, int ref_stride,
                VarianceResults sse); // sse was uint*
    }

    public static interface SDXF {
        void call(PositionableIntArrPointer src_ptr, int src_stride, PositionableIntArrPointer ref_ptr, int ref_stride,
                int[] sad_array);
    }

    public static interface COPY {
        void call(PositionableIntArrPointer src_ptr, int src_stride, FullAccessIntArrPointer ref_ptr, int ref_stride,
                int n);
    }

    public SDF sdf;
    public VF vf;
    public SVF svf;
    public SDXF sdx3f, sdx8f, sdx4df;
    public COPY copymem;

    public VarianceFNs copy() {
        VarianceFNs n = new VarianceFNs();
        n.sdf = sdf;
        n.vf = vf;
        n.svf = svf;
        n.sdx3f = sdx3f;
        n.sdx8f = sdx8f;
        n.sdx4df = sdx4df;
        n.copymem = copymem;
        return n;
    }
}
