package org.jcodec.codecs.vpx.vp8;

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
public class Subtract {
    static void vpx_subtract_block(final int rows, final int cols, final FullAccessIntArrPointer diff_ptr,
            final int diff_stride, final ReadOnlyIntArrPointer src_ptr, final int src_stride,
            final ReadOnlyIntArrPointer pred_ptr, final int pred_stride) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0, diff = r * diff_stride, src = r * src_stride,
                    pred = r * pred_stride; c < cols; c++, diff++, src++, pred++) {
                diff_ptr.setRel(diff, (short) (src_ptr.getRel(src) - pred_ptr.getRel(pred)));
            }
        }
    }

}
