package org.jcodec.codecs.vpx.vp8.data;

import java.util.EnumMap;
import java.util.EnumSet;

import org.jcodec.codecs.vpx.vp8.BitStream;
import org.jcodec.codecs.vpx.vp8.CommonUtils;
import org.jcodec.codecs.vpx.vp8.DCT;
import org.jcodec.codecs.vpx.vp8.MComp;
import org.jcodec.codecs.vpx.vp8.OnyxIf;
import org.jcodec.codecs.vpx.vp8.PickInter;
import org.jcodec.codecs.vpx.vp8.Quantize;
import org.jcodec.codecs.vpx.vp8.ReconIntra;
import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.FrameType;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.enums.SearchMethods;
import org.jcodec.codecs.vpx.vp8.enums.TokenAlphabet;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessGenArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;

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
public class Macroblock {
    public static final int MAX_MODES = 20;
    public static final int MAX_ERROR_BINS = 1024;
    public FullAccessIntArrPointer src_diff = new FullAccessIntArrPointer(400); /* 25 blocks Y,U,V,Y2 */ // short
    public FullAccessIntArrPointer coeff = new FullAccessIntArrPointer(400); /* 25 blocks Y,U,V,Y2 */ // short
    public FullAccessIntArrPointer thismb = new FullAccessIntArrPointer(256); // uchar

    FullAccessIntArrPointer thismb_ptr; // pos in thismb

    /* 16 Y, 4 U, 4 V, 1 DC 2nd order block */
    public FullAccessGenArrPointer<Block> block = new FullAccessGenArrPointer<Block>(25);

    public YV12buffer src;

    public MacroblockD e_mbd;
    public FullAccessGenArrPointer<Partition_Info> partition_info; /* work pointer */
    public FullAccessGenArrPointer<Partition_Info> pi; /* Corresponds to upper left visible macroblock */
    public FullAccessGenArrPointer<Partition_Info> pip; /* Base of allocated array */

    public int[] ref_frame_cost = new int[MVReferenceFrame.count];

    public FullAccessGenArrPointer<SearchSite> ss = new FullAccessGenArrPointer<SearchSite>(
            (MComp.MAX_MVSEARCH_STEPS * 8) + 1);
    public int ss_count;
    public int searches_per_step;

    public int errorperbit;
    public int sadperbit16;
    public int sadperbit4;
    public int rddiv;
    public int rdmult;
    public FullAccessIntArrPointer mb_activity_ptr; // uint
    int[] mb_norm_activity_ptr;
    public int act_zbin_adj;
    public int last_act_zbin_adj;

    public FullAccessIntArrPointer[] mvcost = new FullAccessIntArrPointer[2];
    public FullAccessIntArrPointer[] mvsadcost = new FullAccessIntArrPointer[2];

    public EnumMap<FrameType, EnumMap<MBPredictionMode, Integer>> mbmode_cost;
    public int[][] intra_uv_mode_cost = new int[MBPredictionMode.count][];
    public EnumMap<BPredictionMode, EnumMap<BPredictionMode, EnumMap<BPredictionMode, Integer>>> bmode_costs;
    public EnumMap<BPredictionMode, Integer> inter_bmode_costs;
    public int[][][][] token_costs = new int[Entropy.COEF_BANDS][Entropy.PREV_COEF_CONTEXTS][TokenAlphabet.entropyTokenCount][];

    /*
     * These define limits to motion vector components to prevent them from
     * extending outside the UMV borders.
     */
    public short mv_col_min;
    public short mv_col_max;
    public short mv_row_min;
    public short mv_row_max;

    public boolean skip;

    public int encode_breakout; // uint

    public FullAccessIntArrPointer gf_active_ptr;

    public FullAccessIntArrPointer active_ptr; // uchar*
    MVContext[] mvc;

    public boolean optimize;
    public int q_index;
    public boolean is_skin;
    int denoise_zeromv;

    int increase_denoising;
    MBPredictionMode best_sse_inter_mode;
    MV best_sse_mv;
    MVReferenceFrame best_reference_frame;
    MVReferenceFrame best_zeromv_reference_frame;
    int need_to_clamp_best_mvs; // uchar

