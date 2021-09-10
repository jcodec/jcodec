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
public class D63EPredictor4x4 implements IntraPredFN {

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        final short A = above.getRel(0);
        final short B = above.getRel(1);
        final short C = above.getRel(2);
        final short D = above.getRel(3);
        final short E = above.getRel(4);
        final short F = above.getRel(5);
        final short G = above.getRel(6);
        final short H = above.getRel(7);
        D63Predictor4x4.vpx_d63_helper(dst, stride, avg2(A, B), avg2(B, C), avg2(C, D), avg2(D, E), avg3(E, F, G),
                avg3(A, B, C), avg3(B, C, D), avg3(C, D, E), avg3(D, E, F), avg3(F, G, H));
    }

}
