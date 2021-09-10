package org.jcodec.codecs.vpx.vp8.subpixfns;

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
public interface SubpixFN {
    // uint* src, dst_ptr
    void call(ReadOnlyIntArrPointer src, int src_pixels_per_line, int xoffset, int yoffset,
            FullAccessIntArrPointer dst_ptr, int dst_pitch);
}