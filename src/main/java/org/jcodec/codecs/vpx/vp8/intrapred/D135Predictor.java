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
public class D135Predictor extends BlockSizeSpecificPredictor {
    public D135Predictor(int bs) {
        super(bs);
    }

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        int i;
        FullAccessIntArrPointer border = new FullAccessIntArrPointer(32 + 32 - 1);// outer border from bottom-left
                                                                                  // to top-right
        PositionableIntArrPointer pLeft = PositionableIntArrPointer.makePositionable(left);
        pLeft.incBy(bs);
        // dst(bs, bs - 2)[0], i.e., border starting at bottom-left
        for (i = 0; i < bs - 2; ++i) {
            border.setAndInc(avg3(pLeft.getRel(3 - i), pLeft.getRel(2 - i), pLeft.getRel(1 - i)));
        }
        border.setAndInc(avg3(above.getRel(-1), left.get(), left.getRel(1)));
        border.setAndInc(avg3(left.get(), above.getRel(-1), above.get()));
        border.setAndInc(avg3(above.getRel(-1), above.get(), above.getRel(1)));
        // dst[0][2, size), i.e., remaining top border ascending
        for (i = 0; i < bs - 2; ++i) {
            border.setAndInc(avg3(above.getRel(i), above.getRel(i + 1), above.getRel(i + 2)));
        }
        border.rewind();
        for (i = 0; i < bs; ++i) {
            dst.memcopyin(i * stride, border, bs - 1 - i, bs);
        }
    }

}
