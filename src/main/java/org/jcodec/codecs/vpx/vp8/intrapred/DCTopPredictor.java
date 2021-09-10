package org.jcodec.codecs.vpx.vp8.intrapred;

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
public class DCTopPredictor extends AverageBasedPredictor {
    public DCTopPredictor(int bs) {
        super(bs);
    }

    @Override
    protected int getToSum(final ReadOnlyIntArrPointer above, final ReadOnlyIntArrPointer left, final int idx) {
        return above.getRel(idx);
    }

    @Override
    protected int getCount(int bs) {
        return bs;
    }
}
