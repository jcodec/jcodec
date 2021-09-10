package org.jcodec.codecs.vpx.vp8;

import java.util.Arrays;

import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.data.LoopFilterInfo;
import org.jcodec.codecs.vpx.vp8.data.LoopFilterInfoN;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.ModeInfo;
import org.jcodec.codecs.vpx.vp8.data.CommonData;
import org.jcodec.codecs.vpx.vp8.data.YV12buffer;
import org.jcodec.codecs.vpx.vp8.enums.FrameType;
import org.jcodec.codecs.vpx.vp8.enums.LoopFilterType;
import org.jcodec.codecs.vpx.vp8.enums.MBLvlFeatures;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessGenArrPointer;
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
public class LoopFilter {

    public static final short MAX_LOOP_FILTER = 63;
    public static final int PARTIAL_FRAME_FRACTION = 8;

    static void vp8_loop_filter_frame_init(CommonData cm, MacroblockD mbd, short default_filt_lvl) {
        int seg, /* segment number */
                ref, /* index in ref_lf_deltas */
                mode; /* index in mode_lf_deltas */

        LoopFilterInfoN lfi = cm.lf_info;

        /* update limits if sharpness has changed */
        if (cm.last_sharpness_level != cm.sharpness_level) {
            lfi.vp8_loop_filter_update_sharpness(cm.sharpness_level);
            cm.last_sharpness_level = cm.sharpness_level;
        }

        for (seg = 0; seg < BlockD.MAX_MB_SEGMENTS; ++seg) {
            short lvl_seg = default_filt_lvl;
            int lvl_ref;
            short lvl_mode;

            /* Note the baseline filter values for each segment */
            if (mbd.segmentation_enabled != 0) {
                if (mbd.mb_segement_abs_delta == BlockD.SEGMENT_ABSDATA) {
                    lvl_seg = mbd.segment_feature_data[MBLvlFeatures.ALT_LF.ordinal()][seg];
                } else { /* Delta Value */
                    lvl_seg += mbd.segment_feature_data[MBLvlFeatures.ALT_LF.ordinal()][seg];
                }
                lvl_seg = (lvl_seg > 0) ? ((lvl_seg > 63) ? 63 : lvl_seg) : 0;
            }

            if (!mbd.mode_ref_lf_delta_enabled) {
                /*
                 * we could get rid of this if we assume that deltas are set to zero when not in
                 * use; encoder always uses deltas
                 */
                for (int i = 0; i < lfi.lvl[seg].length; i++) {
                    Arrays.fill(lfi.lvl[seg][i], lvl_seg);
                }
                continue;
            }

            /* INTRA_FRAME */
            ref = MVReferenceFrame.INTRA_FRAME.ordinal();

            /* Apply delta for reference frame */
            lvl_ref = lvl_seg + mbd.ref_lf_deltas[ref];

            /* Apply delta for Intra modes */
            mode = 0; /* B_PRED */
            /* Only the split mode BPRED has a further special case */
            lvl_mode = (short) (lvl_ref + mbd.mode_lf_deltas[mode]);
            /* clamp */
            lvl_mode = (lvl_mode > 0) ? (lvl_mode > 63 ? 63 : lvl_mode) : 0;

            lfi.lvl[seg][ref][mode] = lvl_mode;

            mode = 1; /* all the rest of Intra modes */
            /* clamp */
            lvl_mode = (short) ((lvl_ref > 0) ? (lvl_ref > 63 ? 63 : lvl_ref) : 0);
            lfi.lvl[seg][ref][mode] = lvl_mode;

            /* LAST, GOLDEN, ALT */
            for (ref = 1; ref < MVReferenceFrame.count; ++ref) {
                /* Apply delta for reference frame */
                lvl_ref = lvl_seg + mbd.ref_lf_deltas[ref];

                /* Apply delta for Inter modes */
                for (mode = 1; mode < 4; ++mode) {
                    lvl_mode = (short) (lvl_ref + mbd.mode_lf_deltas[mode]);
                    /* clamp */
                    lvl_mode = (lvl_mode > 0) ? (lvl_mode > 63 ? 63 : lvl_mode) : 0;

                    lfi.lvl[seg][ref][mode] = lvl_mode;
                }
            }
        }
    }

    interface LoopFilterCore {
        void call(int mb_col, int mb_row, LoopFilterInfoN lfi_n, boolean skip_lf, FrameType frame_type,
                int filter_level, FullAccessIntArrPointer y_ptr, int post_y_stride, FullAccessIntArrPointer u_ptr,
                FullAccessIntArrPointer v_ptr, int post_uv_stride, boolean dontSkipFirstRow);
    }

    interface PtrIncrementor {
        void nextMB(FullAccessIntArrPointer y_ptr, FullAccessIntArrPointer u_ptr, FullAccessIntArrPointer v_ptr);

        void nextRow(FullAccessIntArrPointer y_ptr, FullAccessIntArrPointer u_ptr, FullAccessIntArrPointer v_ptr,
                YV12buffer post);
    }

