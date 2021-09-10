package org.jcodec.codecs.vpx.vp8.intrapred;

import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;

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
public class AllIntraPred {

    public enum sizes {
        SIZE_16, SIZE_8, NUM_SIZES,
    }

    public static final IntraPredFN[][] pred = new IntraPredFN[4][sizes.NUM_SIZES.ordinal()];
    public static final IntraPredFN[][][] dc_pred = new IntraPredFN[2][2][sizes.NUM_SIZES.ordinal()];
    public static final IntraPredFN[] bpred = new IntraPredFN[BPredictionMode.bpredModecount];

    static {
        pred[MBPredictionMode.V_PRED.ordinal()][sizes.SIZE_16.ordinal()] = new VPredictor(16);
        pred[MBPredictionMode.H_PRED.ordinal()][sizes.SIZE_16.ordinal()] = new HPredictor(16);
        pred[MBPredictionMode.TM_PRED.ordinal()][sizes.SIZE_16.ordinal()] = new TMPredictor(16);

        pred[MBPredictionMode.V_PRED.ordinal()][sizes.SIZE_8.ordinal()] = new VPredictor(8);
        pred[MBPredictionMode.H_PRED.ordinal()][sizes.SIZE_8.ordinal()] = new HPredictor(8);
        pred[MBPredictionMode.TM_PRED.ordinal()][sizes.SIZE_8.ordinal()] = new TMPredictor(8);

        dc_pred[0][0][sizes.SIZE_16.ordinal()] = new DC128Predictor(16);
        dc_pred[0][1][sizes.SIZE_16.ordinal()] = new DCTopPredictor(16);
        dc_pred[1][0][sizes.SIZE_16.ordinal()] = new DCLeftPredictor(16);
        dc_pred[1][1][sizes.SIZE_16.ordinal()] = new DCPredictor(16);

        dc_pred[0][0][sizes.SIZE_8.ordinal()] = new DC128Predictor(8);
        dc_pred[0][1][sizes.SIZE_8.ordinal()] = new DCTopPredictor(8);
        dc_pred[1][0][sizes.SIZE_8.ordinal()] = new DCLeftPredictor(8);
        dc_pred[1][1][sizes.SIZE_8.ordinal()] = new DCPredictor(8);

        bpred[BPredictionMode.B_DC_PRED.ordinal()] = new DCPredictor(4);
        bpred[BPredictionMode.B_TM_PRED.ordinal()] = new TMPredictor(4);
        bpred[BPredictionMode.B_VE_PRED.ordinal()] = new VEPredictor4x4();
        bpred[BPredictionMode.B_HE_PRED.ordinal()] = new HEPredictor4x4();
        bpred[BPredictionMode.B_LD_PRED.ordinal()] = new D45EPredictor4x4();
        bpred[BPredictionMode.B_RD_PRED.ordinal()] = new D135Predictor4x4();
        bpred[BPredictionMode.B_VR_PRED.ordinal()] = new D117Predictor4x4();
        bpred[BPredictionMode.B_VL_PRED.ordinal()] = new D63EPredictor4x4();
        bpred[BPredictionMode.B_HD_PRED.ordinal()] = new D153Predictor4x4();
        bpred[BPredictionMode.B_HU_PRED.ordinal()] = new D207Predictor4x4();
    }

}
