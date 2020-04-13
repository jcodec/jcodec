package org.jcodec.codecs.h264.encode;

import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Dumb rate control policy, always maintains the same QP for the whole video
 * 
 * @author The JCodec project
 * 
 */
public class DumbRateControl implements RateControl {
    private static final int QP = 20;
    private int bitsPerMb;
    private int totalQpDelta;
    private boolean justSwitched;

    @Override
    public int accept(int bits) {
        if (bits >= bitsPerMb) {
            totalQpDelta ++;
            justSwitched = true;
            return 1;
        } else {
            // Only decrease qp if we got too few bits (more then 12.5%)
            if (totalQpDelta > 0 && !justSwitched && (bitsPerMb - bits > (bitsPerMb >> 3))) {
                -- totalQpDelta;
                justSwitched = true;
                return -1;
            } else {
                justSwitched = false;
            }
            return 0;
        }
    }

    @Override
    public int startPicture(Size sz, int maxSize, SliceType sliceType) {
        int totalMb = ((sz.getWidth() + 15) >> 4) * ((sz.getHeight() + 15) >> 4);
        bitsPerMb = (maxSize << 3) / totalMb;
        totalQpDelta = 0;
        justSwitched = false;
        return QP + (sliceType == SliceType.P ? 6 : 0);
    }

    @Override
    public int initialQpDelta(Picture pic, int mbX, int mbY) {
        return 0;
    }
}
