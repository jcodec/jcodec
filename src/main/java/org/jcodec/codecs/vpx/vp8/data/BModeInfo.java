package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;

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
public class BModeInfo {// should be an union...
    public BPredictionMode as_mode() {
        int i = (int) mv.col & 0xF; // Might need to use row if externally used
        for (BPredictionMode b : BPredictionMode.values()) {
            if (i == b.ordinal()) {
                return b;
            }
        }
        return null;
    }

    public void as_mode(BPredictionMode b) {
        mv.col = (short) b.ordinal();
    }

    public MV mv = new MV();
}
