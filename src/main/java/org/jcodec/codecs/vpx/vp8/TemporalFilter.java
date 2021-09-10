package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.Block;
import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.data.Lookahead;
import org.jcodec.codecs.vpx.vp8.data.LookaheadEntry;
import org.jcodec.codecs.vpx.vp8.data.MV;
import org.jcodec.codecs.vpx.vp8.data.Macroblock;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.data.VarianceResults;
import org.jcodec.codecs.vpx.vp8.data.YV12buffer;
import org.jcodec.codecs.vpx.vp8.enums.BlockEnum;
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
public class TemporalFilter {
    public static final int THRESH_LOW = 10000;
    public static final int THRESH_HIGH = 20000;

    static void vp8_temporal_filter_predictors_mb(MacroblockD x, FullAccessIntArrPointer y_mb_ptr,
            FullAccessIntArrPointer u_mb_ptr, FullAccessIntArrPointer v_mb_ptr, int stride, int mv_row, int mv_col,
            FullAccessIntArrPointer pred) {
        int offset;
        FullAccessIntArrPointer yptr, uptr, vptr;

        /* Y */
        yptr = y_mb_ptr.shallowCopyWithPosInc((mv_row >> 3) * stride + (mv_col >> 3));

        if (((mv_row | mv_col) & 7) != 0) {
            x.subpixel_predict16x16.call(yptr, stride, mv_col & 7, mv_row & 7, pred, 16);
        } else {
            CommonUtils.vp8_copy_mem16x16(yptr, stride, pred, 16);
        }

        /* U & V */
        mv_row >>= 1;
        mv_col >>= 1;
        stride = (stride + 1) >> 1;
        offset = (mv_row >> 3) * stride + (mv_col >> 3);
        uptr = u_mb_ptr.shallowCopyWithPosInc(offset);
        vptr = v_mb_ptr.shallowCopyWithPosInc(offset);

        if (((mv_row | mv_col) & 7) != 0) {
            x.subpixel_predict8x8.call(uptr, stride, mv_col & 7, mv_row & 7,
                    pred.shallowCopyWithPosInc(MacroblockD.USHIFT), 8);
            x.subpixel_predict8x8.call(vptr, stride, mv_col & 7, mv_row & 7,
                    pred.shallowCopyWithPosInc(MacroblockD.VSHIFT), 8);
        } else {
            CommonUtils.vp8_copy_mem8x8(uptr, stride, pred.shallowCopyWithPosInc(MacroblockD.USHIFT), 8);
            CommonUtils.vp8_copy_mem8x8(vptr, stride, pred.shallowCopyWithPosInc(MacroblockD.VSHIFT), 8);
        }
    }

    static void vp8_temporal_filter_apply(FullAccessIntArrPointer frame1, int stride, FullAccessIntArrPointer frame2,
            int block_size, int strength, int filter_weight, FullAccessIntArrPointer accumulator,
            FullAccessIntArrPointer count) {
        int i, j, k;
        int modifier;
        int byt = 0;
        frame2 = frame2.shallowCopy();
        final int rounding = strength > 0 ? 1 << (strength - 1) : 0;

        for (i = 0, k = 0; i < block_size; ++i) {
            for (j = 0; j < block_size; j++, k++) {
                int src_byte = frame1.getRel(byt);
                int pixel_value = frame2.getAndInc();

                modifier = src_byte - pixel_value;
                /*
                 * This is an integer approximation of: float coeff = (3.0 * modifer * modifier)
                 * / pow(2, strength); modifier = (int)roundf(coeff > 16 ? 0 : 16-coeff);
                 */
                modifier *= modifier;
                modifier *= 3;
                modifier += rounding;
                modifier >>= strength;

                if (modifier > 16)
                    modifier = 16;

                modifier = 16 - modifier;
                modifier *= filter_weight;

                count.setRel(k, (short) (count.getRel(k) + modifier));
                accumulator.setRel(k, (short) (accumulator.getRel(k) + modifier * pixel_value));

                byt++;
            }

            byt += stride - block_size;
        }
    }

