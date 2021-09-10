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
public abstract class SingleValPredictor extends BlockSizeSpecificPredictor {

    public SingleValPredictor(int bs) {
        super(bs);
    }

    @Override
    public void call(FullAccessIntArrPointer dst, int stride, ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        final short expectedVal = calcSingleValue(above, left);
        for (int r = 0; r < bs; r++) {
            dst.memset(r * stride, expectedVal, bs);
        }
    }

    protected abstract short calcSingleValue(ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left);

}
