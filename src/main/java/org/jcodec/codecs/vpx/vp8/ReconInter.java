package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.data.MV;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.ModeInfo;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.ReadOnlyIntArrPointer;
import org.jcodec.codecs.vpx.vp8.subpixfns.SubpixFN;

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
public class ReconInter {

    static void build_inter_predictors_custom(BlockD d, FullAccessIntArrPointer dst, int dst_stride,
            ReadOnlyIntArrPointer base_pre, int pre_stride, int w, int h, SubpixFN predict) {
        PositionableIntArrPointer ptr = PositionableIntArrPointer.makePositionableAndInc(base_pre,
                d.getOffset() + (d.bmi.mv.row >> 3) * pre_stride + (d.bmi.mv.col >> 3));

        if ((d.bmi.mv.row & 7) != 0 || (d.bmi.mv.col & 7) != 0) {
            predict.call(ptr, pre_stride, d.bmi.mv.col & 7, d.bmi.mv.row & 7, dst, dst_stride);
        } else {
            CommonUtils.genericCopy(ptr, pre_stride, dst, dst_stride, w, h);
        }
    }

    static void vp8_build_inter_predictors_b(BlockD d, int pitch, ReadOnlyIntArrPointer base_pre, int pre_stride,
            SubpixFN sppf) {
        build_inter_predictors_custom(d, d.predictor.shallowCopy(), pitch, base_pre, pre_stride, 4, 4, sppf);
    }

    static void build_inter_predictors_b(BlockD d, FullAccessIntArrPointer dst, int dst_stride,
            PositionableIntArrPointer base_pre, int pre_stride, SubpixFN sppf) {
        build_inter_predictors_custom(d, dst, dst_stride, base_pre, pre_stride, 4, 4, sppf);
    }

    static void build_inter_predictors2b(MacroblockD x, BlockD d, FullAccessIntArrPointer dst, int dst_stride,
            PositionableIntArrPointer base_pre, int pre_stride) {
        build_inter_predictors_custom(d, dst, dst_stride, base_pre, pre_stride, 8, 4, x.subpixel_predict8x4);
    }

    static void build_inter_predictors4b(MacroblockD x, BlockD d, FullAccessIntArrPointer dst, int dst_stride,
            PositionableIntArrPointer base_pre, int pre_stride) {
        build_inter_predictors_custom(d, dst, dst_stride, base_pre, pre_stride, 8, 8, x.subpixel_predict8x8);
    }