    static long vp8_temporal_filter_find_matching_mb(Compressor cpi, YV12buffer arf_frame, YV12buffer frame_ptr,
            int mb_offset, int error_thresh) {
        Macroblock x = cpi.mb;
        long bestsme = Long.MAX_VALUE;

        Block b = x.block.get();
        BlockD d = x.e_mbd.block.get();
        MV best_ref_mv1 = new MV();
        MV best_ref_mv1_full = new MV(); /* full-pixel value of best_ref_mv1 */

        /* Save input state */
        FullAccessIntArrPointer base_src = b.base_src;
        int src = b.src;
        int src_stride = b.src_stride;
        FullAccessIntArrPointer base_pre = x.e_mbd.pre.y_buffer.shallowCopy();
        int pre = d.getOffset();
        int pre_stride = x.e_mbd.pre.y_stride;

        /* Setup frame pointers */
        b.base_src = arf_frame.y_buffer.shallowCopy();
        b.src_stride = arf_frame.y_stride;
        b.src = mb_offset;

        x.e_mbd.pre.y_buffer = frame_ptr.y_buffer.shallowCopy();
        x.e_mbd.pre.y_stride = frame_ptr.y_stride;
        d.setOffset(mb_offset);

        /* Ignore mv costing by sending NULL cost arrays */
        bestsme = x.hex.apply(x, false, cpi.fn_ptr.get(BlockEnum.BLOCK_16X16), best_ref_mv1_full, best_ref_mv1,
                d.bmi.mv);

        /* Try sub-pixel MC? */
        {
            VarianceResults res = new VarianceResults();

            /* Ignore mv costing by sending NULL cost array */
            bestsme = cpi.find_fractional_mv_step.call(x, b, d, d.bmi.mv, best_ref_mv1, x.errorperbit,
                    cpi.fn_ptr.get(BlockEnum.BLOCK_16X16), null, res);
        }

        /* Restore input state */
        b.base_src = base_src;
        b.src = src;
        b.src_stride = src_stride;
        x.e_mbd.pre.y_buffer = base_pre;
        d.setOffset(pre);
        x.e_mbd.pre.y_stride = pre_stride;

        return bestsme;
    }

