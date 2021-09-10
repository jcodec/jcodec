package org.jcodec.codecs.vpx.vp8.intrapred;

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
public class D45Predictor extends BlockSizeSpecificPredictor {

    public D45Predictor(int bs) {
        super(bs);
    }

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        dst = dst.shallowCopy();
        final short above_right = above.getRel(bs - 1);
        ReadOnlyIntArrPointer dst_row0 = dst.readOnly();
        int x, size;

        for (x = 0; x < bs - 1; ++x) {
            dst.setRel(x, avg3(above.getRel(x), above.getRel(x + 1), above.getRel(x + 2)));
        }
        dst.setRel(bs - 1, above_right);
        dst.incBy(stride);
        for (x = 1, size = bs - 2; x < bs; ++x, --size) {
            dst.memcopyin(0, dst_row0, x, size);
            dst.memset(size, above_right, x + 1);
            dst.incBy(stride);
        }
    }

}
