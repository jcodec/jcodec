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
public class D153Predictor extends BlockSizeSpecificPredictor {

    public D153Predictor(int bs) {
        super(bs);
    }

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        dst = dst.shallowCopy();
        int r, c;
        dst.set(avg2(above.getRel(-1), left.get()));
        for (r = 1; r < bs; r++)
            dst.setRel(r * stride, avg2(left.getRel(r - 1), left.getRel(r)));
        dst.inc();

        dst.set(avg3(left.get(), above.getRel(-1), above.get()));
        dst.setRel(stride, avg3(above.getRel(-1), left.get(), left.getRel(1)));
        for (r = 2; r < bs; r++)
            dst.setRel(r * stride, avg3(left.getRel(r - 2), left.getRel(r - 1), left.getRel(r)));
        dst.inc();

        for (c = 0; c < bs - 2; c++)
            dst.setRel(c, avg3(above.getRel(c - 1), above.getRel(c), above.getRel(c + 1)));
        dst.incBy(stride);

        for (r = 1; r < bs; ++r) {
            for (c = 0; c < bs - 2; c++)
                dst.setRel(c, dst.getRel(-stride + c - 2));
            dst.incBy(stride);
        }
    }

}
