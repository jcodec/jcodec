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
public class D117Predictor extends BlockSizeSpecificPredictor {

    public D117Predictor(int bs) {
        super(bs);
    }

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        dst = dst.shallowCopy();
        int r, c;

        // first row
        for (c = 0; c < bs; c++)
            dst.setRel(c, avg2(above.getRel(c - 1), above.getRel(c)));
        dst.incBy(stride);

        // second row
        dst.set(avg3(left.get(), above.getRel(-1), above.get()));
        for (c = 1; c < bs; c++)
            dst.setRel(c, avg3(above.getRel(c - 2), above.getRel(c - 1), above.getRel(c)));
        dst.incBy(stride);

        // the rest of first col
        dst.set(avg3(above.getRel(-1), left.get(), left.getRel(1)));
        for (r = 3; r < bs; ++r)
            dst.setRel((r - 2) * stride, avg3(left.getRel(r - 3), left.getRel(r - 2), left.getRel(r - 1)));

        // the rest of the block
        for (r = 2; r < bs; ++r) {
            for (c = 1; c < bs; c++)
                dst.setRel(c, dst.getRel(-2 * stride + c - 1));
            dst.incBy(stride);
        }

    }

}