    public int skip_true_count;
    public int[][][][] coef_counts = new int[Entropy.BLOCK_TYPES][Entropy.COEF_BANDS][Entropy.PREV_COEF_CONTEXTS][TokenAlphabet.entropyTokenCount];
    public int[][] MVcount = new int[2][EntropyMV.MVvals]; /*
                                                            * (row , col) MV cts this frame
                                                            */
    public int[] ymode_count = new int[BlockD.VP8_YMODES]; /* intra MB type cts this frame */
    public int[] uv_mode_count = new int[BlockD.VP8_UV_MODES]; /* intra MB type cts this frame */
    public long prediction_error;
    public long intra_error;
    public EnumMap<MVReferenceFrame, Integer> count_mb_ref_frame_usage = new EnumMap<MVReferenceFrame, Integer>(
            MVReferenceFrame.class);

    public int[] rd_thresh_mult = new int[MAX_MODES];
    public int[] rd_threshes = new int[MAX_MODES];
    public int mbs_tested_so_far; // uint
    public int[] mode_test_hit_counts = new int[MAX_MODES];
    public boolean zbin_mode_boost_enabled;
    public int zbin_mode_boost;
    public int last_zbin_mode_boost;

    public int last_zbin_over_quant;
    public int zbin_over_quant;
    public int[] error_bins = new int[MAX_ERROR_BINS];

    public DCT.TRANSFORM short_fdct4x4, short_fdct8x4, short_walsh4x4;

    public Quantize.Quant quantize_b;

    public int mbs_zero_last_dot_suppress;
    public boolean zero_last_dot_suppress;
    public final ReconIntra recon = new ReconIntra();
    public final PickInter interPicker = new PickInter();
    public final HexSearch hex = new HexSearch();

    public Macroblock(Compressor cpi) {
        /* setup RD costs to MACROBLOCK struct */
        mvcost[0] = cpi.rd_costs.mvcosts[0].shallowCopyWithPosInc(EntropyMV.mv_max + 1);
        mvcost[1] = cpi.rd_costs.mvcosts[1].shallowCopyWithPosInc(EntropyMV.mv_max + 1);
        mvsadcost[0] = cpi.rd_costs.mvsadcosts[0].shallowCopyWithPosInc(EntropyMV.mvfp_max + 1);
        mvsadcost[1] = cpi.rd_costs.mvsadcosts[1].shallowCopyWithPosInc(EntropyMV.mvfp_max + 1);
        cal_mvsadcosts(this.mvsadcost);
        mbmode_cost = cpi.rd_costs.mbmode_cost;
        intra_uv_mode_cost = cpi.rd_costs.intra_uv_mode_cost;
        bmode_costs = cpi.rd_costs.bmode_costs;
        inter_bmode_costs = cpi.rd_costs.inter_bmode_costs;
        token_costs = cpi.rd_costs.token_costs;
        /* make sure frame 1 is okay */
        error_bins[0] = cpi.common.MBs;
        /* setup block ptrs & offsets */
        vp8_setup_block_ptrs();
        vp8_alloc_partition_data(cpi);
        e_mbd = new MacroblockD(cpi);
        resetSpeedFeatures();
        changeFNs(cpi);

        for (int i = 0; i < rd_thresh_mult.length; ++i) {
            rd_thresh_mult[i] = 128;
        }
        initRefFrameCounts();
    }

    static void cal_mvsadcosts(FullAccessIntArrPointer[] mvsadcost) {
        int i = 1;

        mvsadcost[0].set((short) 300);
        mvsadcost[1].set((short) 300);

        do {
            double z = 256 * (2 * (OnyxIf.log2f(8 * i) + .6));
            mvsadcost[0].setRel(i, (short) z);
            mvsadcost[1].setRel(i, (short) z);
            mvsadcost[0].setRel(-i, (short) z);
            mvsadcost[1].setRel(-i, (short) z);
        } while (++i <= EntropyMV.mvfp_max);
    }

    private void initRefFrameCounts() {
        for (MVReferenceFrame rf : MVReferenceFrame.validFrames) {
            count_mb_ref_frame_usage.put(rf, 0);
        }
    }

