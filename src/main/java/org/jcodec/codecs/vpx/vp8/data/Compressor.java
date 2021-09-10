package org.jcodec.codecs.vpx.vp8.data;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import org.jcodec.codecs.vpx.vp8.BitStream;
import org.jcodec.codecs.vpx.vp8.BoolEncoder;
import org.jcodec.codecs.vpx.vp8.MComp;
import org.jcodec.codecs.vpx.vp8.OnyxIf;
import org.jcodec.codecs.vpx.vp8.Quantize;
import org.jcodec.codecs.vpx.vp8.enums.BlockEnum;
import org.jcodec.codecs.vpx.vp8.enums.CompressMode;
import org.jcodec.codecs.vpx.vp8.enums.EndUsage;
import org.jcodec.codecs.vpx.vp8.enums.MBLvlFeatures;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.enums.TokenPartition;
import org.jcodec.codecs.vpx.vp8.enums.Scaling;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessGenArrPointer;
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
public class Compressor {
    public static final int MAX_PARTITIONS = 9;

    public EnumMap<CommonData.Quant, QuantDetails> q = new EnumMap<CommonData.Quant, QuantDetails>(CommonData.Quant.class);

    public Macroblock mb;
    public CommonData common = new CommonData();
    public BoolEncoder[] bc = new BoolEncoder[9]; /* one boolcoder for each partition */

    public Config oxcf;

    public Lookahead lookahead;
    public LookaheadEntry sourceLAE;
    public LookaheadEntry alt_ref_source;
    LookaheadEntry last_source;

    public YV12buffer sourceYV12;
    public YV12buffer un_scaled_source;
    public YV12buffer scaled_source;
    YV12buffer last_frame_unscaled_source;

    public int frames_till_alt_ref_frame;
    /* frame in src_buffers has been identified to be encoded as an alt ref */
    public boolean source_alt_ref_pending;
    /* an alt ref frame has been encoded and is usable */
    public boolean source_alt_ref_active;
    /* source of frame to encode is an exact copy of an alt ref frame */
    public boolean is_src_frame_alt_ref;

    /* golden frame same as last frame ( short circuit gold searches) */
    public boolean gold_is_last;
    /* Alt reference frame same as last ( short circuit altref search) */
    public boolean alt_is_last;
    /* don't do both alt and gold search ( just do gold). */
    public boolean gold_is_alt;

    public YV12buffer pick_lf_lvl_frame;

    public FullAccessGenArrPointer<TokenExtra> tok;
    public int tok_count;

    public int frames_since_key;
    public int key_frame_frequency;
    public boolean this_key_frame_forced;
    int next_key_frame_forced;

    /* Ambient reconstruction err target for force key frames */
    public int ambient_err;

    public int[] mode_check_freq = new int[Block.MAX_MODES];

    public int[] rd_baseline_thresh = new int[Block.MAX_MODES];

    public int RDMULT;
    public int RDDIV;

    public CodingContext coding_context = new CodingContext();

    /* Rate targeting variables */
    public long last_prediction_error;
    public long last_intra_error;

    public int this_frame_target;
    public int projected_frame_size;
    public short[] last_q = new short[2]; /* Separate values for Intra/Inter */

    public double rate_correction_factor;
    public double key_frame_rate_correction_factor;
    public double gf_rate_correction_factor;

    public int frames_since_golden;
    /* Count down till next GF */
    public int frames_till_gf_update_due;

    /* GF interval chosen when we coded the last GF */
    public int current_gf_interval;

    /* Total bits overspent becasue of GF boost (cumulative) */
    public int gf_overspend_bits;

    /*
     * Used in the few frames following a GF to recover the extra bits spent in that
     * GF
     */
    public int non_gf_bitrate_adjustment;

    /* Extra bits spent on key frames that need to be recovered */
    public int kf_overspend_bits;

    /* Current number of bit s to try and recover on each inter frame. */
    public int kf_bitrate_adjustment;
    public int max_gf_interval;
    public int baseline_gf_interval;
    public int active_arnr_frames;

    public long key_frame_count;
    public int[] prior_key_frame_distance = new int[OnyxInt.KEY_FRAME_CONTEXT];
    /* Current section per frame bandwidth target */
    public int per_frame_bandwidth;
    /* Average frame size target for clip */
    public int av_per_frame_bandwidth;
    /* Minimum allocation that should be used for any frame */
    public int min_frame_bandwidth;
    public int inter_frame_target;
    public double output_framerate;
    public long last_time_stamp_seen;
    public long last_end_time_stamp_seen;
    public long first_time_stamp_ever;