    static final LoopFilterCore normalCore = new LoopFilterCore() {
        @Override
        public void call(int mb_col, int mb_row, LoopFilterInfoN lfi_n, boolean skip_lf, FrameType frame_type,
                int filter_level, FullAccessIntArrPointer y_ptr, int post_y_stride, FullAccessIntArrPointer u_ptr,
                FullAccessIntArrPointer v_ptr, int post_uv_stride, boolean dontSkipFirstRow) {
            LoopFilterInfo lfi = new LoopFilterInfo(frame_type, lfi_n, filter_level);
            if (mb_col > 0)
                LFFilters.vp8_loop_filter_mbv(y_ptr, u_ptr, v_ptr, post_y_stride, post_uv_stride, lfi);
            if (!skip_lf)
                LFFilters.vp8_loop_filter_bv(y_ptr, u_ptr, v_ptr, post_y_stride, post_uv_stride, lfi);

            /* don't apply across umv border */
            if (dontSkipFirstRow || mb_row > 0)
                LFFilters.vp8_loop_filter_mbh(y_ptr, u_ptr, v_ptr, post_y_stride, post_uv_stride, lfi);

            if (!skip_lf)
                LFFilters.vp8_loop_filter_bh(y_ptr, u_ptr, v_ptr, post_y_stride, post_uv_stride, lfi);
        }
    };

    static final LoopFilterCore simpleCore = new LoopFilterCore() {
        @Override
        public void call(int mb_col, int mb_row, LoopFilterInfoN lfi_n, boolean skip_lf, FrameType frame_type,
                int filter_level, FullAccessIntArrPointer y_ptr, int post_y_stride, FullAccessIntArrPointer u_ptr,
                FullAccessIntArrPointer v_ptr, int post_uv_stride, boolean dontSkipFirstRow) {
            LoopFilterInfo lfi = new LoopFilterInfo(frame_type, lfi_n, filter_level);

            if (mb_col > 0)
                LFFilters.vp8_loop_filter_simple_vertical_edge(y_ptr, post_y_stride, lfi.mblim);

            if (!skip_lf)
                LFFilters.vp8_loop_filter_bvs(y_ptr, post_y_stride, lfi.blim);

            /* don't apply across umv border */
            if (mb_row > 0)
                LFFilters.vp8_loop_filter_simple_horizontal_edge(y_ptr, post_y_stride, lfi.mblim);

            if (!skip_lf)
                LFFilters.vp8_loop_filter_bhs(y_ptr, post_y_stride, lfi.blim);

        }
    };

    static final LoopFilterCore yOnlySimpleCore = new LoopFilterCore() {
        @Override
        public void call(int mb_col, int mb_row, LoopFilterInfoN lfi_n, boolean skip_lf, FrameType frame_type,
                int filter_level, FullAccessIntArrPointer y_ptr, int post_y_stride, FullAccessIntArrPointer u_ptr,
                FullAccessIntArrPointer v_ptr, int post_uv_stride, boolean dontSkipFirstRow) {
            if (mb_col > 0)
                LFFilters.vp8_loop_filter_simple_vertical_edge(y_ptr, post_y_stride,
                        new ReadOnlyIntArrPointer(lfi_n.mblim[filter_level], 0));

            if (!skip_lf)
                LFFilters.vp8_loop_filter_bvs(y_ptr, post_y_stride,
                        new ReadOnlyIntArrPointer(lfi_n.blim[filter_level], 0));

            if (dontSkipFirstRow || mb_row > 0)
                LFFilters.vp8_loop_filter_simple_horizontal_edge(y_ptr, post_y_stride,
                        new ReadOnlyIntArrPointer(lfi_n.mblim[filter_level], 0));

            if (!skip_lf)
                LFFilters.vp8_loop_filter_bhs(y_ptr, post_y_stride,
                        new ReadOnlyIntArrPointer(lfi_n.blim[filter_level], 0));
        }
    };

    static final PtrIncrementor regularIncrementor = new PtrIncrementor() {

        @Override
        public void nextRow(FullAccessIntArrPointer y_ptr, FullAccessIntArrPointer u_ptr, FullAccessIntArrPointer v_ptr,
                YV12buffer post) {
            y_ptr.incBy(post.y_stride * 16 - post.y_width);
            u_ptr.incBy(post.uv_stride * 8 - post.uv_width);
            v_ptr.incBy(post.uv_stride * 8 - post.uv_width);
        }

        @Override
        public void nextMB(FullAccessIntArrPointer y_ptr, FullAccessIntArrPointer u_ptr,
                FullAccessIntArrPointer v_ptr) {
            y_ptr.incBy(16);
            u_ptr.incBy(8);
            v_ptr.incBy(8);
        }
    };

    static final PtrIncrementor yOnlyIncrementor = new PtrIncrementor() {

        @Override
        public void nextRow(FullAccessIntArrPointer y_ptr, FullAccessIntArrPointer u_ptr, FullAccessIntArrPointer v_ptr,
                YV12buffer post) {
            y_ptr.incBy(post.y_stride * 16 - post.y_width);
        }

        @Override
        public void nextMB(FullAccessIntArrPointer y_ptr, FullAccessIntArrPointer u_ptr,
                FullAccessIntArrPointer v_ptr) {
            y_ptr.incBy(16);
        }
    };

