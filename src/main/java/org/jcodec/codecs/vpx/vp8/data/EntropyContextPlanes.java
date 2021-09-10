package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.CommonUtils;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;

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
public class EntropyContextPlanes {
    public final FullAccessIntArrPointer panes;

    public EntropyContextPlanes() {
        // 4 ints of y1, 2-2 ints of u and v, and one int of y2.
        panes = new FullAccessIntArrPointer(9);
        reset();
    }

    public void reset() {
        CommonUtils.vp8_zero(panes);
    }

    public EntropyContextPlanes(EntropyContextPlanes other) {
        panes = other.panes.deepCopy();
    }
}