    public short ni_av_qi;
    public int ni_tot_qi;
    public int ni_frames;
    public short avg_frame_qindex;

    public long total_byte_count;

    public boolean buffered_mode;

    public double framerate;
    public double ref_framerate;
    public long buffer_level;
    public long bits_off_target;

    public int rolling_target_bits;
    public int rolling_actual_bits;

    public int long_rolling_target_bits;
    public int long_rolling_actual_bits;

    public long total_actual_bits;
    int total_target_vs_actual; /* debug stats */

    public short worst_quality;
    public short active_worst_quality;
    public short best_quality;
    public short active_best_quality;

    public short cq_target_quality;

    public boolean drop_frames_allowed; /* Are we permitted to drop frames? */
    public boolean drop_frame; /* Drop this frame? */
    public short[][][][] frame_coef_probs = new short[Entropy.BLOCK_TYPES][Entropy.COEF_BANDS][Entropy.PREV_COEF_CONTEXTS][Entropy.ENTROPY_NODES];
    short[][][][] update_probs = new short[Entropy.BLOCK_TYPES][Entropy.COEF_BANDS][Entropy.PREV_COEF_CONTEXTS][Entropy.ENTROPY_NODES];

    public int[][][][][] frame_branch_ct = new int[Entropy.BLOCK_TYPES][Entropy.COEF_BANDS][Entropy.PREV_COEF_CONTEXTS][Entropy.ENTROPY_NODES][2];

    int gfu_boost;
    public int kf_boost;
    public int last_boost;

    public int target_bandwidth;
    public List<CodecPkt> output_pkt_list;

    public int decimation_factor;
    public int decimation_count;

    /* for real time encoding */
    public int avg_encode_time; /* microsecond */
    public int avg_pick_mode_time; /* microsecond */
    public int Speed;
    public int compressor_speed;

    public boolean auto_gold;
    public boolean auto_adjust_gold_quantizer;
    public boolean auto_worst_q;
    int cpu_used;

    public int prob_intra_coded;
    public int prob_last_coded;
    public int prob_gf_coded;
    public int prob_skip_false;
    public int[] last_skip_false_probs = new int[3];
    public int[] last_skip_probs_q = new int[3];
    public EnumMap<MVReferenceFrame, Integer> recent_ref_frame_usage = new EnumMap<MVReferenceFrame, Integer>(
            MVReferenceFrame.class);

    public int this_frame_percent_intra;
    public int last_frame_percent_intra;

    public EnumSet<MVReferenceFrame> ref_frame_flags;

    public SpeedFeatures sf = new SpeedFeatures();

    /* Count ZEROMV on all reference frames. */
    public int zeromv_count;
    public int lf_zeromv_pct;

    public boolean[] skin_map; // uchar

    public int[] segmentation_map;
    public short[][] segment_feature_data = new short[MBLvlFeatures.featureCount][BlockD.MAX_MB_SEGMENTS];
    public int[] segment_encode_breakout = new int[BlockD.MAX_MB_SEGMENTS];

    public FullAccessIntArrPointer active_map; // uchar
    public boolean active_map_enabled;

    /*
     * Video conferencing cyclic refresh mode flags. This is a mode designed to
     * clean up the background over time in live encoding scenarious. It uses
     * segmentation.
     */
    public boolean cyclic_refresh_mode_enabled;
    public int cyclic_refresh_mode_max_mbs_perframe;
    public int cyclic_refresh_mode_index;
    public int cyclic_refresh_q;
    public byte[] cyclic_refresh_map;
    // Count on how many (consecutive) times a macroblock uses ZER0MV_LAST.
    public int[] consec_zero_last;// uchar
    // Counter that is reset when a block is checked for a mode-bias against
    // ZEROMV_LASTREF.
    public int[] consec_zero_last_mvbias; // uchar

    // Frame counter for the temporal pattern. Counter is rest when the temporal
    // layers are changed dynamically (run-time change).
    public int temporal_pattern_counter;
    // Temporal layer id.
    public int temporal_layer_id;

    // Measure of average squared difference between source and denoised signal.
    int mse_source_denoised;

    public int force_maxqp;
    public int frames_since_last_drop_overshoot;
    public int last_pred_err_mb;

