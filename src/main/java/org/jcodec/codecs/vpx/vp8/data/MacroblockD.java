package org.jcodec.codecs.vpx.vp8.data;

import java.util.Arrays;

import org.jcodec.codecs.vpx.vp8.enums.CompressMode;
import org.jcodec.codecs.vpx.vp8.enums.FrameType;
import org.jcodec.codecs.vpx.vp8.enums.MBLvlFeatures;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessGenArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.subpixfns.SubpixFN;

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
public class MacroblockD {

    public static final int USHIFT = 256;
    public static final int VSHIFT = 320;
    public static final int Y2SHIFT = 384;

    public FullAccessIntArrPointer predictor = new FullAccessIntArrPointer(384);// uchar
    public FullAccessIntArrPointer qcoeff = new FullAccessIntArrPointer(400);// short
    FullAccessIntArrPointer dqcoeff = new FullAccessIntArrPointer(400);// short
    public FullAccessIntArrPointer eobs = new FullAccessIntArrPointer(25); // byte

    public FullAccessIntArrPointer dequant_y1 = new FullAccessIntArrPointer(16); // short
    public FullAccessIntArrPointer dequant_y1_dc = new FullAccessIntArrPointer(16); // short
    public FullAccessIntArrPointer dequant_y2 = new FullAccessIntArrPointer(16); // short
    public FullAccessIntArrPointer dequant_uv = new FullAccessIntArrPointer(16); // short

    /* 16 Y blocks, 4 U, 4 V, 1 DC 2nd order block, each with 16 entries. */
    public FullAccessGenArrPointer<BlockD> block = new FullAccessGenArrPointer<BlockD>(25);
    public int fullpixel_mask;

    public YV12buffer pre; /* Filtered copy of previous frame reconstruction */
    public YV12buffer dst;

    public FullAccessGenArrPointer<ModeInfo> mode_info_context;
    public int mode_info_stride;

    public FrameType frame_type;

    public boolean up_available;
    public boolean left_available;

    int[][] recon_above = new int[3][]; // uchar
    int[][] recon_left = new int[3][]; // uchar
    int[] recon_left_stride = new int[2];

    /* Y,U,V,Y2 */
    public FullAccessGenArrPointer<EntropyContextPlanes> above_context;
    public EntropyContextPlanes left_context;

    /*
     * 0 indicates segmentation at MB level is not enabled. Otherwise the individual
     * bits indicate which features are active.
     */
    public int segmentation_enabled; // uchar

    /* 0 (do not update) 1 (update) the macroblock segmentation map. */
    public boolean update_mb_segmentation_map; // uchar

    /* 0 (do not update) 1 (update) the macroblock segmentation feature data. */
    public boolean update_mb_segmentation_data; // uchar

    /* 0 (do not update) 1 (update) the macroblock segmentation feature data. */
    public boolean mb_segement_abs_delta; // uchar

    /*
     * Per frame flags that define which MB level features (such as quantizer or
     * loop filter level)
     */
    /*
     * are enabled and when enabled the proabilities used to decode the per MB flags
     * in MB_MODE_INFO
     */
    /* Probability Tree used to code Segment number */
    public int[] mb_segment_tree_probs = new int[BlockD.MB_FEATURE_TREE_PROBS];
    /* Segment parameters */
    public short[][] segment_feature_data = new short[MBLvlFeatures.featureCount][BlockD.MAX_MB_SEGMENTS];

    /* mode_based Loop filter adjustment */
    public boolean mode_ref_lf_delta_enabled; // uchar
    public boolean mode_ref_lf_delta_update; // uchar

    /* Delta values have the range +/- MAX_LOOP_FILTER */
    public byte[] last_ref_lf_deltas = new byte[BlockD.MAX_REF_LF_DELTAS]; /* 0 = Intra, Last, GF, ARF */
    public byte[] ref_lf_deltas = new byte[BlockD.MAX_REF_LF_DELTAS]; /* 0 = Intra, Last, GF, ARF */
    /* 0 = BPRED, ZERO_MV, MV, SPLIT */
    public byte[] last_mode_lf_deltas = new byte[BlockD.MAX_MODE_LF_DELTAS];
    public byte[] mode_lf_deltas = new byte[BlockD.MAX_MODE_LF_DELTAS]; /*
                                                                         * 0 = BPRED, ZERO_MV, MV, SPLIT
                                                                         */

    /* Distance of MB away from frame edges */
    public int mb_to_left_edge;
    public int mb_to_right_edge;
    public int mb_to_top_edge;
    public int mb_to_bottom_edge;

    public SubpixFN subpixel_predict;
    public SubpixFN subpixel_predict8x4;
    public SubpixFN subpixel_predict8x8;
    public SubpixFN subpixel_predict16x16;

    int corrupted;

    public FullAccessIntArrPointer y_buf = new FullAccessIntArrPointer(22 * 38); // uchar

    private FullAccessIntArrPointer getArbitraryFreshPtr(int shift) {
        return predictor.shallowCopyWithPosInc(shift);
    }

