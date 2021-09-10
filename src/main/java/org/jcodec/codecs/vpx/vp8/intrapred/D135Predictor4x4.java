package org.jcodec.codecs.vpx.vp8.intrapred;

import static org.jcodec.codecs.vpx.VP8Util.avg3;

import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
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
public class D135Predictor4x4 implements IntraPredFN {

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        PositionableIntArrPointer pLeft = PositionableIntArrPointer.makePositionable(left);
        PositionableIntArrPointer pAbove = PositionableIntArrPointer.makePositionable(above);
        final short I = pLeft.getAndInc();
        final short J = pLeft.getAndInc();
        final short K = pLeft.getAndInc();
        final short L = pLeft.getAndInc();
        final short X = pAbove.getRel(-1);
        final short A = pAbove.getAndInc();
        final short B = pAbove.getAndInc();
        final short C = pAbove.getAndInc();
        final short D = pAbove.getAndInc();
        dst.setRel(3 * stride, avg3(J, K, L));
        dst.setRel(1 + 3 * stride, dst.setRel(2 * stride, avg3(I, J, K)));
        dst.setRel(2 + 3 * stride, dst.setRel(1 + 2 * stride, dst.setRel(stride, avg3(X, I, J))));
        dst.setRel(3 + 3 * stride, dst.setRel(2 + 2 * stride, dst.setRel(1 + stride, dst.set(avg3(A, X, I)))));
        dst.setRel(3 + 2 * stride, dst.setRel(2 + stride, dst.setRel(1, avg3(B, A, X))));
        dst.setRel(3 + stride, dst.setRel(2, avg3(C, B, A)));
        dst.setRel(3, avg3(D, C, B));
    }

}
