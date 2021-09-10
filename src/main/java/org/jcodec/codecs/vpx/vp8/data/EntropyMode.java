package org.jcodec.codecs.vpx.vp8.data;

import java.util.EnumMap;

import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.Sumvfref;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;
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
public class EntropyMode {
    public static final PositionableIntArrPointer vp8_mbsplit_probs = new PositionableIntArrPointer(
            new short[] { 110, 111, 150 }, 0);

    /**
     *
     * <pre>
     *      <root>
     *     /      \
     * B_DC_PRED   2
     *           /  \
     *  B_TM_PRED    4
     *             /   \
     *     B_VE_PRED    6---------------------+
     *                /                        \        
     *               8                         12
     *             /  \                      /   \
     *     B_HE_PRED  10             B_LD_PRED    14
     *               /  \                       /   \
     *      B_RD_PRED  B_VR_PRED        B_VL_PRED    16
     *                                             /   \
     *                                     B_HD_PRED  B_HU_PRED
     * </pre>
     */
    public static final PositionableIntArrPointer vp8_bmode_tree = new PositionableIntArrPointer(new short[] {
            (short) -BPredictionMode.B_DC_PRED.ordinal(), 2, /*
                                                              * 0 = DC_NODE
                                                              */
            (short) -BPredictionMode.B_TM_PRED.ordinal(), 4, /* 1 = TM_NODE */
            (short) -BPredictionMode.B_VE_PRED.ordinal(), 6, /* 2 = VE_NODE */
            8, 12, /* 3 = COM_NODE */
            (short) -BPredictionMode.B_HE_PRED.ordinal(), 10, /* 4 = HE_NODE */
            (short) -BPredictionMode.B_RD_PRED.ordinal(), (short) -BPredictionMode.B_VR_PRED.ordinal(), /*
                                                                                                         * 5 = RD_NODE
                                                                                                         */
            (short) -BPredictionMode.B_LD_PRED.ordinal(), 14, /* 6 = LD_NODE */
            (short) -BPredictionMode.B_VL_PRED.ordinal(), 16, /* 7 = VL_NODE */
            (short) -BPredictionMode.B_HD_PRED.ordinal(), (short) -BPredictionMode.B_HU_PRED.ordinal() /* 8 = HD_NODE */
    }, 0);

    public static final PositionableIntArrPointer vp8_ymode_tree = new PositionableIntArrPointer(
            new short[] { (short) -MBPredictionMode.DC_PRED.ordinal(), 2, 4, 6,
                    (short) -MBPredictionMode.V_PRED.ordinal(), (short) -MBPredictionMode.H_PRED.ordinal(),
                    (short) -MBPredictionMode.TM_PRED.ordinal(), (short) -MBPredictionMode.B_PRED.ordinal() },
            0);

    public static final PositionableIntArrPointer vp8_kf_ymode_tree = new PositionableIntArrPointer(
            new short[] { (short) -MBPredictionMode.B_PRED.ordinal(), 2, 4, 6,
                    (short) -MBPredictionMode.DC_PRED.ordinal(), (short) -MBPredictionMode.V_PRED.ordinal(),
                    (short) -MBPredictionMode.H_PRED.ordinal(), (short) -MBPredictionMode.TM_PRED.ordinal() },
            0);

    public static final PositionableIntArrPointer vp8_uv_mode_tree = new PositionableIntArrPointer(
            new short[] { (short) -MBPredictionMode.DC_PRED.ordinal(), 2, (short) -MBPredictionMode.V_PRED.ordinal(), 4,
                    (short) -MBPredictionMode.H_PRED.ordinal(), (short) -MBPredictionMode.TM_PRED.ordinal() },
            0);

    public static final PositionableIntArrPointer vp8_mbsplit_tree = new PositionableIntArrPointer(
            new short[] { -3, 2, -2, 4, -0, -1 }, 0);

