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
public class D207Predictor4x4 implements IntraPredFN {

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        dst = dst.shallowCopy();
        PositionableIntArrPointer pLeft = PositionableIntArrPointer.makePositionable(left);
        final short I = pLeft.getAndInc();
        final short J = pLeft.getAndInc();
        final short K = pLeft.getAndInc();
        final short L = pLeft.getAndInc();
        dst.set(avg2(I, J));
        dst.setRel(2, dst.setRel(stride, avg2(J, K)));
        dst.setRel(2 + stride, dst.setRel(2 * stride, avg2(K, L)));
        dst.setRel(1, avg3(I, J, K));
        dst.setRel(3, dst.setRel(stride + 1, avg3(J, K, L)));
        dst.setRel(3 + stride, dst.setRel(1 + 2 * stride, avg3(K, L, L)));
        dst.setRel(3 + 2 * stride, dst.setRel(2 + 2 * stride, dst.setRel(3 * stride,
                dst.setRel(1 + 3 * stride, dst.setRel(2 + 3 * stride, dst.setRel(3 + 3 * stride, L))))));

    }

}