    static void filterThrough(final LoopFilterCore actualFilter, final PtrIncrementor incrementor, final CommonData cm,
            final FrameType frame_type, final int mb_rows, final int mb_cols, final FullAccessIntArrPointer y_ptr,
            final FullAccessIntArrPointer u_ptr, final FullAccessIntArrPointer v_ptr,
            final FullAccessGenArrPointer<ModeInfo> mode_info_context, final boolean dontSkipFirstRow) {
        final YV12buffer post = cm.frame_to_show;
        final LoopFilterInfoN lfi_n = cm.lf_info;
        /* vp8_filter each macro block */
        for (int mb_row = 0; mb_row < mb_rows; ++mb_row) {
            for (int mb_col = 0; mb_col < mb_cols; ++mb_col) {
                ModeInfo mi = mode_info_context.get();
                final boolean skip_lf = (ModeInfo.hasSecondOrder(mode_info_context) && mi.mbmi.mb_skip_coeff);

                final int mode_index = lfi_n.mode_lf_lut.get(mi.mbmi.mode);
                final int seg = mi.mbmi.segment_id;
                final MVReferenceFrame ref_frame = mi.mbmi.ref_frame;

                final int filter_level = lfi_n.lvl[seg][ref_frame.ordinal()][mode_index];

                if (filter_level != 0) {
                    actualFilter.call(mb_col, mb_row, lfi_n, skip_lf, frame_type, filter_level, y_ptr, post.y_stride,
                            u_ptr, v_ptr, post.uv_stride, dontSkipFirstRow);
                }

                incrementor.nextMB(y_ptr, u_ptr, v_ptr);

                mode_info_context.inc(); /* step to next MB */
            }
            incrementor.nextRow(y_ptr, u_ptr, v_ptr, post);

            mode_info_context.inc(); /* Skip border mb */
        }
    }

    static void vp8_loop_filter_frame(CommonData cm, MacroblockD mbd, FrameType frame_type) {

        /* Initialize the loop filter for this frame. */
        vp8_loop_filter_frame_init(cm, mbd, cm.filter_level);

        /* Set up the buffer pointers */
        YV12buffer post = cm.frame_to_show;

        FullAccessIntArrPointer y_ptr, u_ptr, v_ptr;
        y_ptr = post.y_buffer.shallowCopy();
        u_ptr = post.u_buffer.shallowCopy();
        v_ptr = post.v_buffer.shallowCopy();

        filterThrough(cm.filter_type == LoopFilterType.NORMAL ? normalCore : simpleCore, regularIncrementor, cm,
                frame_type, cm.mb_rows, cm.mb_cols, y_ptr, u_ptr, v_ptr, cm.mi.shallowCopy(), false);
    }

    static void vp8_loop_filter_frame_yonly(CommonData cm, MacroblockD mbd, short default_filt_lvl) {
        YV12buffer post = cm.frame_to_show;
        vp8_loop_filter_partial_frame_spec(cm, mbd, default_filt_lvl, cm.mb_rows << 4, post.y_buffer.shallowCopy(),
                cm.mi.shallowCopy(), false);
    }

    private static void vp8_loop_filter_partial_frame_spec(CommonData cm, MacroblockD mbd, short default_filt_lvl,
            int linestocopy, FullAccessIntArrPointer y_ptr, FullAccessGenArrPointer<ModeInfo> mic, boolean alwaysMBH) {
        /* Initialize the loop filter for this frame. */
        vp8_loop_filter_frame_init(cm, mbd, default_filt_lvl);
        filterThrough(cm.filter_type == LoopFilterType.NORMAL ? normalCore : yOnlySimpleCore, yOnlyIncrementor, cm,
                cm.frame_type, linestocopy >> 4, cm.mb_cols, y_ptr, null, null, mic, alwaysMBH);
    }

    static void vp8_loop_filter_partial_frame(CommonData cm, MacroblockD mbd, short default_filt_lvl) {
        YV12buffer post = cm.frame_to_show;
        /* number of MB rows to use in partial filtering */
        int linestocopy = cm.mb_rows / PARTIAL_FRAME_FRACTION;
        linestocopy = linestocopy != 0 ? linestocopy << 4 : 16; /* 16 lines per MB */
        /* Set up the buffer pointers; partial image starts at ~middle of frame */
        FullAccessIntArrPointer y_ptr = post.y_buffer
                .shallowCopyWithPosInc(((post.y_height >> 5) * 16) * post.y_stride);
        FullAccessGenArrPointer<ModeInfo> mode_info_context = cm.mi
                .shallowCopyWithPosInc((post.y_height >> 5) * (cm.mb_cols + 1));

        vp8_loop_filter_partial_frame_spec(cm, mbd, default_filt_lvl, linestocopy, y_ptr, mode_info_context, true);
    }

}
