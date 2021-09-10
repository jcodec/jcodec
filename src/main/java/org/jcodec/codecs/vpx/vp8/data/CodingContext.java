package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.CommonUtils;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
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
public class CodingContext {
    int kf_indicated;
    int frames_since_key;
    int frames_since_golden;
    short filter_level;
    int frames_till_gf_update_due;
    int[] recent_ref_frame_usage = new int[MVReferenceFrame.count];

    MVContext[] mvc = new MVContext[2];
    FullAccessIntArrPointer[] mvcosts = new FullAccessIntArrPointer[2];

    FullAccessIntArrPointer ymode_prob = new FullAccessIntArrPointer(EntropyMode.vp8_ymode_prob.size());
    FullAccessIntArrPointer uv_mode_prob = new FullAccessIntArrPointer(
            EntropyMode.vp8_uv_mode_prob.size()); /* interframe intra mode probs */
    short[] kf_ymode_prob = new short[4], kf_uv_mode_prob = new short[3]; /* keyframe "" */

    int[] ymode_count = new int[5], uv_mode_count = new int[4]; /* intra MB type cts this frame */

    int[] count_mb_ref_frame_usage = new int[MVReferenceFrame.count];

    int this_frame_percent_intra;
    int last_frame_percent_intra;

    public void vp8_save_coding_context(Compressor cpi) {
        /*
         * Stores a snapshot of key state variables which can subsequently be restored
         * with a call to vp8_restore_coding_context. These functions are intended for
         * use in a re-code loop in vp8_compress_frame where the quantizer value is
         * adjusted between loop iterations.
         */

        frames_since_key = cpi.frames_since_key;
        filter_level = cpi.common.filter_level;
        frames_till_gf_update_due = cpi.frames_till_gf_update_due;
        frames_since_golden = cpi.frames_since_golden;
        mvc[0] = new MVContext(cpi.common.fc.mvc[0]);
        mvc[1] = new MVContext(cpi.common.fc.mvc[1]);

        mvcosts[0] = cpi.rd_costs.mvcosts[0].deepCopy();
        mvcosts[1] = cpi.rd_costs.mvcosts[1].deepCopy();

        CommonUtils.vp8_copy(cpi.common.fc.ymode_prob, ymode_prob);
        CommonUtils.vp8_copy(cpi.common.fc.uv_mode_prob, uv_mode_prob);

        CommonUtils.vp8_copy(cpi.mb.ymode_count, ymode_count);
        CommonUtils.vp8_copy(cpi.mb.uv_mode_count, uv_mode_count);

        /* Stats */
        this_frame_percent_intra = cpi.this_frame_percent_intra;
    }

    public void vp8_restore_coding_context(Compressor cpi) {
        /*
         * Restore key state variables to the snapshot state stored in the previous call
         * to vp8_save_coding_context.
         */

        cpi.frames_since_key = frames_since_key;
        cpi.common.filter_level = filter_level;
        cpi.frames_till_gf_update_due = frames_till_gf_update_due;
        cpi.frames_since_golden = frames_since_golden;

        cpi.common.fc.mvc[0] = new MVContext(mvc[0]);
        cpi.common.fc.mvc[1] = new MVContext(mvc[1]);

        cpi.rd_costs.mvcosts[0] = mvcosts[0].deepCopy();
        cpi.rd_costs.mvcosts[1] = mvcosts[1].deepCopy();

        CommonUtils.vp8_copy(ymode_prob, cpi.common.fc.ymode_prob);
        CommonUtils.vp8_copy(uv_mode_prob, cpi.common.fc.uv_mode_prob);
        CommonUtils.vp8_copy(ymode_count, cpi.mb.ymode_count);
        CommonUtils.vp8_copy(uv_mode_count, cpi.mb.uv_mode_count);

        /* Stats */
        cpi.this_frame_percent_intra = this_frame_percent_intra;

    }
}