    // GF update for 1 pass cbr.
    public boolean gf_update_onepass_cbr;
    public int gf_interval_onepass_cbr;
    public boolean gf_noboost_onepass_cbr;

    public TokenList[] tplist;
    public int[] partition_sz = new int[MAX_PARTITIONS];
    int[] partition_d = new int[MAX_PARTITIONS];
    int[] partition_d_end = new int[MAX_PARTITIONS];

    public interface FractionalMVStepIF {
        long call(Macroblock x, Block b, BlockD d, MV bestmv, MV ref_mv, int error_per_bit, VarianceFNs vfp,
                ReadOnlyIntArrPointer[] mvcost, VarianceResults ret);
    }

    public FractionalMVStepIF find_fractional_mv_step;

    public interface FullSearchIF {
        long call(Macroblock x, Block b, BlockD d, MV ref_mv, int sad_per_bit, int distance, VarianceFNs fn_ptr,
                ReadOnlyIntArrPointer[] mvcost, MV center_mv);
    }

    public FullSearchIF full_search_sad;

    public interface RefiningSearchIF {
        long call(Macroblock x, Block b, BlockD d, MV ref_mv, int sad_per_bit, int distance, VarianceFNs fn_ptr,
                ReadOnlyIntArrPointer[] mvcost, MV center_mv);
    }

    public RefiningSearchIF refining_search_sad;

    public interface DiamondSearchIF {
        void call(Macroblock x, Block b, BlockD d, MV ref_mv, MV best_mv, int search_param, int sad_per_bit, // int*
                VarWithNum ret, VarianceFNs fn_ptr, ReadOnlyIntArrPointer[] mvcost, MV center_mv);
    }

    public DiamondSearchIF diamond_search_sad;
    public EnumMap<BlockEnum, VarianceFNs> fn_ptr = new EnumMap<BlockEnum, VarianceFNs>(BlockEnum.class);
    public long time_receive_data;
    public long time_compress_data;
    public long time_pick_lpf;
    public long time_encode_mb_row;

    public int[] base_skip_false_prob;

    public FrameContext lfc_n; /* last frame entropy */
    public FrameContext lfc_a; /* last alt ref entropy */
    public FrameContext lfc_g; /* last gold ref entropy */

    public YV12buffer alt_ref_buffer;
    public YV12buffer[] frames = new YV12buffer[OnyxInt.MAX_LAG_BUFFERS];
    public int[] fixed_divide = new int[512];

    public boolean b_calculate_psnr;

    /* Per MB activity measurement */
    public int activity_avg; // uint
    public FullAccessIntArrPointer mb_activity_map; // uint*

    /*
     * Record of which MBs still refer to last golden frame either directly or
     * through 0,0
     */
    public FullAccessIntArrPointer gf_active_flags; // uchar*
    public int gf_active_count;

    public boolean output_partition;

    /* Store last frame's MV info for next frame MV prediction */
    public MV[] lfmv;
    public boolean[] lf_ref_frame_sign_bias;
    public MVReferenceFrame[] lf_ref_frame;

    /* force next frame to intra when kf_auto says so */
    public boolean force_next_frame_intra;

    public boolean droppable;

    int initial_width;
    int initial_height;

    /* Coding layer state variables */
    public int current_layer; // uint
    public LayerContext[] layer_context = new LayerContext[OnyxIf.VPX_TS_MAX_LAYERS];

    long[] frames_in_layer = new long[OnyxIf.VPX_TS_MAX_LAYERS];
    long[] bytes_in_layer = new long[OnyxIf.VPX_TS_MAX_LAYERS];
    double[] sum_psnr = new double[OnyxIf.VPX_TS_MAX_LAYERS];
    double[] sum_psnr_p = new double[OnyxIf.VPX_TS_MAX_LAYERS];
    double[] total_error2 = new double[OnyxIf.VPX_TS_MAX_LAYERS];
    double[] total_error2_p = new double[OnyxIf.VPX_TS_MAX_LAYERS];
    double[] sum_ssim = new double[OnyxIf.VPX_TS_MAX_LAYERS];
    double[] sum_weights = new double[OnyxIf.VPX_TS_MAX_LAYERS];

    double[] total_ssimg_y_in_layer = new double[OnyxIf.VPX_TS_MAX_LAYERS];
    double[] total_ssimg_u_in_layer = new double[OnyxIf.VPX_TS_MAX_LAYERS];
    double[] total_ssimg_v_in_layer = new double[OnyxIf.VPX_TS_MAX_LAYERS];
    double[] total_ssimg_all_in_layer = new double[OnyxIf.VPX_TS_MAX_LAYERS];