    static void vp8_temporal_filter_iterate(Compressor cpi, int frame_count, int alt_ref_index, int strength) {
        int byt;
        int frame;
        int mb_col, mb_row;
        int filter_weight;
        int mb_cols = cpi.common.mb_cols;
        int mb_rows = cpi.common.mb_rows;
        int mb_y_offset = 0;
        int mb_uv_offset = 0;
        FullAccessIntArrPointer accumulator = new FullAccessIntArrPointer(16 * 16 + 8 * 8 + 8 * 8);
        FullAccessIntArrPointer count = new FullAccessIntArrPointer(16 * 16 + 8 * 8 + 8 * 8);
        MacroblockD mbd = cpi.mb.e_mbd;
        YV12buffer f = cpi.frames[alt_ref_index];
        FullAccessIntArrPointer dst1, dst2;
        FullAccessIntArrPointer predictor = new FullAccessIntArrPointer(16 * 16 + 8 * 8 + 8 * 8);

        /* Save input state */
        FullAccessIntArrPointer y_buffer = mbd.pre.y_buffer.shallowCopy();
        FullAccessIntArrPointer u_buffer = mbd.pre.u_buffer.shallowCopy();
        FullAccessIntArrPointer v_buffer = mbd.pre.v_buffer.shallowCopy();

        for (mb_row = 0; mb_row < mb_rows; ++mb_row) {
            /*
             * Source frames are extended to 16 pixels. This is different than L/A/G
             * reference frames that have a border of 32 (VP8BORDERINPIXELS) A 6 tap filter
             * is used for motion search. This requires 2 pixels before and 3 pixels after.
             * So the largest Y mv on a border would then be 16 - 3. The UV blocks are half
             * the size of the Y and therefore only extended by 8. The largest mv that a UV
             * block can support is 8 - 3. A UV mv is half of a Y mv. (16 - 3) >> 1 == 6
             * which is greater than 8 - 3. To keep the mv in play for both Y and UV planes
             * the max that it can be on a border is therefore 16 - 5.
             */
            cpi.mb.mv_row_min = (short) -((mb_row * 16) + (16 - 5));
            cpi.mb.mv_row_max = (short) (((cpi.common.mb_rows - 1 - mb_row) * 16) + (16 - 5));

            for (mb_col = 0; mb_col < mb_cols; ++mb_col) {
                int i, j, k;
                int stride;
                CommonUtils.vp8_zero(accumulator);
                CommonUtils.vp8_zero(count);

                cpi.mb.mv_col_min = (short) -((mb_col * 16) + (16 - 5));
                cpi.mb.mv_col_max = (short) (((cpi.common.mb_cols - 1 - mb_col) * 16) + (16 - 5));

                for (frame = 0; frame < frame_count; ++frame) {
                    if (cpi.frames[frame] == null)
                        continue;

                    mbd.block.get().bmi.mv.setZero();

                    if (frame == alt_ref_index) {
                        filter_weight = 2;
                    } else {
                        long err = 0;
                        /* Find best match in this frame by MC */
                        err = vp8_temporal_filter_find_matching_mb(cpi, cpi.frames[alt_ref_index], cpi.frames[frame],
                                mb_y_offset, THRESH_LOW);
                        /*
                         * Assign higher weight to matching MB if it's error score is lower. If not
                         * applying MC default behavior is to weight all MBs equal.
                         */
                        filter_weight = err < THRESH_LOW ? 2 : err < THRESH_HIGH ? 1 : 0;
                    }

                    if (filter_weight != 0) {
                        /* Construct the predictors */
                        vp8_temporal_filter_predictors_mb(mbd,
                                cpi.frames[frame].y_buffer.shallowCopyWithPosInc(mb_y_offset),
                                cpi.frames[frame].u_buffer.shallowCopyWithPosInc(mb_uv_offset),
                                cpi.frames[frame].v_buffer.shallowCopyWithPosInc(mb_uv_offset),
                                cpi.frames[frame].y_stride, mbd.block.get().bmi.mv.row, mbd.block.get().bmi.mv.col,
                                predictor);

                        /* Apply the filter (YUV) */
                        vp8_temporal_filter_apply(f.y_buffer.shallowCopyWithPosInc(mb_y_offset), f.y_stride, predictor,
                                16, strength, filter_weight, accumulator, count);

                        vp8_temporal_filter_apply(f.u_buffer.shallowCopyWithPosInc(mb_uv_offset), f.uv_stride,
                                predictor.shallowCopyWithPosInc(MacroblockD.USHIFT), 8, strength, filter_weight,
                                accumulator.shallowCopyWithPosInc(MacroblockD.USHIFT),
                                count.shallowCopyWithPosInc(MacroblockD.USHIFT));

                        vp8_temporal_filter_apply(f.v_buffer.shallowCopyWithPosInc(mb_uv_offset), f.uv_stride,
                                predictor.shallowCopyWithPosInc(MacroblockD.VSHIFT), 8, strength, filter_weight,
                                accumulator.shallowCopyWithPosInc(MacroblockD.VSHIFT),
                                count.shallowCopyWithPosInc(MacroblockD.VSHIFT));
                    }
                }

                /* Normalize filter output to produce AltRef frame */
                dst1 = cpi.alt_ref_buffer.y_buffer;
                stride = cpi.alt_ref_buffer.y_stride;
                byt = mb_y_offset;
                for (i = 0, k = 0; i < 16; ++i) {
                    for (j = 0; j < 16; j++, k++) {
                        int pval = accumulator.getRel(k) + (count.getRel(k) >> 1);
                        pval *= cpi.fixed_divide[count.getRel(k)];
                        pval >>= 19;

                        dst1.setRel(byt, (short) pval);

                        /* move to next pixel */
                        byt++;
                    }

                    byt += stride - 16;
                }

                dst1 = cpi.alt_ref_buffer.u_buffer;
                dst2 = cpi.alt_ref_buffer.v_buffer;
                stride = cpi.alt_ref_buffer.uv_stride;
                byt = mb_uv_offset;
                for (i = 0, k = 256; i < 8; ++i) {
                    for (j = 0; j < 8; j++, k++) {
                        int m = k + 64;

                        /* U */
                        int pval = accumulator.getRel(k) + (count.getRel(k) >> 1);
                        pval *= cpi.fixed_divide[count.getRel(k)];
                        pval >>= 19;
                        dst1.setRel(byt, (short) pval);

                        /* V */
                        pval = accumulator.getRel(m) + (count.getRel(m) >> 1);
                        pval *= cpi.fixed_divide[count.getRel(m)];
                        pval >>= 19;
                        dst2.setRel(byt, (short) pval);

                        /* move to next pixel */
                        byt++;
                    }

                    byt += stride - 8;
                }

                mb_y_offset += 16;
                mb_uv_offset += 8;
            }

            mb_y_offset += 16 * (f.y_stride - mb_cols);
            mb_uv_offset += 8 * (f.uv_stride - mb_cols);
        }

        /* Restore input state */
        mbd.pre.y_buffer = y_buffer;
        mbd.pre.u_buffer = u_buffer;
        mbd.pre.v_buffer = v_buffer;
    }

