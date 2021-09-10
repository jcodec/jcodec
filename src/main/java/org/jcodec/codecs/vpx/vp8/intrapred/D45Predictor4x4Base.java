package org.jcodec.codecs.vpx.vp8.intrapred;

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
public class D45Predictor4x4Base implements IntraPredFN {

    private final boolean avgCalc;

    public D45Predictor4x4Base(boolean avgCalc) {
        this.avgCalc = avgCalc;
    }

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
        dst.set(avg3(A, B, C));
        dst.setRel(1, dst.setRel(stride, avg3(B, C, D)));
        dst.setRel(2, dst.setRel(1 + stride, dst.setRel(2 * stride, avg3(C, D, E))));
        dst.setRel(3, dst.setRel(2 + stride, dst.setRel(1 + 2 * stride, dst.setRel(3 * stride, avg3(D, E, F)))));
        dst.setRel(3 + stride, dst.setRel(2 + 2 * stride, dst.setRel(1 + 3 * stride, avg3(E, F, G))));
        dst.setRel(3 + 2 * stride, dst.setRel(2 + 3 * stride, avg3(F, G, H)));
        dst.setRel(3 + 3 * stride, avgCalc ? avg3(G, H, H) : H); // differs from vp8
    }

}
