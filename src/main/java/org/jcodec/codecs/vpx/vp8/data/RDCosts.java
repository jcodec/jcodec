package org.jcodec.codecs.vpx.vp8.data;

import java.util.EnumMap;

import org.jcodec.codecs.vpx.VP8Util;
import org.jcodec.codecs.vpx.vp8.TreeWriter;
import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.FrameType;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.TokenAlphabet;
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
public class RDCosts {
    FullAccessIntArrPointer[] mvcosts = new FullAccessIntArrPointer[2];
    FullAccessIntArrPointer[] mvsadcosts = new FullAccessIntArrPointer[2];
    EnumMap<FrameType, EnumMap<MBPredictionMode, Integer>> mbmode_cost = new EnumMap<FrameType, EnumMap<MBPredictionMode, Integer>>(
            FrameType.class);
    int[][] intra_uv_mode_cost = new int[2][MBPredictionMode.count];
    EnumMap<BPredictionMode, EnumMap<BPredictionMode, EnumMap<BPredictionMode, Integer>>> bmode_costs = new EnumMap<BPredictionMode, EnumMap<BPredictionMode, EnumMap<BPredictionMode, Integer>>>(
            BPredictionMode.class);
    EnumMap<BPredictionMode, Integer> inter_bmode_costs = new EnumMap<BPredictionMode, Integer>(BPredictionMode.class);
    int[][][][] token_costs = new int[Entropy.BLOCK_TYPES][Entropy.COEF_BANDS][Entropy.PREV_COEF_CONTEXTS][TokenAlphabet.entropyTokenCount];

    private EnumMap<FrameType, int[]> costArrayCache = new EnumMap<FrameType, int[]>(FrameType.class);
    private int[] bpmHelper = new int[BPredictionMode.bpredModecount];

    public RDCosts() {
        initcostarr(mvcosts, EntropyMV.MVvals + 1);
        initcostarr(mvsadcosts, EntropyMV.MVfpvals + 1);
        for (FrameType ft : FrameType.values()) {
            mbmode_cost.put(ft, new EnumMap<MBPredictionMode, Integer>(MBPredictionMode.class));
        }
        for (BPredictionMode bpm : BPredictionMode.values()) {
            EnumMap<BPredictionMode, EnumMap<BPredictionMode, Integer>> submap = new EnumMap<BPredictionMode, EnumMap<BPredictionMode, Integer>>(
                    BPredictionMode.class);
            for (BPredictionMode b2pm : BPredictionMode.values()) {
                submap.put(b2pm, new EnumMap<BPredictionMode, Integer>(BPredictionMode.class));
            }
            bmode_costs.put(bpm, submap);
        }
    }

    private static void initcostarr(FullAccessIntArrPointer[] carr, int size) {
        carr[0] = new FullAccessIntArrPointer(size);
        carr[1] = new FullAccessIntArrPointer(size);
    }

    public int[] getMBmodeCostAsArray(FrameType t) {
        int[] ret = costArrayCache.get(t);
        if (ret == null) {
            ret = new int[MBPredictionMode.count];
            costArrayCache.put(t, ret);
        }
        EnumMap<MBPredictionMode, Integer> data = mbmode_cost.get(t);
        for (MBPredictionMode mode : MBPredictionMode.validModes) {
            Integer v = data.get(mode);
            ret[mode.ordinal()] = v == null ? 0 : v;
        }
        return ret;
    }

    public void setMBmodeCostAsArray(FrameType t) {
        EnumMap<MBPredictionMode, Integer> data = mbmode_cost.get(t);
        int i = 0;
        int[] arr = costArrayCache.get(t);
        for (MBPredictionMode mode : MBPredictionMode.validModes) {
            data.put(mode, arr[i++]);
        }
    }

    private void bpmMaptoArray(final EnumMap<BPredictionMode, Integer> data) {
        for (BPredictionMode mode : BPredictionMode.validmodes) {
            Integer v = data.get(mode);
            bpmHelper[mode.ordinal()] = v == null ? 0 : v;
        }
    }

    private void arraytoBpmMap(final EnumMap<BPredictionMode, Integer> data) {
        int i = 0;
        for (BPredictionMode mode : BPredictionMode.validmodes) {
            data.put(mode, bpmHelper[i++]);
        }
    }

    public void vp8_init_mode_costs(Compressor c) {
        int[] temp;
        ReadOnlyIntArrPointer T = EntropyMode.vp8_bmode_tree;
        for (BPredictionMode bpO : BPredictionMode.bintramodes) {
            for (BPredictionMode bpI : BPredictionMode.bintramodes) {
                bpmMaptoArray(bmode_costs.get(bpO).get(bpI));
                TreeWriter.vp8_cost_tokens(bpmHelper, FullAccessIntArrPointer.toPointer(
                        VP8Util.SubblockConstants.keyFrameSubblockModeProb[bpO.ordinal()][bpI.ordinal()]), T);
                arraytoBpmMap(bmode_costs.get(bpO).get(bpI));
            }
        }

        bpmMaptoArray(inter_bmode_costs);
        TreeWriter.vp8_cost_tokens(bpmHelper, c.common.fc.bmode_prob, T);
        TreeWriter.vp8_cost_tokens(bpmHelper, c.common.fc.sub_mv_ref_prob, EntropyMode.vp8_sub_mv_ref_tree);
        arraytoBpmMap(inter_bmode_costs);

        temp = getMBmodeCostAsArray(FrameType.INTER_FRAME);
        TreeWriter.vp8_cost_tokens(temp, c.common.fc.ymode_prob, EntropyMode.vp8_ymode_tree);
        setMBmodeCostAsArray(FrameType.INTER_FRAME);
        temp = getMBmodeCostAsArray(FrameType.KEY_FRAME);
        TreeWriter.vp8_cost_tokens(temp, EntropyMode.vp8_kf_ymode_prob, EntropyMode.vp8_kf_ymode_tree);
        setMBmodeCostAsArray(FrameType.KEY_FRAME);

        TreeWriter.vp8_cost_tokens(intra_uv_mode_cost[1], c.common.fc.uv_mode_prob, EntropyMode.vp8_uv_mode_tree);
        TreeWriter.vp8_cost_tokens(intra_uv_mode_cost[0], EntropyMode.vp8_kf_uv_mode_prob,
                EntropyMode.vp8_uv_mode_tree);
    }

}
