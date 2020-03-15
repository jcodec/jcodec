package org.jcodec.codecs.h264.encode;

import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.model.Size;

public class CQPRateControl implements RateControl {
    
    private int qp;

    public CQPRateControl(int qp) {
        this.qp = qp;
    }

    @Override
    public int startPicture(Size sz, int maxSize, SliceType sliceType) {
        return Math.max(10, qp - (sliceType == SliceType.I ? 8 : 0));
    }

    @Override
    public int initialQpDelta() {
        return 0;
    }

    @Override
    public int accept(int bits) {
        return 0;
    }
}