    public FullAccessIntArrPointer getFreshUPredPtr() {
        return getArbitraryFreshPtr(USHIFT);
    }

    public FullAccessIntArrPointer getFreshVPredPtr() {
        return getArbitraryFreshPtr(VSHIFT);
    }

    public MacroblockD(Compressor cpi) {
        setup_features(cpi);
        vp8_setup_block_dptrs();
    }

    private void vp8_setup_block_dptrs() {
        int r, c;

        for (r = 0; r < 4; ++r) {
            for (c = 0; c < 4; ++c) {
                block.setRel(r * 4 + c, new BlockD(predictor.shallowCopyWithPosInc(r * 4 * 16 + c * 4)));
            }
        }

        for (r = 0; r < 2; ++r) {
            for (c = 0; c < 2; ++c) {
                block.setRel(16 + r * 2 + c, new BlockD(getFreshUPredPtr().shallowCopyWithPosInc(r * 4 * 8 + c * 4)));
            }
        }

        for (r = 0; r < 2; ++r) {
            for (c = 0; c < 2; ++c) {
                block.setRel(20 + r * 2 + c, new BlockD(getFreshVPredPtr().shallowCopyWithPosInc(r * 4 * 8 + c * 4)));
            }
        }
        block.setRel(24, new BlockD(null));

        for (r = 0; r < 25; ++r) {
            block.getRel(r).qcoeff = qcoeff.shallowCopyWithPosInc(r * 16);
            block.getRel(r).dqcoeff = dqcoeff.shallowCopyWithPosInc(r * 16);
            block.getRel(r).eob = eobs.shallowCopyWithPosInc(r);
        }
    }

    public void setup_features(Compressor cpi) {
        // If segmentation enabled set the update flags
        if (segmentation_enabled != 0) {
            update_mb_segmentation_map = true;
            update_mb_segmentation_data = true;
        } else {
            update_mb_segmentation_map = false;
            update_mb_segmentation_data = false;
        }

        mode_ref_lf_delta_enabled = false;
        mode_ref_lf_delta_update = false;
        Arrays.fill(ref_lf_deltas, (byte) 0);
        Arrays.fill(mode_lf_deltas, (byte) 0);
        Arrays.fill(last_ref_lf_deltas, (byte) 0);
        Arrays.fill(last_mode_lf_deltas, (byte) 0);
        set_default_lf_deltas(cpi);
    }

    private void set_default_lf_deltas(Compressor cpi) {
        mode_ref_lf_delta_enabled = true;
        mode_ref_lf_delta_update = true;
        Arrays.fill(ref_lf_deltas, (byte) 0);
        Arrays.fill(mode_lf_deltas, (byte) 0);

        /* Test of ref frame deltas */
        ref_lf_deltas[MVReferenceFrame.INTRA_FRAME.ordinal()] = 2;
        ref_lf_deltas[MVReferenceFrame.LAST_FRAME.ordinal()] = 0;
        ref_lf_deltas[MVReferenceFrame.GOLDEN_FRAME.ordinal()] = -2;
        ref_lf_deltas[MVReferenceFrame.ALTREF_FRAME.ordinal()] = -2;

        mode_lf_deltas[0] = 4; /* BPRED */

        if (cpi.oxcf.Mode == CompressMode.REALTIME) {
            mode_lf_deltas[1] = -12; /* Zero */
        } else {
            mode_lf_deltas[1] = -2; /* Zero */
        }

        mode_lf_deltas[2] = 2; /* New mv */
        mode_lf_deltas[3] = 4; /* Split mv */
    }

    public void init_encode_frame_mbd_context(Compressor cpi) {
        CommonData cm = cpi.common;
        mode_info_context = cm.mi.shallowCopy();
        mode_info_stride = cm.mode_info_stride;
        frame_type = cm.frame_type;
        /* Copy data over into macro block data structures. */
        pre = cm.yv12_fb[cm.frameIdxs.get(MVReferenceFrame.LAST_FRAME)].shallowCopy();
        dst = cm.yv12_fb[cm.new_fb_idx].shallowCopy();
        ModeInfo mi = mode_info_context.get();
        mi.mbmi.mode = MBPredictionMode.DC_PRED;
        mi.mbmi.uv_mode = MBPredictionMode.DC_PRED;
        left_context = cm.left_context;
        fullpixel_mask = 0xffffffff;
        if (cm.full_pixel)
            fullpixel_mask = 0xfffffff8;
    }

    public boolean hasSecondOrder() {
        return ModeInfo.hasSecondOrder(mode_info_context);
    }

    public void vp8_build_block_doffsets() {
        int blockNo;

        for (blockNo = 0; blockNo < 16; ++blockNo) /* y blocks */
        {
            block.getRel(blockNo).calcBlockYOffset(blockNo, dst.y_stride);
        }

        for (; blockNo < 20; ++blockNo) /* U and V blocks */
        {
            block.getRel(blockNo).calcBlockUVOffset(blockNo, dst.uv_stride);
            block.getRel(blockNo + 4).calcBlockUVOffset(blockNo, dst.uv_stride);
        }
    }

}
