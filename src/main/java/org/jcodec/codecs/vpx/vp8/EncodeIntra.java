package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.Block;
import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.data.Macroblock;
import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.data.ModeInfo;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;

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
public class EncodeIntra {
    static int vp8_encode_intra(Compressor cpi, Macroblock x, boolean use_dc_pred) {
        int i;
        int intra_pred_var = 0;

        if (use_dc_pred) {
            ModeInfo mi = x.e_mbd.mode_info_context.get();

            mi.mbmi.mode = MBPredictionMode.DC_PRED;
            mi.mbmi.uv_mode = MBPredictionMode.DC_PRED;
            mi.mbmi.ref_frame = MVReferenceFrame.INTRA_FRAME;

            vp8_encode_intra16x16mby(x);

            InvTrans.vp8_inverse_transform_mby(x.e_mbd);
        } else {
            for (i = 0; i < 16; ++i) {
                x.e_mbd.block.getRel(i).bmi.as_mode(BPredictionMode.B_DC_PRED);
                vp8_encode_intra4x4block(x, i);
            }
        }

        intra_pred_var = Variance.vpx_get_mb_ss(x.src_diff);

        return intra_pred_var;
    }

    static void vp8_encode_intra4x4block(Macroblock x, int ib) {
        BlockD b = x.e_mbd.block.getRel(ib);
        Block be = x.block.getRel(ib);
        int dst_stride = x.e_mbd.dst.y_stride;
        FullAccessIntArrPointer dst = b.getOffsetPointer(x.e_mbd.dst.y_buffer);
        PositionableIntArrPointer Above = PositionableIntArrPointer.makePositionableAndInc(dst, -dst_stride);
        PositionableIntArrPointer yleft = PositionableIntArrPointer.makePositionableAndInc(dst, -1);
        short top_left = Above.getRel(-1);

        x.recon.vp8_intra4x4_predict(Above, yleft, dst_stride, b.bmi.as_mode(), b.predictor, 16, top_left);
        EncodeMB.vp8_subtract_b(be, b, 16);

        x.short_fdct4x4.call(be.src_diff, be.coeff, 32);

        x.quantize_b.call(be, b);

        if (b.eob.get() > 1) {
            IDCTllm.vp8_short_idct4x4llm(b.dqcoeff, b.predictor, 16, dst, dst_stride);
        } else {
            IDCTllm.vp8_dc_only_idct_add(b.dqcoeff.get(), b.predictor, 16, dst, dst_stride);
        }

    }

    static void vp8_encode_intra4x4mby(Macroblock mb) {
        ReconIntra.intra_prediction_down_copy(mb.e_mbd);
        for (int i = 0; i < 16; ++i)
            vp8_encode_intra4x4block(mb, i);
    }

    static void vp8_encode_intra16x16mby(Macroblock x) {
        Block b = x.block.get();
        MacroblockD xd = x.e_mbd;

        PositionableIntArrPointer above = PositionableIntArrPointer.makePositionableAndInc(xd.dst.y_buffer,
                -xd.dst.y_stride);
        PositionableIntArrPointer left = PositionableIntArrPointer.makePositionableAndInc(xd.dst.y_buffer, -1);

        x.recon.vp8_build_intra_predictors_mby_s(xd, above, left, xd.dst.y_stride, xd.dst.y_buffer, xd.dst.y_stride);

        EncodeMB.vp8_subtract_mby(x.src_diff, b.base_src, b.src_stride, xd.dst.y_buffer, xd.dst.y_stride);

        EncodeMB.vp8_transform_intra_mby(x);

        Quantize.vp8_quantize_mby(x);

        if (x.optimize)
            EncodeMB.vp8_optimize_mby(x);
    }

    static void vp8_encode_intra16x16mbuv(Macroblock x) {
        MacroblockD xd = x.e_mbd;
        PositionableIntArrPointer uab = PositionableIntArrPointer.makePositionableAndInc(xd.dst.u_buffer,
                -xd.dst.uv_stride);
        PositionableIntArrPointer vab = PositionableIntArrPointer.makePositionableAndInc(xd.dst.v_buffer,
                -xd.dst.uv_stride);
        PositionableIntArrPointer ulef = PositionableIntArrPointer.makePositionableAndInc(xd.dst.u_buffer, -1);
        PositionableIntArrPointer vlef = PositionableIntArrPointer.makePositionableAndInc(xd.dst.v_buffer, -1);

        x.recon.vp8_build_intra_predictors_mbuv_s(xd, uab, vab, ulef, vlef, xd.dst.uv_stride, xd.dst.u_buffer,
                xd.dst.v_buffer, xd.dst.uv_stride);

        EncodeMB.vp8_subtract_mbuv(x.src_diff, x.src.u_buffer, x.src.v_buffer, x.src.uv_stride, xd.dst.u_buffer,
                xd.dst.v_buffer, xd.dst.uv_stride);

        EncodeMB.vp8_transform_mbuv(x);

        Quantize.vp8_quantize_mbuv(x);

        if (x.optimize)
            EncodeMB.vp8_optimize_mbuv(x);
    }

}