    /* The frame number of each reference frames */
    public EnumMap<MVReferenceFrame, Integer> current_ref_frames = new EnumMap<MVReferenceFrame, Integer>(
            MVReferenceFrame.class);
    // Closest reference frame to current frame.
    public MVReferenceFrame closest_reference_frame;

    public RDCosts rd_costs = new RDCosts();
    // Use the static threshold from ROI settings.
    boolean use_roi_static_threshold;

    public boolean ext_refresh_frame_flags_pending;
    public boolean repeatFrameDetected = false;

    public DefaultVarianceFNs varFns = new DefaultVarianceFNs();

    Compressor(Config oxcfNew) {

        for (CommonData.Quant quant : CommonData.Quant.values()) {
            q.put(quant, new QuantDetails());
        }

        /*
         * Frame rate is not available on the first frame, as it's derived from the
         * observed timestamps. The actual value used here doesn't matter too much, as
         * it will adapt quickly.
         */
        if (oxcfNew.timebase.num > 0) {
            framerate = oxcfNew.timebase.flip().toDouble();
        } else {
            framerate = 30;
        }

        /*
         * If the reciprocal of the timebase seems like a reasonable framerate, then use
         * that as a guess, otherwise use 30.
         */
        if (framerate > 180)
            framerate = 30;

        // start onyx_if init config
        /* change includes all joint functionality */
        vp8_change_config(oxcfNew);

        auto_gold = true;
        auto_adjust_gold_quantizer = true;

        ref_framerate = framerate;

        ref_frame_flags = EnumSet.copyOf(MVReferenceFrame.interFrames);

        /* Initialize active best and worst q and average q values. */
        active_worst_quality = oxcf.worst_allowed_q;
        active_best_quality = oxcf.best_allowed_q;
        avg_frame_qindex = oxcf.worst_allowed_q;

        /* Initialise the starting buffer levels */
        buffer_level = oxcf.starting_buffer_level;
        bits_off_target = oxcf.starting_buffer_level;

        rolling_target_bits = av_per_frame_bandwidth;
        rolling_actual_bits = av_per_frame_bandwidth;
        long_rolling_target_bits = av_per_frame_bandwidth;
        long_rolling_actual_bits = av_per_frame_bandwidth;

        total_actual_bits = 0;
        total_target_vs_actual = 0;

        /* Temporal scalabilty */
        if (oxcf.number_of_layers > 1) {
            double prev_layer_framerate = 0;

            for (int i = 0; i < oxcf.number_of_layers; ++i) {
                init_temporal_layer_context(i, prev_layer_framerate);
                prev_layer_framerate = output_framerate / oxcf.rate_decimator[i];
            }
        }

        {
            int i;

            fixed_divide[0] = 0;

            for (i = 1; i < 512; ++i)
                fixed_divide[i] = 0x80000 / i;
        }

        // end onyx_if init config

        base_skip_false_prob = Arrays.copyOf(BitStream.vp8cx_base_skip_false_prob,
                BitStream.vp8cx_base_skip_false_prob.length);

        common.current_video_frame = 0;
        temporal_pattern_counter = 0;
        temporal_layer_id = -1;
        kf_overspend_bits = 0;
        kf_bitrate_adjustment = 0;
        frames_till_gf_update_due = 0;
        gf_overspend_bits = 0;
        non_gf_bitrate_adjustment = 0;
        prob_last_coded = 128;
        prob_gf_coded = 128;
        prob_intra_coded = 63;

        /*
         * Prime the recent reference frame usage counters. Hereafter they will be
         * maintained as a sort of moving average
         */
        for (MVReferenceFrame mvrf : MVReferenceFrame.validFrames) {
            recent_ref_frame_usage.put(mvrf, 1);
        }

        /* Set reference frame sign bias for ALTREF frame to 1 (for now) */
        common.ref_frame_sign_bias.put(MVReferenceFrame.ALTREF_FRAME, true);

        baseline_gf_interval = OnyxInt.DEFAULT_GF_INTERVAL;

        gold_is_last = false;
        alt_is_last = false;
        gold_is_alt = false;

        active_map_enabled = false;

        use_roi_static_threshold = false;

        mse_source_denoised = 0;

        /*
         * Should we use the cyclic refresh method. Currently there is no external
         * control for this. Enable it for error_resilient_mode, or for 1 pass CBR mode.
         */
        cyclic_refresh_mode_enabled = (oxcf.error_resilient_mode || oxcf.end_usage == EndUsage.STREAM_FROM_SERVER);
        cyclic_refresh_mode_max_mbs_perframe = (common.mb_rows * common.mb_cols) / 7;
        if (oxcf.number_of_layers == 1) {
            cyclic_refresh_mode_max_mbs_perframe = (common.mb_rows * common.mb_cols) / 20;
        } else if (oxcf.number_of_layers == 2) {
            cyclic_refresh_mode_max_mbs_perframe = (common.mb_rows * common.mb_cols) / 10;
        }
        cyclic_refresh_mode_index = 0;
        cyclic_refresh_q = 32;

        // GF behavior for 1 pass CBR, used when error_resilience is off.
        gf_update_onepass_cbr = false;
        gf_noboost_onepass_cbr = false;
        if (!oxcf.error_resilient_mode && oxcf.end_usage == EndUsage.STREAM_FROM_SERVER) {
            gf_update_onepass_cbr = true;
            gf_noboost_onepass_cbr = true;
            gf_interval_onepass_cbr = cyclic_refresh_mode_max_mbs_perframe > 0
                    ? (2 * (common.mb_rows * common.mb_cols) / cyclic_refresh_mode_max_mbs_perframe)
                    : 10;
            gf_interval_onepass_cbr = Math.min(40, Math.max(6, gf_interval_onepass_cbr));
            baseline_gf_interval = gf_interval_onepass_cbr;
        }

        if (cyclic_refresh_mode_enabled) {

            cyclic_refresh_map = new byte[common.mb_rows * common.mb_cols];
        } else {
            cyclic_refresh_map = null;
        }

        skin_map = new boolean[common.mb_rows * common.mb_cols];

        consec_zero_last = new int[common.mb_rows * common.mb_cols];
        consec_zero_last_mvbias = new int[common.mb_rows * common.mb_cols];

        /* Initialize the feed-forward activity masking. */
        activity_avg = 90 << 12;

        /* Give a sensible default for the first frame. */
        frames_since_key = 8;
        key_frame_frequency = oxcf.key_freq;
        this_key_frame_forced = false;
        next_key_frame_forced = 0;

        source_alt_ref_pending = false;
        source_alt_ref_active = false;
        common.refresh_alt_ref_frame = false;

        force_maxqp = 0;
        frames_since_last_drop_overshoot = 0;

        b_calculate_psnr = false;

        first_time_stamp_ever = 0x7FFFFFFF;

        frames_till_gf_update_due = 0;
        key_frame_count = 1;

        ni_av_qi = oxcf.worst_allowed_q;
        ni_tot_qi = 0;
        ni_frames = 0;
        total_byte_count = 0;

        drop_frame = false;

        rate_correction_factor = 1.0;
        key_frame_rate_correction_factor = 1.0;
        gf_rate_correction_factor = 1.0;

        for (int i = 0; i < prior_key_frame_distance.length; ++i) {
            prior_key_frame_distance[i] = (int) output_framerate;
        }

        output_pkt_list = oxcf.output_pkt_list;

        if (compressor_speed == 2) {
            avg_encode_time = 0;
            avg_pick_mode_time = 0;
        }

        for (BlockEnum be : BlockEnum.values()) {
            fn_ptr.put(be, varFns.default_fn_ptr.get(be).copy());
        }

        full_search_sad = new FullSearchIF() {
            @Override
            public long call(Macroblock x, Block b, BlockD d, MV ref_mv, int sad_per_bit, int distance,
                    VarianceFNs fn_ptr, ReadOnlyIntArrPointer[] mvcost, MV center_mv) {
                return MComp.vp8_full_search_sad(x, b, d, ref_mv, sad_per_bit, distance, fn_ptr, mvcost, center_mv);
            }
        };
        diamond_search_sad = new DiamondSearchIF() {
            @Override
            public void call(Macroblock x, Block b, BlockD d, MV ref_mv, MV best_mv, int search_param, int sad_per_bit,
                    VarWithNum ret, VarianceFNs fn_ptr, ReadOnlyIntArrPointer[] mvcost, MV center_mv) {
                MComp.vp8_diamond_search_sad(x, b, d, ref_mv, best_mv, search_param, sad_per_bit, ret, fn_ptr, mvcost,
                        center_mv);
            }
        };
        refining_search_sad = new RefiningSearchIF() {
            @Override
            public long call(Macroblock x, Block b, BlockD d, MV ref_mv, int sad_per_bit, int distance,
                    VarianceFNs fn_ptr, ReadOnlyIntArrPointer[] mvcost, MV center_mv) {
                // TODO Auto-generated method stub
                return MComp.vp8_refining_search_sad(x, b, d, ref_mv, sad_per_bit, distance, fn_ptr, mvcost, center_mv);
            }
        };

        for (int i = 0; i < bc.length; i++) {
            bc[i] = new BoolEncoder();
        }

        /*
         * vp8cx_init_quantizer() is first called here. Add check in
         * vp8cx_frame_init_quantizer() so that vp8cx_init_quantizer is only called
         * later when needed. This will avoid unnecessary calls of
         * vp8cx_init_quantizer() for every frame.
         */
        Quantize.vp8cx_init_quantizer(this);
        mb = new Macroblock(this);
        OnyxIf.vp8_set_speed_features(this);
        common.setVersion((byte) oxcf.Version);

    }

