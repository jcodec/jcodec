package org.jcodec.codecs.vpx.vp8.intrapred;

import static org.jcodec.codecs.vpx.VP8Util.avg2;
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
public class D117Predictor4x4 implements IntraPredFN {

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        PositionableIntArrPointer pLeft = PositionableIntArrPointer.makePositionable(left);
        PositionableIntArrPointer pAbove = PositionableIntArrPointer.makePositionable(above);
        final short I = pLeft.getAndInc();
        final short J = pLeft.getAndInc();
        final short K = pLeft.getAndInc();
        final short X = pAbove.getRel(-1);
        final short A = pAbove.getAndInc();
        final short B = pAbove.getAndInc();
        final short C = pAbove.getAndInc();
        final short D = pAbove.getAndInc();
        dst.set(dst.setRel(1 + 2 * stride, avg2(X, A)));
        dst.setRel(1, dst.setRel(2 + 2 * stride, avg2(A, B)));
        dst.setRel(2, dst.setRel(3 + 2 * stride, avg2(B, C)));
        dst.setRel(3, avg2(C, D));

        dst.setRel(3 * stride, avg3(K, J, I));
        dst.setRel(2 * stride, avg3(J, I, X));
        dst.setRel(stride, dst.setRel(1 + 3 * stride, avg3(I, X, A)));
        dst.setRel(1 + stride, dst.setRel(2 + 3 * stride, avg3(X, A, B)));
        dst.setRel(2 + stride, dst.setRel(3 + 3 * stride, avg3(A, B, C)));
        dst.setRel(3 + stride, avg3(B, C, D));

    }

}
