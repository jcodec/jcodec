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
public class HEPredictor4x4 implements IntraPredFN {

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        dst = dst.shallowCopy();
        PositionableIntArrPointer pLeft = PositionableIntArrPointer.makePositionable(left);
        final short H = above.getRel(-1);
        final short I = pLeft.getAndInc();
        final short J = pLeft.getAndInc();
        final short K = pLeft.getAndInc();
        final short L = pLeft.getAndInc();

        dst.memset(0, avg3(H, I, J), 4);
        dst.memset(stride, avg3(I, J, K), 4);
        dst.memset(stride * 2, avg3(J, K, L), 4);
        dst.memset(stride * 3, avg3(K, L, L), 4);
    }

}
