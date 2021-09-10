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
public class D63Predictor extends BlockSizeSpecificPredictor {
    public D63Predictor(int bs) {
        super(bs);
    }

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        int r, c;
        int size;
        for (c = 0; c < bs; ++c) {
            dst.setRel(c, avg2(above.getRel(c), above.getRel(c + 1)));
            dst.setRel(stride + c, avg3(above.getRel(c), above.getRel(c + 1), above.getRel(c + 2)));
        }
        for (r = 2, size = bs - 2; r < bs; r += 2, --size) {
            dst.memcopyin((r + 0) * stride, dst, r >> 1, size);
            dst.memset((r + 0) * stride + size, above.getRel(bs - 1), bs - size);
            dst.memcopyin((r + 1) * stride, dst, stride + (r >> 1), size);
            dst.memset((r + 1) * stride + size, above.getRel(bs - 1), bs - size);
        }

    }

}
