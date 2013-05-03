package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.H264Const.PartPred.Bi;
import static org.jcodec.codecs.h264.H264Const.PartPred.L0;
import static org.jcodec.codecs.h264.H264Const.PartPred.L1;
import static org.jcodec.common.tools.MathUtil.clip;

import org.jcodec.codecs.h264.H264Const.PartPred;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.PredictionWeightTable;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Prediction merge and weight routines
 * 
 * @author The JCodec project
 * 
 */
public class Prediction {

    private SliceHeader sh;

    public Prediction(SliceHeader sh) {
        this.sh = sh;
    }

    public void mergePrediction(int refIdxL0, int refIdxL1, PartPred predType, int comp, int[] pred0, int[] pred1,
            int off, int stride, int blkW, int blkH, int[] out, Frame[][] refs, Frame thisFrame) {

        PictureParameterSet pps = sh.pps;
        if (sh.slice_type == SliceType.P) {
            if (pps.weighted_pred_flag && sh.pred_weight_table != null) {

                PredictionWeightTable w = sh.pred_weight_table;
                weight(pred0, stride, off, blkW, blkH, comp == 0 ? w.luma_log2_weight_denom
                        : w.chroma_log2_weight_denom, comp == 0 ? w.luma_weight[0][refIdxL0]
                        : w.chroma_weight[0][comp - 1][refIdxL0], comp == 0 ? w.luma_offset[0][refIdxL0]
                        : w.chroma_offset[0][comp - 1][refIdxL0], out);
            } else {
                copyPrediction(pred0, stride, off, blkW, blkH, out);
            }
        } else {
            if (!pps.weighted_pred_flag || sh.pps.weighted_bipred_idc == 0
                    || (sh.pps.weighted_bipred_idc == 2 && predType != Bi)) {
                mergeAvg(pred0, pred1, stride, predType, off, blkW, blkH, out);
            } else if (sh.pps.weighted_bipred_idc == 1) {
                PredictionWeightTable w = sh.pred_weight_table;
                int w0 = refIdxL0 == -1 ? 0 : (comp == 0 ? w.luma_weight[0][refIdxL0]
                        : w.chroma_weight[0][comp - 1][refIdxL0]);
                int w1 = refIdxL1 == -1 ? 0 : (comp == 0 ? w.luma_weight[1][refIdxL1]
                        : w.chroma_weight[1][comp - 1][refIdxL1]);
                int o0 = refIdxL0 == -1 ? 0 : (comp == 0 ? w.luma_offset[0][refIdxL0]
                        : w.chroma_offset[0][comp - 1][refIdxL0]);
                int o1 = refIdxL1 == -1 ? 0 : (comp == 0 ? w.luma_offset[1][refIdxL1]
                        : w.chroma_offset[1][comp - 1][refIdxL1]);
                mergeWeight(pred0, pred1, stride, predType, off, blkW, blkH, comp == 0 ? w.luma_log2_weight_denom
                        : w.chroma_log2_weight_denom, w0, w1, o0, o1, out);
            } else {
                int tb = MathUtil.clip(thisFrame.getPOC() - refs[0][refIdxL0].getPOC(), -128, 127);
                int td = MathUtil.clip(refs[1][refIdxL1].getPOC() - refs[0][refIdxL0].getPOC(), -128, 127);
                int w0 = 32, w1 = 32;
                if (td != 0 && refs[0][refIdxL0].isShortTerm() && refs[1][refIdxL1].isShortTerm()) {
                    int tx = (16384 + Math.abs(td / 2)) / td;
                    int dsf = clip((tb * tx + 32) >> 6, -1024, 1023) >> 2;

                    if (dsf >= -64 && dsf <= 128) {
                        w1 = dsf;
                        w0 = 64 - dsf;
                    }
                }

                mergeWeight(pred0, pred1, stride, predType, off, blkW, blkH, 5, w0, w1, 0, 0, out);
            }
        }
    }

    private void mergeAvg(int[] blk0, int[] blk1, int stride, PartPred p0, int off, int blkW, int blkH, int[] o) {
        if (p0 == Bi)
            mergePrediction(blk0, blk1, stride, p0, off, blkW, blkH, o);
        else if (p0 == L0)
            copyPrediction(blk0, stride, off, blkW, blkH, o);
        else if (p0 == L1)
            copyPrediction(blk1, stride, off, blkW, blkH, o);

    }

    private void mergeWeight(int[] blk0, int[] blk1, int stride, PartPred partPred, int off, int blkW, int blkH,
            int logWD, int w0, int w1, int o0, int o1, int[] out) {
        if (partPred == L0) {
            weight(blk0, stride, off, blkW, blkH, logWD, w0, o0, out);
        } else if (partPred == L1) {
            weight(blk1, stride, off, blkW, blkH, logWD, w1, o1, out);
        } else if (partPred == Bi) {
            weightPrediction(blk0, blk1, stride, off, blkW, blkH, logWD, w0, w1, o0, o1, out);
        }
    }

    private void copyPrediction(int[] in, int stride, int off, int blkW, int blkH, int[] o) {

        for (int i = 0; i < blkH; i++, off += stride - blkW)
            for (int j = 0; j < blkW; j++, off++)
                o[off] = in[off];
    }

    private void mergePrediction(int[] blk0, int[] blk1, int stride, PartPred p0, int off, int blkW, int blkH, int[] o) {

        for (int i = 0; i < blkH; i++, off += stride - blkW)
            for (int j = 0; j < blkW; j++, off++)
                o[off] = (blk0[off] + blk1[off] + 1) >> 1;
    }

    private void weightPrediction(int[] blk0, int[] blk1, int stride, int off, int blkW, int blkH, int logWD, int w0,
            int w1, int o0, int o1, int[] out) {
        int dvadva = 1 << logWD;
        int sum = (o0 + o1 + 1) >> 1;
        int logWDCP1 = logWD + 1;
        for (int i = 0; i < blkH; i++, off += stride - blkW)
            for (int j = 0; j < blkW; j++, off++) {
                out[off] = MathUtil.clip(((blk0[off] * w0 + blk1[off] * w1 + dvadva) >> logWDCP1) + sum, 0, 255);
            }
    }

    private void weight(int[] blk0, int stride, int off, int blkW, int blkH, int logWD, int w, int o, int[] out) {
        int dva = 1 << (logWD - 1);
        if (logWD >= 1) {
            for (int i = 0; i < blkH; i++, off += stride - blkW)
                for (int j = 0; j < blkW; j++, off++)
                    out[off] = MathUtil.clip(((blk0[off] * w + dva) >> logWD) + o, 0, 255);
        } else {
            for (int i = 0; i < blkH; i++, off += stride - blkW)
                for (int j = 0; j < blkW; j++, off++)
                    out[off] = MathUtil.clip(blk0[off] * w + o, 0, 255);
        }
    }
}