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
public abstract class AverageBasedPredictor extends SingleValPredictor {
    final int shift;
    final int adj;

    public AverageBasedPredictor(final int bs) {
        super(bs);
        adj = getCount(bs) >> 1;
        shift = Integer.numberOfTrailingZeros(adj) + 1;
    }

    @Override
    protected short calcSingleValue(ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left) {
        int sum = 0;
        for (int i = 0; i < bs; i++) {
            sum += getToSum(above, left, i);
        }
        return (short) ((sum + adj) >> shift);
    }

    protected abstract int getToSum(ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer left, int idx);

    protected abstract int getCount(int bs);

}