    public void vp8_convert_rfct_to_prob() {
        final ReferenceCounts rf = mb.sumReferenceCounts();
        final EnumMap<MVReferenceFrame, Integer> rfct = mb.count_mb_ref_frame_usage;
        /* Calculate the probabilities used to code the ref frame based on usage */
        if ((prob_intra_coded = rf.intra * 255 / (rf.total)) == 0) {
            prob_intra_coded = 1;
        }

        prob_last_coded = rf.inter != 0 ? (rfct.get(MVReferenceFrame.LAST_FRAME) * 255) / rf.inter : 128;

        if (prob_last_coded == 0)
            prob_last_coded = 1;

        prob_gf_coded = ((rfct.get(MVReferenceFrame.GOLDEN_FRAME) + rfct.get(MVReferenceFrame.ALTREF_FRAME))) != 0
                ? (rfct.get(MVReferenceFrame.GOLDEN_FRAME) * 255)
                        / (rfct.get(MVReferenceFrame.GOLDEN_FRAME) + rfct.get(MVReferenceFrame.ALTREF_FRAME))
                : 128;

        if (prob_gf_coded == 0)
            prob_gf_coded = 1;
    }

    public void init_temporal_layer_context(int layer, double prev_layer_framerate) {
        layer_context[layer] = new LayerContext(this, oxcf, layer, prev_layer_framerate);
    }

