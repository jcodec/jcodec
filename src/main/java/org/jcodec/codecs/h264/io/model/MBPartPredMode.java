package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Macroblock part prediction mode enum
 * 
 * @author Jay Codec
 * 
 */
public class MBPartPredMode {
    public final static MBPartPredMode Intra_16x16 = new MBPartPredMode();
    public static final MBPartPredMode Intra_4x4 = new MBPartPredMode();
    public static final MBPartPredMode Intra_8x8 = new MBPartPredMode();
    public static final MBPartPredMode Direct = new MBPartPredMode();
    public static final MBPartPredMode BiPred = new MBPartPredMode();
    public static final MBPartPredMode Pred_L1 = new MBPartPredMode();
    public static final MBPartPredMode Pred_L0 = new MBPartPredMode();
}
