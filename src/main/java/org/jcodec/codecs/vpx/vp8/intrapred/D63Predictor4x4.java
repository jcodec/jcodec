package org.jcodec.codecs.vpx.vp8.intrapred;

import static org.jcodec.codecs.vpx.VP8Util.avg2;
import static org.jcodec.codecs.vpx.VP8Util.avg3;

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
public class D63Predictor4x4 implements IntraPredFN {

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        final short A = above.getRel(0);
        final short B = above.getRel(1);
        final short C = above.getRel(2);
        final short D = above.getRel(3);
        final short E = above.getRel(4);
        final short F = above.getRel(5);
        final short G = above.getRel(6);

        vpx_d63_helper(dst, stride, avg2(A, B), avg2(B, C), avg2(C, D), avg2(D, E), avg2(E, F), avg3(A, B, C),
                avg3(B, C, D), avg3(C, D, E), avg3(D, E, F), avg3(E, F, G));
    }

    static void vpx_d63_helper(FullAccessIntArrPointer dst, int stride, short... vals) {
        dst = dst.shallowCopy();
        dst.set(vals[0]);
        dst.setRel(1, dst.setRel(2 * stride, vals[1]));
        dst.setRel(2, dst.setRel(1 + 2 * stride, vals[2]));
        dst.setRel(3, dst.setRel(2 + 2 * stride, vals[3]));
        dst.setRel(3 + 2 * stride, vals[4]);

        dst.setRel(stride, vals[5]);
        dst.setRel(1 + stride, dst.setRel(3 * stride, vals[6]));
        dst.setRel(2 + stride, dst.setRel(1 + 3 * stride, vals[7]));
        dst.setRel(3 + stride, dst.setRel(2 + 3 * stride, vals[8]));
        dst.setRel(3 + 3 * stride, vals[9]);

    }

}