    private void vp8_setup_block_ptrs() {
        int r, c;
        int i;

        for (r = 0; r < 4; ++r) {
            for (c = 0; c < 4; ++c) {
                block.setRel(r * 4 + c, new Block(src_diff.shallowCopyWithPosInc(r * 4 * 16 + c * 4)));
            }
        }

        for (r = 0; r < 2; ++r) {
            for (c = 0; c < 2; ++c) {
                block.setRel(16 + r * 2 + c,
                        new Block(src_diff.shallowCopyWithPosInc(MacroblockD.USHIFT + r * 4 * 8 + c * 4)));
            }
        }

        for (r = 0; r < 2; ++r) {
            for (c = 0; c < 2; ++c) {
                block.setRel(20 + r * 2 + c,
                        new Block(src_diff.shallowCopyWithPosInc(MacroblockD.VSHIFT + r * 4 * 8 + c * 4)));
            }
        }

        block.setRel(24, new Block(src_diff.shallowCopyWithPosInc(MacroblockD.Y2SHIFT)));

        for (i = 0; i < 25; ++i) {
            block.getRel(i).coeff = coeff.shallowCopyWithPosInc(i * 16);
        }
    }

    private void vp8_alloc_partition_data(Compressor cpi) {

        pip = new FullAccessGenArrPointer<Partition_Info>((cpi.common.mb_cols + 1) * (cpi.common.mb_rows + 1));
        for (int i = 0; i < pip.size(); i++) {
            pip.setRel(i, new Partition_Info());
        }
        pi = pip.shallowCopyWithPosInc(cpi.common.mode_info_stride + 1);
    }

    public void resetSpeedFeatures() {
        mbs_tested_so_far = 0;
        mbs_zero_last_dot_suppress = 0;
    }

    private void prepInitMotionComp() {
        ss_count = 1;
        ss.set(new SearchSite(0, 0, 0));
    }

    private void init_addBasicSearchSites(int stride, int Len) {
        ss.incBy(ss_count);
        ss.setAndInc(new SearchSite(-Len, 0, -Len * stride));
        ss.setAndInc(new SearchSite(Len, 0, Len * stride));
        ss.setAndInc(new SearchSite(0, -Len, -Len));
        ss.setAndInc(new SearchSite(0, Len, Len));
        ss_count += 4;
        ss.rewind();
    }

    private void vp8_init3smotion_compensation(int stride) {
        prepInitMotionComp();
        /* Generate offsets for 8 search sites per step. */
        int Len = MComp.MAX_FIRST_STEP;

        while (Len > 0) {
            /* Compute offsets for search sites. */
            init_addBasicSearchSites(stride, Len);
            ss.incBy(ss_count);
            ss.setAndInc(new SearchSite(-Len, -Len, -Len * stride - Len));
            ss.setAndInc(new SearchSite(-Len, Len, -Len * stride + Len));
            ss.setAndInc(new SearchSite(Len, -Len, Len * stride - Len));
            ss.setAndInc(new SearchSite(Len, Len, Len * stride + Len));
            ss.rewind();
            ss_count += 4;

            /* Contract. */
            Len >>= 1;
        }
        searches_per_step = 8;
    }

    private void vp8_init_dsmotion_compensation(int stride) {
        prepInitMotionComp();
        /* Generate offsets for 4 search sites per step. */
        int Len = MComp.MAX_FIRST_STEP;

        while (Len > 0) {
            init_addBasicSearchSites(stride, Len);

            /* Contract. */
            Len >>= 1;
        }

        searches_per_step = 4;
    }

    public void changeFNs(Compressor cpi) {
        CommonData cm = cpi.common;
        if (cpi.sf.search_method == SearchMethods.NSTEP) {
            vp8_init3smotion_compensation(cm.yv12_fb[cm.frameIdxs.get(MVReferenceFrame.LAST_FRAME)].y_stride);
        } else if (cpi.sf.search_method == SearchMethods.DIAMOND) {
            vp8_init_dsmotion_compensation(cm.yv12_fb[cm.frameIdxs.get(MVReferenceFrame.LAST_FRAME)].y_stride);
        }
        if (cpi.sf.improved_dct) {
            short_fdct8x4 = DCT.vp8_short_fdct8x4;
            short_fdct4x4 = DCT.vp8_short_fdct4x4;
        } else {
            /* No fast FDCT defined for any platform at this time. */
            short_fdct8x4 = DCT.vp8_short_fdct8x4;
            short_fdct4x4 = DCT.vp8_short_fdct4x4;
        }

        short_walsh4x4 = DCT.vp8_short_walsh4x4;

        if (cpi.sf.improved_quant) {
            quantize_b = Quantize.regularQuant;
        } else {
            quantize_b = Quantize.fastQuant;
        }
        optimize = cpi.sf.optimize_coefficients;
    }

