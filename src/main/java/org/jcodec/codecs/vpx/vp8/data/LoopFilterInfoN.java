package org.jcodec.codecs.vpx.vp8.data;

import java.util.Arrays;
import java.util.EnumMap;

import org.jcodec.codecs.vpx.vp8.enums.FrameType;
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
public class LoopFilterInfoN {
    public static final int MAX_LOOP_FILTER = 63;

    public static final int SIMD_WIDTH = 16;

    public short[][] mblim = new short[MAX_LOOP_FILTER + 1][SIMD_WIDTH]; // uchar
    public short[][] blim = new short[MAX_LOOP_FILTER + 1][SIMD_WIDTH]; // uchar
    short[][] lim = new short[MAX_LOOP_FILTER + 1][SIMD_WIDTH]; // uchar
    short[][] hev_thr = new short[4][SIMD_WIDTH]; // uchar

    public short[][][] lvl = new short[4][4][4]; // uchar
    EnumMap<FrameType, short[]> hev_thr_lut = new EnumMap<FrameType, short[]>(FrameType.class);
    public EnumMap<MBPredictionMode, Short> mode_lf_lut = new EnumMap<MBPredictionMode, Short>(MBPredictionMode.class); // uchar

    public LoopFilterInfoN(CommonData cm) {
        vp8_loop_filter_update_sharpness(cm.sharpness_level);
        cm.last_sharpness_level = cm.sharpness_level;
        lf_init_lut();
        for (short i = 0; i < 4; i++) {
            Arrays.fill(hev_thr[i], i);
        }
    }

    private void lf_init_lut() {
        int filt_lvl;
        short[] kfTHRLut = new short[MAX_LOOP_FILTER + 1];
        short[] ifTHRLut = new short[MAX_LOOP_FILTER + 1];

        for (filt_lvl = 0; filt_lvl <= LoopFilterInfoN.MAX_LOOP_FILTER; ++filt_lvl) {
            if (filt_lvl >= 40) {
                kfTHRLut[filt_lvl] = 2;
                ifTHRLut[filt_lvl] = 3;
            } else if (filt_lvl >= 20) {
                kfTHRLut[filt_lvl] = 1;
                ifTHRLut[filt_lvl] = 2;
            } else if (filt_lvl >= 15) {
                kfTHRLut[filt_lvl] = 1;
                ifTHRLut[filt_lvl] = 1;
            } else {
                kfTHRLut[filt_lvl] = 0;
                ifTHRLut[filt_lvl] = 0;
            }
        }
        hev_thr_lut.put(FrameType.KEY_FRAME, kfTHRLut);
        hev_thr_lut.put(FrameType.INTER_FRAME, ifTHRLut);

        mode_lf_lut.put(MBPredictionMode.DC_PRED, (short) 1);
        mode_lf_lut.put(MBPredictionMode.V_PRED, (short) 1);
        mode_lf_lut.put(MBPredictionMode.H_PRED, (short) 1);
        mode_lf_lut.put(MBPredictionMode.TM_PRED, (short) 1);
        mode_lf_lut.put(MBPredictionMode.B_PRED, (short) 0);
        mode_lf_lut.put(MBPredictionMode.ZEROMV, (short) 1);
        mode_lf_lut.put(MBPredictionMode.NEARESTMV, (short) 2);
        mode_lf_lut.put(MBPredictionMode.NEARMV, (short) 2);
        mode_lf_lut.put(MBPredictionMode.NEWMV, (short) 2);
        mode_lf_lut.put(MBPredictionMode.SPLITMV, (short) 3);
    }

    public void vp8_loop_filter_update_sharpness(int sharpness_lvl) {
        int i;

        /* For each possible value for the loop filter fill out limits */
        for (i = 0; i <= LoopFilterInfoN.MAX_LOOP_FILTER; ++i) {
            int filt_lvl = i;
            int block_inside_limit = 0;

            /* Set loop filter paramaeters that control sharpness. */
            block_inside_limit = filt_lvl >> (sharpness_lvl > 0 ? 1 : 0);
            block_inside_limit = block_inside_limit >> (sharpness_lvl > 4 ? 1 : 0);

            if (sharpness_lvl > 0) {
                if (block_inside_limit > (9 - sharpness_lvl)) {
                    block_inside_limit = (9 - sharpness_lvl);
                }
            }

            if (block_inside_limit < 1)
                block_inside_limit = 1;
            Arrays.fill(lim[i], (short) block_inside_limit);
            Arrays.fill(blim[i], (short) (2 * filt_lvl + block_inside_limit));
            Arrays.fill(mblim[i], (short) (2 * (filt_lvl + 2) + block_inside_limit));
        }
    }

}
