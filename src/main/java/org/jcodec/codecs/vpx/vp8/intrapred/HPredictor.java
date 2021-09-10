package org.jcodec.codecs.vpx.vp8.intrapred;

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
public class HPredictor extends BlockSizeSpecificPredictor {

    public HPredictor(int bs) {
        super(bs);
    }

    @Override
    public void call(final FullAccessIntArrPointer dst, final int stride, final ReadOnlyIntArrPointer above,
            final ReadOnlyIntArrPointer left) {
        for (int r = 0; r < bs; r++) {
            dst.memset(r * stride, left.getRel(r), bs);
        }
    }

}
