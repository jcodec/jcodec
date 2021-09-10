package org.jcodec.codecs.vpx.vp8.enums;

import java.util.EnumSet;

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
public enum MBPredictionMode {
    DC_PRED, /* average of above and left pixels */
    V_PRED, /* vertical prediction */
    H_PRED, /* horizontal prediction */
    TM_PRED, /* Truemotion prediction */
    B_PRED, /* block based prediction, each block has its own prediction mode */

    NEARESTMV, NEARMV, ZEROMV, NEWMV, SPLITMV;

    public static EnumSet<MBPredictionMode> has_no_y_block = EnumSet.of(B_PRED, SPLITMV);
    public static EnumSet<MBPredictionMode> basicModes = EnumSet.of(DC_PRED, V_PRED, H_PRED, TM_PRED, B_PRED);
    public static EnumSet<MBPredictionMode> mvModes = EnumSet.of(NEARESTMV, NEARMV, ZEROMV, NEWMV, SPLITMV);
    public static EnumSet<MBPredictionMode> validModes = EnumSet.range(DC_PRED, SPLITMV);
    public static EnumSet<MBPredictionMode> nonBlockPred = EnumSet.range(DC_PRED, TM_PRED);

    public static final int count = validModes.size();
}
