package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.MacroblockD;
import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.intrapred.AllIntraPred;
import org.jcodec.codecs.vpx.vp8.intrapred.IntraPredFN;
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
public class ReconIntra {
    FullAccessIntArrPointer aboveRow = new FullAccessIntArrPointer(12);
    FullAccessIntArrPointer yLeftCol = new FullAccessIntArrPointer(16);
    FullAccessIntArrPointer uLeftCol = new FullAccessIntArrPointer(8);
    FullAccessIntArrPointer vLeftCol = new FullAccessIntArrPointer(8);

    public void vp8_build_intra_predictors_mby_s(MacroblockD x, ReadOnlyIntArrPointer yAbove,
            ReadOnlyIntArrPointer yLeft, int left_stride, FullAccessIntArrPointer yPred, int y_stride) {
        MBPredictionMode mode = x.mode_info_context.get().mbmi.mode;
        int i;
        IntraPredFN fn;

        for (i = 0; i < 16; ++i) {
            yLeftCol.setRel(i, yLeft.getRel(i * left_stride));
        }

        if (mode == MBPredictionMode.DC_PRED) {
            fn = AllIntraPred.dc_pred[x.left_available ? 1 : 0][x.up_available ? 1 : 0][AllIntraPred.sizes.SIZE_16
                    .ordinal()];
        } else {
            fn = AllIntraPred.pred[mode.ordinal()][AllIntraPred.sizes.SIZE_16.ordinal()];
        }

        fn.call(yPred, y_stride, yAbove, yLeftCol);
    }

    public void vp8_build_intra_predictors_mbuv_s(MacroblockD x, ReadOnlyIntArrPointer uabove_row,
            ReadOnlyIntArrPointer vabove_row, ReadOnlyIntArrPointer uleft, ReadOnlyIntArrPointer vleft, int left_stride,
            FullAccessIntArrPointer upred, FullAccessIntArrPointer vpred, int pred_stride) {
        MBPredictionMode uvmode = x.mode_info_context.get().mbmi.uv_mode;
        int i;
        IntraPredFN fn;

        for (i = 0; i < 8; ++i) {
            uLeftCol.setRel(i, uleft.getRel(i * left_stride));
            vLeftCol.setRel(i, vleft.getRel(i * left_stride));
        }

        if (uvmode == MBPredictionMode.DC_PRED) {
            fn = AllIntraPred.dc_pred[x.left_available ? 1 : 0][x.up_available ? 1 : 0][AllIntraPred.sizes.SIZE_8
                    .ordinal()];
        } else {
            fn = AllIntraPred.pred[uvmode.ordinal()][AllIntraPred.sizes.SIZE_8.ordinal()];
        }

        fn.call(upred, pred_stride, uabove_row, uLeftCol);
        fn.call(vpred, pred_stride, vabove_row, vLeftCol);
    }

    public void vp8_intra4x4_predict(ReadOnlyIntArrPointer above, ReadOnlyIntArrPointer yleft, int left_stride,
            BPredictionMode b_mode, FullAccessIntArrPointer dst, int dst_stride, short top_left) {
        aboveRow.setPos(4);
        yLeftCol.rewind();
        yLeftCol.setAndInc(yleft.get());
        yLeftCol.setAndInc(yleft.getRel(left_stride));
        yLeftCol.setAndInc(yleft.getRel(left_stride << 1));
        yLeftCol.setAndInc(yleft.getRel(3 * left_stride));
        yLeftCol.rewind();
        aboveRow.memcopyin(0, above, 0, 8);
        aboveRow.setRel(-1, top_left);
        AllIntraPred.bpred[b_mode.ordinal()].call(dst, dst_stride, aboveRow, yLeftCol);
    }

    static void intra_prediction_down_copy(MacroblockD xd) {
        final int srcLoc = -xd.dst.y_stride + 16;
        for (int i = 3; i < 12; i += 4)
            xd.dst.y_buffer.memcopyin(i * xd.dst.y_stride + 16, xd.dst.y_buffer, srcLoc, 4);
    }
}