    static void vp8_temporal_filter_prepare(Compressor cpi, int distance) {
        int frame = 0;

        int num_frames_backward = 0;
        int num_frames_forward = 0;
        int frames_to_blur_backward = 0;
        int frames_to_blur_forward = 0;
        int frames_to_blur = 0;
        int start_frame = 0;

        int strength = cpi.oxcf.arnr_strength;

        int blur_type = cpi.oxcf.arnr_type;

        int max_frames = cpi.active_arnr_frames;

        num_frames_backward = distance;
        num_frames_forward = cpi.lookahead.vp8_lookahead_depth() - (num_frames_backward + 1);

        switch (blur_type) {
        case 1:
            /* Backward Blur */

            frames_to_blur_backward = num_frames_backward;

            if (frames_to_blur_backward >= max_frames) {
                frames_to_blur_backward = max_frames - 1;
            }

            frames_to_blur = frames_to_blur_backward + 1;
            break;

        case 2:
            /* Forward Blur */

            frames_to_blur_forward = num_frames_forward;

            if (frames_to_blur_forward >= max_frames) {
                frames_to_blur_forward = max_frames - 1;
            }

            frames_to_blur = frames_to_blur_forward + 1;
            break;

        case 3:
        default:
            /* Center Blur */
            frames_to_blur_forward = num_frames_forward;
            frames_to_blur_backward = num_frames_backward;

            if (frames_to_blur_forward > frames_to_blur_backward) {
                frames_to_blur_forward = frames_to_blur_backward;
            }

            if (frames_to_blur_backward > frames_to_blur_forward) {
                frames_to_blur_backward = frames_to_blur_forward;
            }

            /* When max_frames is even we have 1 more frame backward than forward */
            if (frames_to_blur_forward > (max_frames - 1) / 2) {
                frames_to_blur_forward = ((max_frames - 1) / 2);
            }

            if (frames_to_blur_backward > (max_frames / 2)) {
                frames_to_blur_backward = (max_frames / 2);
            }

            frames_to_blur = frames_to_blur_backward + frames_to_blur_forward + 1;
            break;
        }

        start_frame = distance + frames_to_blur_forward;

        /* Setup frame pointers, NULL indicates frame not included in filter */
        for (int i = 0; i < max_frames; i++) {
            cpi.frames[i] = null;
        }
        for (frame = 0; frame < frames_to_blur; ++frame) {
            int which_buffer = start_frame - frame;
            LookaheadEntry buf = cpi.lookahead.vp8_lookahead_peek(which_buffer, Lookahead.PEEK_FORWARD);
            cpi.frames[frames_to_blur - 1 - frame] = buf.img;
        }
        vp8_temporal_filter_iterate(cpi, frames_to_blur, frames_to_blur_backward, strength);
    }

}