    public void vp8_change_config(Config oxcfNew) {
        CommonData cm = common;
        int last_w, last_h;
        int prev_number_of_layers;
        int raw_target_rate;

        if (cm.getVersion() != oxcfNew.Version) {
            cm.setVersion((byte) oxcfNew.Version);
        }

        Config tempCfg = oxcf == null ? oxcfNew /* during init */ : oxcf /* real change */;
        last_w = tempCfg.Width;
        last_h = tempCfg.Height;
        prev_number_of_layers = tempCfg.number_of_layers;

        oxcf = oxcfNew.copy();

        switch (oxcf.Mode) {
        case REALTIME:
            compressor_speed = 2;

            break;

        case GOODQUALITY:
            compressor_speed = 1;

            break;

        case BESTQUALITY:
            compressor_speed = 0;
            break;
        }

        auto_worst_q = true;

        oxcf.worst_allowed_q = OnyxIf.q_trans[oxcfNew.worst_allowed_q];
        oxcf.best_allowed_q = OnyxIf.q_trans[oxcfNew.best_allowed_q];
        oxcf.cq_level = OnyxIf.q_trans[oxcf.cq_level];

        if (oxcfNew.fixed_q >= 0) {
            if (oxcfNew.worst_allowed_q < 0) {
                oxcf.fixed_q = OnyxIf.q_trans[0];
            } else {
                oxcf.fixed_q = OnyxIf.q_trans[oxcfNew.worst_allowed_q];
            }

            if (oxcfNew.alt_q < 0) {
                oxcf.alt_q = OnyxIf.q_trans[0];
            } else {
                oxcf.alt_q = OnyxIf.q_trans[oxcfNew.alt_q];
            }

            if (oxcfNew.key_q < 0) {
                oxcf.key_q = OnyxIf.q_trans[0];
            } else {
                oxcf.key_q = OnyxIf.q_trans[oxcfNew.key_q];
            }

            if (oxcfNew.gold_q < 0) {
                oxcf.gold_q = OnyxIf.q_trans[0];
            } else {
                oxcf.gold_q = OnyxIf.q_trans[oxcfNew.gold_q];
            }
        }

        ext_refresh_frame_flags_pending = false;

        baseline_gf_interval = oxcf.alt_freq != 0 ? oxcf.alt_freq : OnyxInt.DEFAULT_GF_INTERVAL;

        // GF behavior for 1 pass CBR, used when error_resilience is off.
        if (!oxcf.error_resilient_mode && oxcf.end_usage == EndUsage.STREAM_FROM_SERVER
                && oxcf.Mode == CompressMode.REALTIME)
            baseline_gf_interval = gf_interval_onepass_cbr;

        oxcf.token_partitions = TokenPartition.EIGHT_PARTITION;

        cm.multi_token_partition = oxcf.token_partitions;

        if (!use_roi_static_threshold) {
            int i;
            for (i = 0; i < BlockD.MAX_MB_SEGMENTS; ++i) {
                segment_encode_breakout[i] = oxcf.encode_breakout;
            }
        }

        /* At the moment the first order values may not be > MAXQ */
        if (oxcf.fixed_q > OnyxInt.MAXQ)
            oxcf.fixed_q = OnyxInt.MAXQ;

        /* local file playback mode == really big buffer */
        if (oxcf.end_usage == EndUsage.LOCAL_FILE_PLAYBACK) {
            oxcf.starting_buffer_level = 60000;
            oxcf.optimal_buffer_level = 60000;
            oxcf.maximum_buffer_size = 240000;
            oxcf.starting_buffer_level_in_ms = 60000;
            oxcf.optimal_buffer_level_in_ms = 60000;
            oxcf.maximum_buffer_size_in_ms = 240000;
        }

        raw_target_rate = (int) ((long) oxcf.Width * oxcf.Height * 8 * 3 * framerate / 1000);
        if (oxcf.target_bandwidth > raw_target_rate)
            oxcf.target_bandwidth = raw_target_rate;
        /* Convert target bandwidth from Kbit/s to Bit/s */
        oxcf.target_bandwidth *= 1000;
        oxcf.starting_buffer_level = OnyxIf.rescale((int) oxcf.starting_buffer_level, oxcf.target_bandwidth, 1000);

        /* Set or reset optimal and maximum buffer levels. */
        if (oxcf.optimal_buffer_level == 0) {
            oxcf.optimal_buffer_level = oxcf.target_bandwidth / 8;
        } else {
            oxcf.optimal_buffer_level = OnyxIf.rescale((int) oxcf.optimal_buffer_level, oxcf.target_bandwidth, 1000);
        }

        if (oxcf.maximum_buffer_size == 0) {
            oxcf.maximum_buffer_size = oxcf.target_bandwidth / 8;
        } else {
            oxcf.maximum_buffer_size = OnyxIf.rescale((int) oxcf.maximum_buffer_size, oxcf.target_bandwidth, 1000);
        }
        // Under a configuration change, where maximum_buffer_size may change,
        // keep buffer level clipped to the maximum allowed buffer size.
        if (bits_off_target > oxcf.maximum_buffer_size) {
            bits_off_target = oxcf.maximum_buffer_size;
            buffer_level = bits_off_target;
        }

        /* Set up frame rate and related parameters rate control values. */
        vp8_new_framerate(framerate);

        /* Set absolute upper and lower quality limits */
        worst_quality = oxcf.worst_allowed_q;
        best_quality = oxcf.best_allowed_q;

        /* active values should only be modified if out of new range */
        if (active_worst_quality > oxcf.worst_allowed_q) {
            active_worst_quality = oxcf.worst_allowed_q;
        }
        /* less likely */
        else if (active_worst_quality < oxcf.best_allowed_q) {
            active_worst_quality = oxcf.best_allowed_q;
        }
        if (active_best_quality < oxcf.best_allowed_q) {
            active_best_quality = oxcf.best_allowed_q;
        }
        /* less likely */
        else if (active_best_quality > oxcf.worst_allowed_q) {
            active_best_quality = oxcf.worst_allowed_q;
        }

        buffered_mode = oxcf.optimal_buffer_level > 0;

        cq_target_quality = oxcf.cq_level;

        /* Only allow dropped frames in buffered mode */
        drop_frames_allowed = oxcf.allow_df && buffered_mode;

        target_bandwidth = oxcf.target_bandwidth;

        // Check if the number of temporal layers has changed, and if so reset the
        // pattern counter and set/initialize the temporal layer context for the
        // new layer configuration.
        if (oxcf.number_of_layers != prev_number_of_layers) {
            // If the number of temporal layers are changed we must start at the
            // base of the pattern cycle, so set the layer id to 0 and reset
            // the temporal pattern counter.
            if (temporal_layer_id > 0) {
                temporal_layer_id = 0;
            }
            temporal_pattern_counter = 0;
            OnyxIf.reset_temporal_layer_change(this, oxcfNew, prev_number_of_layers);
        }

        if (initial_width == 0) {
            initial_width = oxcf.Width;
            initial_height = oxcf.Height;
        }

        cm.Width = oxcf.Width;
        cm.Height = oxcf.Height;
        assert (cm.Width <= initial_width);
        assert (cm.Height <= initial_height);

        /*
         * TODO(jkoleszar): if an internal spatial resampling is active, and we downsize
         * the input image, maybe we should clear the internal scale immediately rather
         * than waiting for it to correct.
         */

        /* VP8 sharpness level mapping 0-7 (vs 0-10 in general VPx dialogs) */
        if (oxcf.Sharpness > 7)
            oxcf.Sharpness = 7;

        cm.sharpness_level = oxcf.Sharpness;

        if (cm.horiz_scale != Scaling.NORMAL || cm.vert_scale != Scaling.NORMAL) {
            final int hr = cm.horiz_scale.hr;
            final int hs = cm.horiz_scale.hs;
            final int vr = cm.vert_scale.hr;
            final int vs = cm.vert_scale.hs;

            /* always go to the next whole number */
            cm.Width = (hs - 1 + oxcf.Width * hr) / hs;
            cm.Height = (vs - 1 + oxcf.Height * vr) / vs;
        }

        if (last_w != oxcf.Width || last_h != oxcf.Height) {
            force_next_frame_intra = true;
        }

        if (cm.yv12_fb[0] == null
                || ((cm.Width + 15) & ~15) != cm.yv12_fb[cm.frameIdxs.get(MVReferenceFrame.LAST_FRAME)].y_width
                || ((cm.Height + 15) & ~15) != cm.yv12_fb[cm.frameIdxs.get(MVReferenceFrame.LAST_FRAME)].y_height
                || cm.yv12_fb[cm.frameIdxs.get(MVReferenceFrame.LAST_FRAME)].y_width == 0) {
            OnyxIf.dealloc_raw_frame_buffers(this);
            OnyxIf.alloc_raw_frame_buffers(this);
            OnyxIf.vp8_alloc_compressor_data(this);
        }

        if (oxcf.fixed_q >= 0) {
            last_q[0] = oxcf.fixed_q;
            last_q[1] = oxcf.fixed_q;
        }

        Speed = oxcf.getCpu_used();

        /* force to allowlag to 0 if lag_in_frames is 0; */
        if (oxcf.lag_in_frames == 0) {
            oxcf.allow_lag = 0;
        }
        /* Limit on lag buffers as these are not currently dynamically allocated */
        else if (oxcf.lag_in_frames > OnyxInt.MAX_LAG_BUFFERS) {
            oxcf.lag_in_frames = OnyxInt.MAX_LAG_BUFFERS;
        }

        /* YX Temp */
        alt_ref_source = null;
        is_src_frame_alt_ref = false;
    }

    public void vp8_new_framerate(double framerate) {
        if (framerate < .1)
            framerate = 30;

        this.framerate = framerate;
        output_framerate = framerate;
        per_frame_bandwidth = (int) (oxcf.target_bandwidth / output_framerate);
        av_per_frame_bandwidth = per_frame_bandwidth;
        min_frame_bandwidth = (int) (av_per_frame_bandwidth / 100);

        /* Set Maximum gf/arf interval */
        max_gf_interval = ((int) (output_framerate / 2.0) + 2);

        if (max_gf_interval < 12)
            max_gf_interval = 12;

        /* Special conditions when altr ref frame enabled in lagged compress mode */
        if (oxcf.play_alternate && oxcf.lag_in_frames != 0) {
            if (max_gf_interval > oxcf.lag_in_frames - 1) {
                max_gf_interval = oxcf.lag_in_frames - 1;
            }
        }
    }

}