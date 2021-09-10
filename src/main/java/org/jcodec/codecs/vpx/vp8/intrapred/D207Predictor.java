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
public class D207Predictor extends BlockSizeSpecificPredictor {
    public D207Predictor(int bs) {
        super(bs);
    }

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        dst = dst.shallowCopy();
        int r, c;
        // first column
        for (r = 0; r < bs - 1; ++r)
            dst.setRel(r * stride, avg2(left.getRel(r), left.getRel(r + 1)));
        dst.setRel((bs - 1) * stride, left.getRel(bs - 1));
        dst.inc();

        // second column
        for (r = 0; r < bs - 2; ++r)
            dst.setRel(r * stride, avg3(left.getRel(r), left.getRel(r + 1), left.getRel(r + 2)));
        dst.setRel((bs - 2) * stride, avg3(left.getRel(bs - 2), left.getRel(bs - 1), left.getRel(bs - 1)));
        dst.setRel((bs - 1) * stride, left.getRel(bs - 1));
        dst.inc();

        // rest of last row
        for (c = 0; c < bs - 2; ++c)
            dst.setRel((bs - 1) * stride + c, left.getRel(bs - 1));

        for (r = bs - 2; r >= 0; --r)
            for (c = 0; c < bs - 2; ++c)
                dst.setRel(r * stride + c, dst.getRel((r + 1) * stride + c - 2));

    }

}
