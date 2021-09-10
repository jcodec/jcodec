package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
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
public class InvTrans {
    static void eob_adjust(final FullAccessIntArrPointer eobs, final PositionableIntArrPointer diff) {
        /* eob adjust.... the idct can only skip if both the dc and eob are zero */
        diff.savePos();
        for (int js = 0; js < 16; ++js) {
            if ((eobs.getRel(js) == 0) && (diff.get() != 0))
                eobs.setRel(js, (short) 1);
            diff.incBy(16);
        }
        diff.rewindToSaved();
    }

    static void vp8_inverse_transform_mby(final MacroblockD xd) {
        final ReadOnlyIntArrPointer DQC;

        if (xd.mode_info_context.get().mbmi.mode != MBPredictionMode.SPLITMV) {
            /* do 2nd order transform on the dc block */
            if (xd.eobs.getRel(24) > 1) {
                IDCTllm.vp8_short_inv_walsh4x4(xd.block.getRel(24).dqcoeff, xd.qcoeff);
            } else {
                IDCTllm.vp8_short_inv_walsh4x4_1(xd.block.getRel(24).dqcoeff, xd.qcoeff);
            }
            eob_adjust(xd.eobs, xd.qcoeff);

            DQC = xd.dequant_y1_dc;
        } else {
            DQC = xd.dequant_y1;
        }

        IDCTBlk.vp8_dequant_idct_add_y_block(xd.qcoeff, DQC, xd.dst.y_buffer, xd.dst.y_stride, xd.eobs);
    }

}
