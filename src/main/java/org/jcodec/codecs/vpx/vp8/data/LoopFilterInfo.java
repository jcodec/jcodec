package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.enums.FrameType;
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
public class LoopFilterInfo {
    public ReadOnlyIntArrPointer mblim;
    public ReadOnlyIntArrPointer blim;
    public ReadOnlyIntArrPointer lim;
    public ReadOnlyIntArrPointer hev_thr;

    public LoopFilterInfo(final FrameType frame_type, final LoopFilterInfoN lfi_n, final int filter_level) {
        final int hev_index = lfi_n.hev_thr_lut.get(frame_type)[filter_level];
        mblim = new ReadOnlyIntArrPointer(lfi_n.mblim[filter_level], 0);
        blim = new ReadOnlyIntArrPointer(lfi_n.blim[filter_level], 0);
        lim = new ReadOnlyIntArrPointer(lfi_n.lim[filter_level], 0);
        hev_thr = new ReadOnlyIntArrPointer(lfi_n.hev_thr[hev_index], 0);
    }
}
