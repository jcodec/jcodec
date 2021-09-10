package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.Macroblock;
import org.jcodec.codecs.vpx.vp8.data.ModeInfo;
import org.jcodec.codecs.vpx.vp8.data.CommonData;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.enums.FrameType;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessGenArrPointer;

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
public class Segmentation {
    static void vp8_update_gf_useage_maps(Compressor cpi, CommonData cm, Macroblock x) {
        int mb_row, mb_col;

        FullAccessGenArrPointer<ModeInfo> this_mb_mode_info = cm.mi.shallowCopy();

        x.gf_active_ptr = cpi.gf_active_flags.shallowCopy();

        if ((cm.frame_type == FrameType.KEY_FRAME) || (cm.refresh_golden_frame)) {
            /* Reset Gf useage monitors */
            cpi.gf_active_flags.memset(0, (short) 1, cm.mb_rows * cm.mb_cols);
            cpi.gf_active_count = cm.mb_rows * cm.mb_cols;
        } else {
            /* for each macroblock row in image */
            for (mb_row = 0; mb_row < cm.mb_rows; ++mb_row) {
                /* for each macroblock col in image */
                for (mb_col = 0; mb_col < cm.mb_cols; ++mb_col) {
                    /*
                     * If using golden then set GF active flag if not already set. If using last
                     * frame 0,0 mode then leave flag as it is else if using non 0,0 motion or intra
                     * modes then clear flag if it is currently set
                     */
                    if ((this_mb_mode_info.get().mbmi.ref_frame == MVReferenceFrame.GOLDEN_FRAME)
                            || (this_mb_mode_info.get().mbmi.ref_frame == MVReferenceFrame.ALTREF_FRAME)) {
                        if (x.gf_active_ptr.get() == 0) {
                            x.gf_active_ptr.set((short) 1);
                            cpi.gf_active_count++;
                        }
                    } else if ((this_mb_mode_info.get().mbmi.mode != MBPredictionMode.ZEROMV)
                            && x.gf_active_ptr.get() != 0) {
                        x.gf_active_ptr.set((short) 0);
                        cpi.gf_active_count--;
                    }

                    x.gf_active_ptr.inc(); /* Step onto next entry */
                    this_mb_mode_info.inc(); /* skip to next mb */
                }

                /* this is to account for the border */
                this_mb_mode_info.inc();
            }
        }
    }

}
