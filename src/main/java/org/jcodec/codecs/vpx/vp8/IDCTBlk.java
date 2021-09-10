package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;
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
public class IDCTBlk {

    static void vp8_dequant_idct_add_core(final int imax, final int jmax, PositionableIntArrPointer eobs,
            FullAccessIntArrPointer q, ReadOnlyIntArrPointer dq, FullAccessIntArrPointer dst, int stride) {
        final int dstinc = (stride - jmax) << 2;
        for (int i = 0; i < imax; ++i) {
            for (int j = 0; j < jmax; ++j) {
                if (eobs.get() > 1) {
                    Dequantize.vp8_dequant_idct_add(q, dq, dst, stride);
                } else {
                    IDCTllm.vp8_dc_only_idct_add(q.get() * dq.get(), dst, stride, dst, stride);
                    q.memset(0, (short) 0, 2);
                }
                eobs.inc();

                q.incBy(16);
                dst.incBy(4);
            }
            dst.incBy(dstinc);
        }
    }

    static void vp8_dequant_idct_add_y_block(FullAccessIntArrPointer q, ReadOnlyIntArrPointer dq,
            FullAccessIntArrPointer dst, int stride, PositionableIntArrPointer eobs) {
        final int ep = eobs.getPos();
        final int qp = q.getPos();
        final int dp = dst.getPos();
        vp8_dequant_idct_add_core(4, 4, eobs, q, dq, dst, stride);
        eobs.setPos(ep);
        q.setPos(qp);
        dst.setPos(dp);
    }

    static void vp8_dequant_idct_add_uv_block(FullAccessIntArrPointer q, ReadOnlyIntArrPointer dq,
            FullAccessIntArrPointer dst_u, FullAccessIntArrPointer dst_v, int stride, PositionableIntArrPointer eobs) {
        final int ep = eobs.getPos();
        final int qp = q.getPos();
        final int dup = dst_u.getPos();
        final int dvp = dst_v.getPos();

        vp8_dequant_idct_add_core(2, 2, eobs, q, dq, dst_u, stride);
        vp8_dequant_idct_add_core(2, 2, eobs, q, dq, dst_v, stride);

        eobs.setPos(ep);
        q.setPos(qp);
        dst_u.setPos(dup);
        dst_v.setPos(dvp);
    }

}