    public static final PositionableIntArrPointer vp8_mv_ref_tree = new PositionableIntArrPointer(
            new short[] { (short) -MBPredictionMode.ZEROMV.ordinal(), 2, (short) -MBPredictionMode.NEARESTMV.ordinal(),
                    4, (short) -MBPredictionMode.NEARMV.ordinal(), 6, (short) -MBPredictionMode.NEWMV.ordinal(),
                    (short) -MBPredictionMode.SPLITMV.ordinal() },
            0);

    public static final PositionableIntArrPointer vp8_sub_mv_ref_tree = new PositionableIntArrPointer(
            new short[] { (short) -BPredictionMode.LEFT4X4.ordinal(), 2, (short) -BPredictionMode.ABOVE4X4.ordinal(), 4,
                    (short) -BPredictionMode.ZERO4X4.ordinal(), (short) -BPredictionMode.NEW4X4.ordinal() },
            0);

    public static final PositionableIntArrPointer vp8_small_mvtree = new PositionableIntArrPointer(
            new short[] { 2, 8, 4, 6, -0, -1, -2, -3, 10, 12, -4, -5, -6, -7 }, 0);
    public static final int[][] vp8_mbsplits = { { 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 },
            { 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1 }, { 0, 0, 1, 1, 0, 0, 1, 1, 2, 2, 3, 3, 2, 2, 3, 3 },
            { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 } };
    public static final int[] vp8_mbsplit_count = { 2, 2, 4, 16 };
    public static final EnumMap<Sumvfref, ReadOnlyIntArrPointer> vp8_sub_mv_ref_prob2;
    public static final PositionableIntArrPointer vp8_kf_ymode_prob = new PositionableIntArrPointer(
            new short[] { 145, 156, 163, 128 }, 0);
    public static final PositionableIntArrPointer vp8_kf_uv_mode_prob = new PositionableIntArrPointer(
            new short[] { 142, 114, 183 }, 0);
    public static final ReadOnlyIntArrPointer vp8_bmode_prob = new ReadOnlyIntArrPointer(
            new short[] { 120, 90, 79, 133, 87, 85, 80, 111, 151 }, 0);
    public static final ReadOnlyIntArrPointer vp8_ymode_prob = new ReadOnlyIntArrPointer(
            new short[] { 112, 86, 140, 37 }, 0);
    public static final ReadOnlyIntArrPointer vp8_uv_mode_prob = new ReadOnlyIntArrPointer(
            new short[] { 162, 101, 204 }, 0);
    public static final ReadOnlyIntArrPointer sub_mv_ref_prob = new ReadOnlyIntArrPointer(new short[] { 180, 162, 25 },
            0);

    static {
        vp8_sub_mv_ref_prob2 = new EnumMap<Sumvfref, ReadOnlyIntArrPointer>(Sumvfref.class);
        vp8_sub_mv_ref_prob2.put(Sumvfref.NORMAL, new ReadOnlyIntArrPointer(new short[] { 147, 136, 18 }, 0));
        vp8_sub_mv_ref_prob2.put(Sumvfref.LEFT_ZED, new ReadOnlyIntArrPointer(new short[] { 106, 145, 1 }, 0));
        vp8_sub_mv_ref_prob2.put(Sumvfref.ABOVE_ZED, new ReadOnlyIntArrPointer(new short[] { 179, 121, 1 }, 0));
        vp8_sub_mv_ref_prob2.put(Sumvfref.LEFT_ABOVE_SAME, new ReadOnlyIntArrPointer(new short[] { 223, 1, 34 }, 0));
        vp8_sub_mv_ref_prob2.put(Sumvfref.LEFT_ABOVE_ZED, new ReadOnlyIntArrPointer(new short[] { 208, 1, 1 }, 0));
    }

    public static Sumvfref vp8_mv_cont(final MV l, MV a) {
        boolean lez = l.isZero();
        boolean aez = a.isZero();
        boolean lea = l.equals(a);

        if (lea && lez)
            return Sumvfref.LEFT_ABOVE_ZED;

        if (lea)
            return Sumvfref.LEFT_ABOVE_SAME;

        if (aez)
            return Sumvfref.ABOVE_ZED;

        if (lez)
            return Sumvfref.LEFT_ZED;

        return Sumvfref.NORMAL;
    }
}