    static void vp8_build_inter4x4_predictors_mbuv(MacroblockD x) {
        /* build uv mvs */
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 2; ++j) {
                int yoffset = i * 8 + j * 2;
                int uoffset = 16 + i * 2 + j;
                int voffset = 20 + i * 2 + j;

                int temp;

                temp = x.block.getRel(yoffset).bmi.mv.row + x.block.getRel(yoffset + 1).bmi.mv.row
                        + x.block.getRel(yoffset + 4).bmi.mv.row + x.block.getRel(yoffset + 5).bmi.mv.row;

                temp += 4 + ((temp >> (4 * 8 - 1)) * 8);

                x.block.getRel(uoffset).bmi.mv.row = (short) ((temp / 8) & x.fullpixel_mask);

                temp = x.block.getRel(yoffset).bmi.mv.col + x.block.getRel(yoffset + 1).bmi.mv.col
                        + x.block.getRel(yoffset + 4).bmi.mv.col + x.block.getRel(yoffset + 5).bmi.mv.col;

                temp += 4 + ((temp >> (4 * 8 - 1)) * 8);

                x.block.getRel(uoffset).bmi.mv.col = (short) ((temp / 8) & x.fullpixel_mask);

                x.block.getRel(voffset).bmi.mv.set(x.block.getRel(uoffset).bmi.mv);
            }
        }

        uvpredictorbuilder(16, 20, x.pre.u_buffer, x);
        uvpredictorbuilder(20, 24, x.pre.v_buffer, x);
    }

    private static void uvpredictorbuilder(int iMin, int iMax, FullAccessIntArrPointer base_pre, MacroblockD x) {
        final int pre_stride = x.pre.uv_stride;
        for (int i = iMin; i < iMax; i++) {
            BlockD d0 = x.block.getRel(i++);
            BlockD d1 = x.block.getRel(i);

            if (d0.bmi.mv.equals(d1.bmi.mv)) {
                build_inter_predictors2b(x, d0, d0.predictor, 8, base_pre, pre_stride);
            } else {
                vp8_build_inter_predictors_b(d0, 8, base_pre, pre_stride, x.subpixel_predict);
                vp8_build_inter_predictors_b(d1, 8, base_pre, pre_stride, x.subpixel_predict);
            }
        }
    }

    static void vp8_build_inter16x16_predictors_mby(MacroblockD x, FullAccessIntArrPointer dst_y, int dst_ystride) {
        PositionableIntArrPointer ptr;
        ModeInfo mi = x.mode_info_context.get();
        int mv_row = mi.mbmi.mv.row;
        int mv_col = mi.mbmi.mv.col;
        int pre_stride = x.pre.y_stride;

        ptr = PositionableIntArrPointer.makePositionableAndInc(x.pre.y_buffer,
                (mv_row >> 3) * pre_stride + (mv_col >> 3));

        if (((mv_row | mv_col) & 7) != 0) {
            x.subpixel_predict16x16.call(ptr, pre_stride, mv_col & 7, mv_row & 7, dst_y, dst_ystride);
        } else {
            CommonUtils.vp8_copy_mem16x16(ptr, pre_stride, dst_y, dst_ystride);
        }
    }

    static void vp8_build_inter16x16_predictors_mbuv(MacroblockD x) {

        FullAccessIntArrPointer upred_ptr = x.getFreshUPredPtr();
        FullAccessIntArrPointer vpred_ptr = x.getFreshVPredPtr();
        ModeInfo mi = x.mode_info_context.get();

        int mv_row = mi.mbmi.mv.row;
        int mv_col = mi.mbmi.mv.col;
        int offset;
        int pre_stride = x.pre.uv_stride;

        /* calc uv motion vectors */
        mv_row += 1 | (mv_row >> (4 * 8 - 1));
        mv_col += 1 | (mv_col >> (4 * 8 - 1));
        mv_row /= 2;
        mv_col /= 2;
        mv_row &= x.fullpixel_mask;
        mv_col &= x.fullpixel_mask;

        offset = (mv_row >> 3) * pre_stride + (mv_col >> 3);
        PositionableIntArrPointer uptr = PositionableIntArrPointer.makePositionableAndInc(x.pre.u_buffer, offset);
        PositionableIntArrPointer vptr = PositionableIntArrPointer.makePositionableAndInc(x.pre.v_buffer, offset);

        if (((mv_row | mv_col) & 7) != 0) {
            x.subpixel_predict8x8.call(uptr, pre_stride, mv_col & 7, mv_row & 7, upred_ptr, 8);
            x.subpixel_predict8x8.call(vptr, pre_stride, mv_col & 7, mv_row & 7, vpred_ptr, 8);
        } else {
            CommonUtils.vp8_copy_mem8x8(uptr, pre_stride, upred_ptr, 8);
            CommonUtils.vp8_copy_mem8x8(vptr, pre_stride, vpred_ptr, 8);
        }
    }

    static void clamp_mv_to_umv_border(MV mv, MacroblockD xd) {
        /*
         * If the MV points so far into the UMV border that no visible pixels are used
         * for reconstruction, the subpel part of the MV can be discarded and the MV
         * limited to 16 pixels with equivalent results.
         *
         * This limit kicks in at 19 pixels for the top and left edges, for the 16
         * pixels plus 3 taps right of the central pixel when subpel filtering. The
         * bottom and right edges use 16 pixels plus 2 pixels left of the central pixel
         * when filtering.
         */
        if (mv.col < (xd.mb_to_left_edge - (19 << 3))) {
            mv.col = (short) (xd.mb_to_left_edge - (16 << 3));
        } else if (mv.col > xd.mb_to_right_edge + (18 << 3)) {
            mv.col = (short) (xd.mb_to_right_edge + (16 << 3));
        }

        if (mv.row < (xd.mb_to_top_edge - (19 << 3))) {
            mv.row = (short) (xd.mb_to_top_edge - (16 << 3));
        } else if (mv.row > xd.mb_to_bottom_edge + (18 << 3)) {
            mv.row = (short) (xd.mb_to_bottom_edge + (16 << 3));
        }
    }

    /* A version of the above function for chroma block MVs. */
    static void clamp_uvmv_to_umv_border(MV mv, MacroblockD xd) {
        mv.col = (short) ((2 * mv.col < (xd.mb_to_left_edge - (19 << 3))) ? (xd.mb_to_left_edge - (16 << 3)) >> 1
                : mv.col);
        mv.col = (short) ((2 * mv.col > xd.mb_to_right_edge + (18 << 3)) ? (xd.mb_to_right_edge + (16 << 3)) >> 1
                : mv.col);

        mv.row = (short) ((2 * mv.row < (xd.mb_to_top_edge - (19 << 3))) ? (xd.mb_to_top_edge - (16 << 3)) >> 1
                : mv.row);
        mv.row = (short) ((2 * mv.row > xd.mb_to_bottom_edge + (18 << 3)) ? (xd.mb_to_bottom_edge + (16 << 3)) >> 1
                : mv.row);
    }

    static void build_4x4uvmvs(MacroblockD x) {
        int i, j;
        final ModeInfo currMi = x.mode_info_context.get();

        for (i = 0; i < 2; ++i) {
            for (j = 0; j < 2; ++j) {
                final int yoffset = i * 8 + j * 2;
                final int uoffset = 16 + i * 2 + j;
                final int voffset = 20 + i * 2 + j;

                int temp;

                temp = currMi.bmi[yoffset + 0].mv.row + currMi.bmi[yoffset + 1].mv.row + currMi.bmi[yoffset + 4].mv.row
                        + currMi.bmi[yoffset + 5].mv.row;

                temp += 4 + ((temp >> (4 * 8 - 1)) * 8);

                int trow = (temp / 8) & x.fullpixel_mask;

                temp = currMi.bmi[yoffset + 0].mv.col + currMi.bmi[yoffset + 1].mv.col + currMi.bmi[yoffset + 4].mv.col
                        + currMi.bmi[yoffset + 5].mv.col;

                temp += 4 + ((temp >> (4 * 8 - 1)) * 8);

                final int tcol = (temp / 8) & x.fullpixel_mask;
                x.block.getRel(uoffset).bmi.mv.setRC(trow, tcol);

                if (currMi.mbmi.need_to_clamp_mvs) {
                    final MV tmv = x.block.getRel(uoffset).bmi.mv.copy();
                    clamp_uvmv_to_umv_border(tmv, x);
                    x.block.getRel(uoffset).bmi.mv.set(tmv);
                }

                x.block.getRel(voffset).bmi.mv.set(x.block.getRel(uoffset).bmi.mv);
            }
        }
    }

    static void vp8_build_inter16x16_predictors_mb(MacroblockD x, FullAccessIntArrPointer dst_y,
            FullAccessIntArrPointer dst_u, FullAccessIntArrPointer dst_v, int dst_ystride, int dst_uvstride) {
        int offset;
        FullAccessIntArrPointer ptr, uptr, vptr;

        MV _16x16mv;

        int pre_stride = x.pre.y_stride;
        ModeInfo mi = x.mode_info_context.get();

        _16x16mv = mi.mbmi.mv.copy();

        if (mi.mbmi.need_to_clamp_mvs) {
            clamp_mv_to_umv_border(_16x16mv, x);
        }

        ptr = x.pre.y_buffer.shallowCopyWithPosInc((_16x16mv.row >> 3) * pre_stride + (_16x16mv.col >> 3));

        if ((_16x16mv.row & 0x7) != 0 || (_16x16mv.col & 0x7) != 0) {
            x.subpixel_predict16x16.call(ptr, pre_stride, _16x16mv.col & 7, _16x16mv.row & 7, dst_y, dst_ystride);
        } else {
            CommonUtils.vp8_copy_mem16x16(ptr, pre_stride, dst_y, dst_ystride);
        }

        /* calc uv motion vectors */
        _16x16mv.row += 1 | (_16x16mv.row >> (4 * 8 - 1));
        _16x16mv.col += 1 | (_16x16mv.col >> (4 * 8 - 1));
        _16x16mv.row /= 2;
        _16x16mv.col /= 2;
        _16x16mv.row &= x.fullpixel_mask;
        _16x16mv.col &= x.fullpixel_mask;

        if (2 * _16x16mv.col < (x.mb_to_left_edge - (19 << 3)) || 2 * _16x16mv.col > x.mb_to_right_edge + (18 << 3)
                || 2 * _16x16mv.row < (x.mb_to_top_edge - (19 << 3))
                || 2 * _16x16mv.row > x.mb_to_bottom_edge + (18 << 3)) {
            return;
        }

        pre_stride >>= 1;
        offset = (_16x16mv.row >> 3) * pre_stride + (_16x16mv.col >> 3);
        uptr = x.pre.u_buffer.shallowCopyWithPosInc(offset);
        vptr = x.pre.v_buffer.shallowCopyWithPosInc(offset);

        if ((_16x16mv.row & 0x7) != 0 || (_16x16mv.col & 0x7) != 0) {
            x.subpixel_predict8x8.call(uptr, pre_stride, _16x16mv.col & 7, _16x16mv.row & 7, dst_u, dst_uvstride);
            x.subpixel_predict8x8.call(vptr, pre_stride, _16x16mv.col & 7, _16x16mv.row & 7, dst_v, dst_uvstride);
        } else {
            CommonUtils.vp8_copy_mem8x8(uptr, pre_stride, dst_u, dst_uvstride);
            CommonUtils.vp8_copy_mem8x8(vptr, pre_stride, dst_v, dst_uvstride);
        }
    }

    static void build_inter4x4_predictors_mb(MacroblockD x) {
        int i;
        FullAccessIntArrPointer base_dst = x.dst.y_buffer;
        PositionableIntArrPointer base_pre = x.pre.y_buffer;
        ModeInfo mi = x.mode_info_context.get();

        if (mi.mbmi.partitioning.ordinal() < 3) { // ???
            final int[] idxes = { 0, 2, 8, 10 };
            int dst_stride = x.dst.y_stride;

            for (int idx : idxes) {
                x.block.getRel(idx).bmi = mi.bmi[idx];
                if (mi.mbmi.need_to_clamp_mvs) {
                    clamp_mv_to_umv_border(x.block.getRel(idx).bmi.mv, x);
                }
            }

            for (int idx : idxes) {
                BlockD b = x.block.getRel(idx);
                build_inter_predictors4b(x, b, b.getOffsetPointer(base_dst), dst_stride, base_pre, dst_stride);
            }
        } else {
            for (i = 0; i < 16; i += 2) {
                x.block.getRel(i).bmi = mi.bmi[i];
                x.block.getRel(i + 1).bmi = mi.bmi[i + 1];
                if (mi.mbmi.need_to_clamp_mvs) {
                    clamp_mv_to_umv_border(x.block.getRel(i).bmi.mv, x);
                    clamp_mv_to_umv_border(x.block.getRel(i + 1).bmi.mv, x);
                }
                intpredbuilderhelper(i, x.dst.y_stride, x, base_dst, base_pre);
            }
        }
        base_dst = x.dst.u_buffer;
        base_pre = x.pre.u_buffer;
        for (i = 16; i < 20; i += 2) {
            /* Note: uv mvs already clamped in build_4x4uvmvs() */
            intpredbuilderhelper(i, x.dst.uv_stride, x, base_dst, base_pre);
        }

        base_dst = x.dst.v_buffer;
        base_pre = x.pre.v_buffer;
        for (i = 20; i < 24; i += 2) {
            /* Note: uv mvs already clamped in build_4x4uvmvs() */
            intpredbuilderhelper(i, x.dst.uv_stride, x, base_dst, base_pre);
        }
    }

    static void intpredbuilderhelper(int i, int dst_stride, MacroblockD x, FullAccessIntArrPointer base_dst,
            PositionableIntArrPointer base_pre) {
        BlockD d0 = x.block.getRel(i);
        BlockD d1 = x.block.getRel(i + 1);

        if (d0.bmi.mv.equals(d1.bmi.mv)) {
            build_inter_predictors2b(x, d0, d0.getOffsetPointer(base_dst), dst_stride, base_pre, dst_stride);
        } else {
            build_inter_predictors_b(d0, d0.getOffsetPointer(base_dst), dst_stride, base_pre, dst_stride,
                    x.subpixel_predict);
            build_inter_predictors_b(d1, d1.getOffsetPointer(base_dst), dst_stride, base_pre, dst_stride,
                    x.subpixel_predict);
        }
    }

    static void vp8_build_inter_predictors_mb(MacroblockD xd) {
        if (xd.mode_info_context.get().mbmi.mode != MBPredictionMode.SPLITMV) {
            vp8_build_inter16x16_predictors_mb(xd, xd.dst.y_buffer, xd.dst.u_buffer, xd.dst.v_buffer, xd.dst.y_stride,
                    xd.dst.uv_stride);
        } else {
            build_4x4uvmvs(xd);
            build_inter4x4_predictors_mb(xd);
        }
    }

}
