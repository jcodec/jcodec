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
public enum BPredictionMode {
    B_DC_PRED, /* average of above and left pixels */
    B_TM_PRED,

    B_VE_PRED, /* vertical prediction */
    B_HE_PRED, /* horizontal prediction */

    B_LD_PRED, B_RD_PRED,

    B_VR_PRED, B_VL_PRED, B_HD_PRED, B_HU_PRED,

    LEFT4X4, ABOVE4X4, ZERO4X4, NEW4X4;

    public static EnumSet<BPredictionMode> fourfour = EnumSet.range(LEFT4X4, NEW4X4);
    public static EnumSet<BPredictionMode> bintramodes = EnumSet.range(B_DC_PRED, B_HU_PRED);
    public static EnumSet<BPredictionMode> validmodes = EnumSet.allOf(BPredictionMode.class);
    public static EnumSet<BPredictionMode> basicbmodes = EnumSet.range(B_DC_PRED, B_HE_PRED);

    public static final int bpredModecount = validmodes.size();
}