    /* values are now correlated to quantizer */
    static final int[] sad_per_bit16lut = { 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6,
            6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 9, 9,
            9, 9, 9, 9, 9, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 11, 12, 12, 12, 12, 12, 12, 13, 13, 13,
            13, 14, 14 };
    static final int[] sad_per_bit4lut = { 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4,
            4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8,
            8, 8, 8, 8, 9, 9, 9, 9, 9, 9, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 11, 11, 11, 12, 12, 12,
            12, 12, 12, 12, 12, 13, 13, 13, 13, 13, 13, 13, 14, 14, 14, 14, 14, 15, 15, 15, 15, 16, 16, 16, 16, 17, 17,
            17, 18, 18, 18, 19, 19, 19, 20, 20, 20, };

    public void vp8cx_initialize_me_consts(int QIndex) {
        sadperbit16 = sad_per_bit16lut[QIndex];
        sadperbit4 = sad_per_bit4lut[QIndex];
    }

    public ReferenceCounts sumReferenceCounts() {
        int inter = 0;
        for (MVReferenceFrame rf : EnumSet.range(MVReferenceFrame.LAST_FRAME, MVReferenceFrame.ALTREF_FRAME)) {
            inter += count_mb_ref_frame_usage.get(rf);
        }
        return new ReferenceCounts(count_mb_ref_frame_usage.get(MVReferenceFrame.INTRA_FRAME), inter);
    }

    public void init_encode_frame_mb_context(Compressor cpi) {
        /* GF active flags data structure */
        gf_active_ptr = cpi.gf_active_flags.shallowCopy();

        /* Activity map pointer */
        mb_activity_ptr = cpi.mb_activity_map.shallowCopy();

        act_zbin_adj = 0;

        partition_info = pi.shallowCopy();
        src = cpi.sourceYV12.shallowCopy();
        vp8_build_block_offsets();
        mvc = cpi.common.fc.mvc;
        /*
         * Special case treatment when GF and ARF are not sensible options for reference
         */
        if (cpi.ref_frame_flags.contains(MVReferenceFrame.LAST_FRAME) && cpi.ref_frame_flags.size() == 1) {
            BitStream.vp8_calc_ref_frame_costs(ref_frame_cost, cpi.prob_intra_coded, 255, 128);
        } else if ((cpi.oxcf.number_of_layers > 1) && (cpi.ref_frame_flags.contains(MVReferenceFrame.GOLDEN_FRAME))
                && cpi.ref_frame_flags.size() == 1) {
            BitStream.vp8_calc_ref_frame_costs(ref_frame_cost, cpi.prob_intra_coded, 1, 255);
        } else if ((cpi.oxcf.number_of_layers > 1) && (cpi.ref_frame_flags.contains(MVReferenceFrame.ALTREF_FRAME))
                && cpi.ref_frame_flags.size() == 1) {
            BitStream.vp8_calc_ref_frame_costs(ref_frame_cost, cpi.prob_intra_coded, 1, 1);
        } else {
            BitStream.vp8_calc_ref_frame_costs(ref_frame_cost, cpi.prob_intra_coded, cpi.prob_last_coded,
                    cpi.prob_gf_coded);
        }
        CommonUtils.vp8_zero(coef_counts);
        CommonUtils.vp8_zero(ymode_count);
        CommonUtils.vp8_zero(uv_mode_count);
        prediction_error = 0;
        intra_error = 0;
        initRefFrameCounts();
    }

    void vp8_build_block_offsets() {
        int br, bc;

        e_mbd.vp8_build_block_doffsets();

        /* y blocks */
        thismb_ptr = thismb.shallowCopy();
        for (br = 0; br < 4; ++br) {
            for (bc = 0; bc < 4; ++bc) {
                Block this_block = block.getAndInc();
                this_block.base_src = thismb_ptr;
                this_block.src_stride = 16;
                this_block.src = 4 * br * 16 + 4 * bc;
            }
        }

        /* u blocks */
        for (br = 0; br < 2; ++br) {
            for (bc = 0; bc < 2; ++bc) {
                Block this_block = block.getAndInc();
                this_block.base_src = src.u_buffer;
                this_block.src_stride = src.uv_stride;
                this_block.src = 4 * br * this_block.src_stride + 4 * bc;
            }
        }

        /* v blocks */
        for (br = 0; br < 2; ++br) {
            for (bc = 0; bc < 2; ++bc) {
                Block this_block = block.getAndInc();
                this_block.base_src = src.v_buffer;
                this_block.src_stride = src.uv_stride;
                this_block.src = 4 * br * this_block.src_stride + 4 * bc;
            }
        }
        block.rewind();
    }

}
