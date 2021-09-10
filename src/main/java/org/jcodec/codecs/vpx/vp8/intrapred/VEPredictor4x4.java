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
public class VEPredictor4x4 implements IntraPredFN {

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        dst = dst.shallowCopy();
        PositionableIntArrPointer pAbove = PositionableIntArrPointer.makePositionable(above);
        final short H = pAbove.getRel(-1);
        final short I = pAbove.getAndInc();
        final short J = pAbove.getAndInc();
        final short K = pAbove.getAndInc();
        final short L = pAbove.getAndInc();
        final short M = pAbove.getAndInc();

        dst.set(avg3(H, I, J));
        dst.setRel(1, avg3(I, J, K));
        dst.setRel(2, avg3(J, K, L));
        dst.setRel(3, avg3(K, L, M));
        dst.memcopyin(stride, dst, 0, 4);
        dst.memcopyin(stride * 2, dst, 0, 4);
        dst.memcopyin(stride * 3, dst, 0, 4);

    }

}
