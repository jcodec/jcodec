package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Sub macroblock devision type
 * 
 * @author Jay Codec
 * 
 */
public enum SubMBType {

    L0_8x8(1, MBPartPredMode.Pred_L0, 8, 8), L0_8x4(2, MBPartPredMode.Pred_L0, 8, 4), L0_4x8(2, MBPartPredMode.Pred_L0,
            4, 8), L0_4x4(4, MBPartPredMode.Pred_L0, 4, 4), L1_8x8(1, MBPartPredMode.Pred_L1, 8, 8), L1_8x4(2,
            MBPartPredMode.Pred_L1, 8, 4), L1_4x8(2, MBPartPredMode.Pred_L1, 4, 8), L1_4x4(4, MBPartPredMode.Pred_L1,
            4, 4), Bi_8x8(1, MBPartPredMode.BiPred, 8, 8), Bi_8x4(2, MBPartPredMode.BiPred, 8, 4), Bi_4x8(2,
            MBPartPredMode.BiPred, 4, 8), Bi_4x4(4, MBPartPredMode.BiPred, 4, 4), Direct_8x8(4, MBPartPredMode.Direct,
            4, 4);

    private int numParts;
    private MBPartPredMode predMode;
    private int partWidth;
    private int partHeight;

    private SubMBType(int numParts, MBPartPredMode predMode, int partWidth, int partHeight) {
        this.numParts = numParts;
        this.predMode = predMode;
        this.partWidth = partWidth;
        this.partHeight = partHeight;
    }

    public int getNumParts() {
        return numParts;
    }

    public MBPartPredMode getPredMode() {
        return predMode;
    }

    public int getPartWidth() {
        return partWidth;
    }

    public int getPartHeight() {
        return partHeight;
    }
}
