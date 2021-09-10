package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.VP8Exception;
import org.jcodec.common.model.Rational;

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
public class CodecEncCfg {
    public static final int VPX_MAX_LAYERS = 12; // 3 temporal + 4 spatial layers are allowed.
    public static final int VPX_TS_MAX_PERIODICITY = 16;
    public static final int VPX_TS_MAX_LAYERS = 5;
    /* ! Spatial Scalability: Maximum number of coding layers */
    public static final int VPX_SS_MAX_LAYERS = 5;

    /*
     * generic settings (g)
     */

    /*
     * !\brief Deprecated: Algorithm specific "usage" value
     *
     * This value must be zero.
     */
    private int g_usage = 0;

    /*
     * !\brief Bitstream profile to use
     *
     * Some codecs support a notion of multiple bitstream profiles. Typically this
     * maps to a set of features that are turned on or off. Often the profile to use
     * is determined by the features of the intended decoder. Consult the
     * documentation for the codec to determine the valid values for this parameter,
     * or set to zero for a sane default.
     */
    private int g_profile = 0;
    /** < profile of bitstream to use */

    /*
     * !\brief Width of the frame
     *
     * This value identifies the presentation resolution of the frame, in pixels.
     * Note that the frames passed as input to the encoder must have this
     * resolution. Frames will be presented by the decoder in this resolution,
     * independent of any spatial resampling the encoder may do.
     */
    private int g_w;

    /*
     * !\brief Height of the frame
     *
     * This value identifies the presentation resolution of the frame, in pixels.
     * Note that the frames passed as input to the encoder must have this
     * resolution. Frames will be presented by the decoder in this resolution,
     * independent of any spatial resampling the encoder may do.
     */
    private int g_h;

    public static enum vpx_bit_depth {
        VPX_BITS_8(8),
        /** < 8 bits */
        VPX_BITS_10(10),
        /** < 10 bits */
        VPX_BITS_12(12);

        /** < 12 bits */
        final int bits;

        private vpx_bit_depth(int bitcount) {
            bits = bitcount;
        }
    }

    /*
     * !\brief Bit-depth of the codec
     *
     * This value identifies the bit_depth of the codec, Only certain bit-depths are
     * supported as identified in the vpx_bit_depth_t enum.
     */
    private vpx_bit_depth g_bit_depth = vpx_bit_depth.VPX_BITS_8;

    /*
     * !\brief Bit-depth of the input frames
     *
     * This value identifies the bit_depth of the input frames in bits. Note that
     * the frames passed as input to the encoder must have this bit-depth.
     */
    private int g_input_bit_depth = 8;

    /*
     * !\brief Stream timebase units
     *
     * Indicates the smallest interval of time, in seconds, used by the stream. For
     * fixed frame rate material, or variable frame rate material where frames are
     * timed at a multiple of a given clock (ex: video capture), the \ref
     * RECOMMENDED method is to set the timebase to the reciprocal of the frame rate
     * (ex: 1001/30000 for 29.970 Hz NTSC). This allows the pts to correspond to the
     * frame number, which can be handy. For re-encoding video from containers with
     * absolute time timestamps, the \ref RECOMMENDED method is to set the timebase
     * to that of the parent container or multimedia framework (ex: 1/1000 for ms,
     * as in FLV).
     */
    private Rational g_timebase = Rational.R(1, 25);

    /*
     * !\brief Enable error resilient modes.
     *
     * The error resilient bitfield indicates to the encoder which features it
     * should enable to take measures for streaming over lossy or noisy links.
     */
    private boolean g_error_resilient = false;

    /*
     * !\brief Allow lagged encoding
     *
     * If set, this value allows the encoder to consume a number of input frames
     * before producing output frames. This allows the encoder to base decisions for
     * the current frame on future frames. This does increase the latency of the
     * encoding pipeline, so it is not appropriate in all situations (ex: realtime
     * encoding).
     *
     * Note that this is a maximum value -- the encoder may produce frames sooner
     * than the given limit. Set this value to 0 to disable this feature.
     */
    private int g_lag_in_frames = 0;

    /*
     * rate control settings (rc)
     */

    /*
     * !\brief Temporal resampling configuration, if supported by the codec.
     *
     * Temporal resampling allows the codec to "drop" frames as a strategy to meet
     * its target data rate. This can cause temporal discontinuities in the encoded
     * video, which may appear as stuttering during playback. This trade-off is
     * often acceptable, but for many applications is not. It can be disabled in
     * these cases.
     *
     * This threshold is described as a percentage of the target data buffer. When
     * the data buffer falls below this percentage of fullness, a dropped frame is
     * indicated. Set the threshold to zero (0) to disable this feature.
     */
    private int rc_dropframe_thresh = 0;

    /*
     * !\brief Enable/disable spatial resampling, if supported by the codec.
     *
     * Spatial resampling allows the codec to compress a lower resolution version of
     * the frame, which is then upscaled by the encoder to the correct presentation
     * resolution. This increases visual quality at low data rates, at the expense
     * of CPU time on the encoder/decoder.
     */
    private boolean rc_resize_allowed = false;

    /*
     * !\brief Internal coded frame width.
     *
     * If spatial resampling is enabled this specifies the width of the encoded
     * frame.
     */
    private int rc_scaled_width = 1;

    /*
     * !\brief Internal coded frame height.
     *
     * If spatial resampling is enabled this specifies the height of the encoded
     * frame.
     */
    private int rc_scaled_height = 1;

    /*
     * !\brief Spatial resampling up watermark.
     *
     * This threshold is described as a percentage of the target data buffer. When
     * the data buffer rises above this percentage of fullness, the encoder will
     * step up to a higher resolution version of the frame.
     */
    private int rc_resize_up_thresh = 60;

    /*
     * !\brief Spatial resampling down watermark.
     *
     * This threshold is described as a percentage of the target data buffer. When
     * the data buffer falls below this percentage of fullness, the encoder will
     * step down to a lower resolution version of the frame.
     */
    private int rc_resize_down_thresh = 30;

    public static enum vpx_rc_mode {
        VPX_VBR,
        /** < Variable Bit Rate (VBR) mode */
        VPX_CBR,
        /** < Constant Bit Rate (CBR) mode */
        VPX_CQ,
        /** < Constrained Quality (CQ) mode */
        VPX_Q,/** < Constant Quality (Q) mode */
    }

    /*
     * !\brief Rate control algorithm to use.
     *
     * Indicates whether the end usage of this stream is to be streamed over a
     * bandwidth constrained link, indicating that Constant Bit Rate (CBR) mode
     * should be used, or whether it will be played back on a high bandwidth link,
     * as from a local disk, where higher variations in bitrate are acceptable.
     */
    private vpx_rc_mode rc_end_usage = vpx_rc_mode.VPX_VBR;

    /*
     * !\brief Target data rate
     *
     * Target bitrate to use for this stream, in kilobits per second.
     */
    private int rc_target_bitrate = 256;

    /*
     * quantizer settings
     */

    /*
     * !\brief Minimum (Best Quality) Quantizer
     *
     * The quantizer is the most direct control over the quality of the encoded
     * image. The range of valid values for the quantizer is codec specific. Consult
     * the documentation for the codec to determine the values to use.
     */
    private short rc_min_quantizer = 4;

    /*
     * !\brief Maximum (Worst Quality) Quantizer
     *
     * The quantizer is the most direct control over the quality of the encoded
     * image. The range of valid values for the quantizer is codec specific. Consult
     * the documentation for the codec to determine the values to use.
     */
    private short rc_max_quantizer = 63;

    /*
     * bitrate tolerance
     */

    /*
     * !\brief Rate control adaptation undershoot control
     *
     * VP8: Expressed as a percentage of the target bitrate, controls the maximum
     * allowed adaptation speed of the codec. This factor controls the maximum
     * amount of bits that can be subtracted from the target bitrate in order to
     * compensate for prior overshoot.
     * 
     * Valid values in the range 0-100.
     */
    private int rc_undershoot_pct = 100;

    /*
     * !\brief Rate control adaptation overshoot control
     *
     * VP8: Expressed as a percentage of the target bitrate, controls the maximum
     * allowed adaptation speed of the codec. This factor controls the maximum
     * amount of bits that can be added to the target bitrate in order to compensate
     * for prior undershoot.
     *
     * Valid values in the range 0-100.
     */
    private int rc_overshoot_pct = 100;

    /*
     * decoder buffer model parameters
     */

    /*
     * !\brief Decoder Buffer Size
     *
     * This value indicates the amount of data that may be buffered by the decoding
     * application. Note that this value is expressed in units of time
     * (milliseconds). For example, a value of 5000 indicates that the client will
     * buffer (at least) 5000ms worth of encoded data. Use the target bitrate
     * (#rc_target_bitrate) to convert to bits/bytes, if necessary.
     */
    private int rc_buf_sz = 6000;

    /*
     * !\brief Decoder Buffer Initial Size
     *
     * This value indicates the amount of data that will be buffered by the decoding
     * application prior to beginning playback. This value is expressed in units of
     * time (milliseconds). Use the target bitrate (#rc_target_bitrate) to convert
     * to bits/bytes, if necessary.
     */
    private int rc_buf_initial_sz = 4000;

    /*
     * !\brief Decoder Buffer Optimal Size
     *
     * This value indicates the amount of data that the encoder should try to
     * maintain in the decoder's buffer. This value is expressed in units of time
     * (milliseconds). Use the target bitrate (#rc_target_bitrate) to convert to
     * bits/bytes, if necessary.
     */
    private int rc_buf_optimal_sz = 5000;

    /*
     * keyframing settings (kf)
     */

    public static enum vpx_kf_mode {
        VPX_KF_DISABLED,
        /** < Encoder does not place keyframes. */
        VPX_KF_AUTO,/** < Encoder determines optimal placement automatically */
    }

    /*
     * !\brief Keyframe placement mode
     *
     * This value indicates whether the encoder should place keyframes at a fixed
     * interval, or determine the optimal placement automatically (as governed by
     * the #kf_min_dist and #kf_max_dist parameters)
     */
    private vpx_kf_mode kf_mode = vpx_kf_mode.VPX_KF_AUTO;

    /*
     * !\brief Keyframe minimum interval
     *
     * This value, expressed as a number of frames, prevents the encoder from
     * placing a keyframe nearer than kf_min_dist to the previous keyframe. At least
     * kf_min_dist frames non-keyframes will be coded before the next keyframe. Set
     * kf_min_dist equal to kf_max_dist for a fixed interval.
     */
    private int kf_min_dist = 0;

    /*
     * !\brief Keyframe maximum interval
     *
     * This value, expressed as a number of frames, forces the encoder to code a
     * keyframe if one has not been coded in the last kf_max_dist frames. A value of
     * 0 implies all frames will be keyframes. Set kf_min_dist equal to kf_max_dist
     * for a fixed interval.
     */
    private int kf_max_dist = 128;

    /*
     * Spatial scalability settings (ss)
     */

    /*
     * !\brief Number of spatial coding layers.
     *
     * This value specifies the number of spatial coding layers to be used.
     */
    private int ss_number_layer = CodecEncCfg.VPX_SS_DEFAULT_LAYERS;

    /*
     * !\brief Enable auto alt reference flags for each spatial layer.
     *
     * These values specify if auto alt reference frame is enabled for each spatial
     * layer.
     */
    private int[] ss_enable_auto_alt_ref = { 0 };

    /*
     * !\brief Target bitrate for each spatial layer.
     *
     * These values specify the target coding bitrate to be used for each spatial
     * layer. (in kbps)
     */
    private int[] ss_target_bitrate = { 0 };

    /*
     * !\brief Number of temporal coding layers.
     *
     * This value specifies the number of temporal layers to be used.
     */
    private int ts_number_layers = 1;

    /*
     * !\brief Target bitrate for each temporal layer.
     *
     * These values specify the target coding bitrate to be used for each temporal
     * layer. (in kbps)
     */
    private int[] ts_target_bitrate = { 0 };

    /*
     * !\brief Frame rate decimation factor for each temporal layer.
     *
     * These values specify the frame rate decimation factors to apply to each
     * temporal layer.
     */
    private int[] ts_rate_decimator = { 0 };

    /*
     * !\brief Length of the sequence defining frame temporal layer membership.
     *
     * This value specifies the length of the sequence that defines the membership
     * of frames to temporal layers. For example, if the ts_periodicity = 8, then
     * the frames are assigned to coding layers with a repeated sequence of length
     * 8.
     */
    private int ts_periodicity = 0;

    /*
     * !\brief Template defining the membership of frames to temporal layers.
     *
     * This array defines the membership of frames to temporal coding layers. For a
     * 2-layer encoding that assigns even numbered frames to one temporal layer (0)
     * and odd numbered frames to a second temporal layer (1) with ts_periodicity=8,
     * then ts_layer_id = (0,1,0,1,0,1,0,1).
     */
    private int[] ts_layer_id = { 0 };

    /*
     * !\brief Target bitrate for each spatial/temporal layer.
     *
     * These values specify the target coding bitrate to be used for each
     * spatial/temporal layer. (in kbps)
     *
     */
    private int[] layer_target_bitrate = { 0 };

    /*
     * !\brief Temporal layering mode indicating which temporal layering scheme to
     * use.
     *
     * The value (refer to VP9E_TEMPORAL_LAYERING_MODE) specifies the temporal
     * layering mode to use.
     *
     */
    private int temporal_layering_mode = 0;

    /*
     * !\brief A flag indicating whether to use external rate control parameters. By
     * default is 0. If set to 1, the following parameters will be used in the rate
     * control system.
     */
    private boolean use_vizier_rc_params = false;

    /*
     * !\brief Active worst quality factor.
     *
     * Rate control parameters, set from external experiment results. Only when
     * |use_vizier_rc_params| is set to 1, the pass in value will be used.
     * Otherwise, the default value is used.
     *
     */
    private Rational active_wq_factor = Rational.ONE;

    /*
     * !\brief Error per macroblock adjustment factor.
     *
     * Rate control parameters, set from external experiment results. Only when
     * |use_vizier_rc_params| is set to 1, the pass in value will be used.
     * Otherwise, the default value is used.
     *
     */
    private Rational err_per_mb_factor = Rational.ONE;

    /*
     * !\brief Second reference default decay limit.
     *
     * Rate control parameters, set from external experiment results. Only when
     * |use_vizier_rc_params| is set to 1, the pass in value will be used.
     * Otherwise, the default value is used.
     *
     */
    private Rational sr_default_decay_limit = Rational.ONE;

    /*
     * !\brief Second reference difference factor.
     *
     * Rate control parameters, set from external experiment results. Only when
     * |use_vizier_rc_params| is set to 1, the pass in value will be used.
     * Otherwise, the default value is used.
     *
     */
    private Rational sr_diff_factor = Rational.ONE;

    /*
     * !\brief Keyframe error per macroblock adjustment factor.
     *
     * Rate control parameters, set from external experiment results. Only when
     * |use_vizier_rc_params| is set to 1, the pass in value will be used.
     * Otherwise, the default value is used.
     *
     */
    private Rational kf_err_per_mb_factor = Rational.ONE;

    /*
     * !\brief Keyframe minimum boost adjustment factor.
     *
     * Rate control parameters, set from external experiment results. Only when
     * |use_vizier_rc_params| is set to 1, the pass in value will be used.
     * Otherwise, the default value is used.
     *
     */
    private Rational kf_frame_min_boost_factor = Rational.ONE;

    /*
     * !\brief Keyframe maximum boost adjustment factor, for the first keyframe in a
     * chunk.
     *
     * Rate control parameters, set from external experiment results. Only when
     * |use_vizier_rc_params| is set to 1, the pass in value will be used.
     * Otherwise, the default value is used.
     *
     */
    private Rational kf_frame_max_boost_first_factor = Rational.ONE;

    /*
     * !\brief Keyframe maximum boost adjustment factor, for subsequent keyframes.
     *
     * Rate control parameters, set from external experiment results. Only when
     * |use_vizier_rc_params| is set to 1, the pass in value will be used.
     * Otherwise, the default value is used.
     *
     */
    private Rational kf_frame_max_boost_subs_factor = Rational.ONE;

    /*
     * !\brief Keyframe maximum total boost adjustment factor.
     *
     * Rate control parameters, set from external experiment results. Only when
     * |use_vizier_rc_params| is set to 1, the pass in value will be used.
     * Otherwise, the default value is used.
     *
     */
    private Rational kf_max_total_boost_factor = Rational.ONE;

    /*
     * !\brief Golden frame maximum total boost adjustment factor.
     *
     * Rate control parameters, set from external experiment results. Only when
     * |use_vizier_rc_params| is set to 1, the pass in value will be used.
     * Otherwise, the default value is used.
     *
     */
    private Rational gf_max_total_boost_factor = Rational.ONE;

    /*
     * !\brief Golden frame maximum boost adjustment factor.
     *
     * Rate control parameters, set from external experiment results. Only when
     * |use_vizier_rc_params| is set to 1, the pass in value will be used.
     * Otherwise, the default value is used.
     *
     */
    private Rational gf_frame_max_boost_factor = Rational.ONE;

    /*
     * !\brief Zero motion power factor.
     *
     * Rate control parameters, set from external experiment results. Only when
     * |use_vizier_rc_params| is set to 1, the pass in value will be used.
     * Otherwise, the default value is used.
     *
     */
    private Rational zm_factor = Rational.ONE;

    /*
     * !\brief Rate-distortion multiplier for inter frames. The multiplier is a
     * crucial parameter in the calculation of rate distortion cost. It is often
     * related to the qp (qindex) value. Rate control parameters, could be set from
     * external experiment results. Only when |use_vizier_rc_params| is set to 1,
     * the pass in value will be used. Otherwise, the default value is used.
     *
     */
    private Rational rd_mult_inter_qp_fac = Rational.ONE;

    /*
     * !\brief Rate-distortion multiplier for alt-ref frames. The multiplier is a
     * crucial parameter in the calculation of rate distortion cost. It is often
     * related to the qp (qindex) value. Rate control parameters, could be set from
     * external experiment results. Only when |use_vizier_rc_params| is set to 1,
     * the pass in value will be used. Otherwise, the default value is used.
     *
     */
    private Rational rd_mult_arf_qp_fac = Rational.ONE;

    /*
     * !\brief Rate-distortion multiplier for key frames. The multiplier is a
     * crucial parameter in the calculation of rate distortion cost. It is often
     * related to the qp (qindex) value. Rate control parameters, could be set from
     * external experiment results. Only when |use_vizier_rc_params| is set to 1,
     * the pass in value will be used. Otherwise, the default value is used.
     *
     */
    private Rational rd_mult_key_qp_fac = Rational.ONE;
    public static final int VPX_SS_DEFAULT_LAYERS = 1;

    static void rangeCheck(int v, int min, int max) {
        if (v < min || v > max)
            VP8Exception.ERROR(Thread.currentThread().getStackTrace()[1].getMethodName() + " is out of range");
    }

    public int getG_usage() {
        return g_usage;
    }

    public void setG_usage(int g_usage) {
        this.g_usage = g_usage;
    }

    public int getG_profile() {
        return g_profile;
    }

    public void setG_profile(int g_profile) {
        rangeCheck(g_profile, 0, 3);
        this.g_profile = g_profile;
    }

    public int getG_w() {
        return g_w;
    }

    public void setG_w(int g_w) {
        rangeCheck(g_w, 1, 16383);
        this.g_w = g_w;
    }

    public int getG_h() {
        return g_h;
    }

    public void setG_h(int g_h) {
        rangeCheck(g_h, 1, 16383);
        this.g_h = g_h;
    }

    public vpx_bit_depth getG_bit_depth() {
        return g_bit_depth;
    }

    public void setG_bit_depth(vpx_bit_depth g_bit_depth) {
        this.g_bit_depth = g_bit_depth;
    }

    public int getG_input_bit_depth() {
        return g_input_bit_depth;
    }

    public void setG_input_bit_depth(int g_input_bit_depth) {
        this.g_input_bit_depth = g_input_bit_depth;
    }

    public Rational getG_timebase() {
        return g_timebase;
    }

    public void setG_timebase(Rational g_timebase) {
        rangeCheck(g_timebase.getDen(), 1, 1000000000);
        rangeCheck(g_timebase.getNum(), 1, 1000000000);
        this.g_timebase = g_timebase;
    }

    public boolean isG_error_resilient() {
        return g_error_resilient;
    }

    public void setG_error_resilient(boolean g_error_resilient) {
        this.g_error_resilient = g_error_resilient;
    }

    public int getG_lag_in_frames() {
        return g_lag_in_frames;
    }

    public void setG_lag_in_frames(int g_lag_in_frames) {
        rangeCheck(g_lag_in_frames, 0, 25);
        this.g_lag_in_frames = g_lag_in_frames;
    }

    public int getRc_dropframe_thresh() {
        return rc_dropframe_thresh;
    }

    public void setRc_dropframe_thresh(int rc_dropframe_thresh) {
        rangeCheck(rc_dropframe_thresh, 0, 100);
        this.rc_dropframe_thresh = rc_dropframe_thresh;
    }

    public boolean isRc_resize_allowed() {
        return rc_resize_allowed;
    }

    public void setRc_resize_allowed(boolean rc_resize_allowed) {
        this.rc_resize_allowed = rc_resize_allowed;
    }

    public int getRc_scaled_width() {
        return rc_scaled_width;
    }

    public void setRc_scaled_width(int rc_scaled_width) {
        this.rc_scaled_width = rc_scaled_width;
    }

    public int getRc_scaled_height() {
        return rc_scaled_height;
    }

    public void setRc_scaled_height(int rc_scaled_height) {
        this.rc_scaled_height = rc_scaled_height;
    }

    public int getRc_resize_up_thresh() {
        return rc_resize_up_thresh;
    }

    public void setRc_resize_up_thresh(int rc_resize_up_thresh) {
        rangeCheck(rc_resize_up_thresh, 0, 100);
        this.rc_resize_up_thresh = rc_resize_up_thresh;
    }

    public int getRc_resize_down_thresh() {
        return rc_resize_down_thresh;
    }

    public void setRc_resize_down_thresh(int rc_resize_down_thresh) {
        rangeCheck(rc_resize_down_thresh, 0, 100);
        this.rc_resize_down_thresh = rc_resize_down_thresh;
    }

    public vpx_rc_mode getRc_end_usage() {
        return rc_end_usage;
    }

    public void setRc_end_usage(vpx_rc_mode rc_end_usage) {
        this.rc_end_usage = rc_end_usage;
    }

    public int getRc_target_bitrate() {
        return rc_target_bitrate;
    }

    public void setRc_target_bitrate(int rc_target_bitrate) {
        this.rc_target_bitrate = rc_target_bitrate;
    }

    public short getRc_min_quantizer() {
        return rc_min_quantizer;
    }

    public void setRc_min_quantizer(short rc_min_quantizer) {
        rangeCheck(rc_min_quantizer, 0, rc_max_quantizer);
        this.rc_min_quantizer = rc_min_quantizer;
    }

    public short getRc_max_quantizer() {
        return rc_max_quantizer;
    }

    public void setRc_max_quantizer(short rc_max_quantizer) {
        rangeCheck(rc_max_quantizer, 0, 63);
        this.rc_max_quantizer = rc_max_quantizer;
    }

    public int getRc_undershoot_pct() {
        return rc_undershoot_pct;
    }

    public void setRc_undershoot_pct(int rc_undershoot_pct) {
        rangeCheck(rc_undershoot_pct, 0, 100);
        this.rc_undershoot_pct = rc_undershoot_pct;
    }

    public int getRc_overshoot_pct() {
        return rc_overshoot_pct;
    }

    public void setRc_overshoot_pct(int rc_overshoot_pct) {
        rangeCheck(rc_undershoot_pct, 0, 100);
        this.rc_overshoot_pct = rc_overshoot_pct;
    }

    public int getRc_buf_sz() {
        return rc_buf_sz;
    }

    public void setRc_buf_sz(int rc_buf_sz) {
        this.rc_buf_sz = rc_buf_sz;
    }

    public int getRc_buf_initial_sz() {
        return rc_buf_initial_sz;
    }

    public void setRc_buf_initial_sz(int rc_buf_initial_sz) {
        this.rc_buf_initial_sz = rc_buf_initial_sz;
    }

    public int getRc_buf_optimal_sz() {
        return rc_buf_optimal_sz;
    }

    public void setRc_buf_optimal_sz(int rc_buf_optimal_sz) {
        this.rc_buf_optimal_sz = rc_buf_optimal_sz;
    }

    public vpx_kf_mode getKf_mode() {
        return kf_mode;
    }

    public void setKf_mode(vpx_kf_mode kf_mode) {
        this.kf_mode = kf_mode;
        setKf_min_dist(kf_min_dist); // validation
    }

    public int getKf_min_dist() {
        return kf_min_dist;
    }

    public void setKf_min_dist(int kf_min_dist) {
        if (kf_mode == vpx_kf_mode.VPX_KF_AUTO && kf_min_dist != 0 && kf_min_dist != kf_max_dist) {
            VP8Exception.ERROR("kf_min_dist not supported in auto mode, use 0 or kf_max_dist instead.");
        }
        this.kf_min_dist = kf_min_dist;
    }

    public int getKf_max_dist() {
        return kf_max_dist;
    }

    public void setKf_max_dist(int kf_max_dist) {
        this.kf_max_dist = kf_max_dist;
        setKf_min_dist(kf_min_dist); // validation
    }

    public int getSs_number_layer() {
        return ss_number_layer;
    }

    public void setSs_number_layer(int ss_number_layer) {
        this.ss_number_layer = ss_number_layer;
    }

    public int[] getSs_enable_auto_alt_ref() {
        return ss_enable_auto_alt_ref;
    }

    public void setSs_enable_auto_alt_ref(int[] ss_enable_auto_alt_ref) {
        this.ss_enable_auto_alt_ref = ss_enable_auto_alt_ref;
    }

    public int[] getSs_target_bitrate() {
        return ss_target_bitrate;
    }

    public void setSs_target_bitrate(int[] ss_target_bitrate) {
        this.ss_target_bitrate = ss_target_bitrate;
    }

    public int getTs_number_layers() {
        return ts_number_layers;
    }

    public void setTs_number_layers(int ts_number_layers) {
        rangeCheck(ts_number_layers, 1, 5);
        this.ts_number_layers = ts_number_layers;
        // Validation:
        setTs_periodicity(ts_periodicity);
        setTs_rate_decimator(ts_rate_decimator);
        setTs_target_bitrate(ts_target_bitrate);
        setTs_layer_id(ts_layer_id);
    }

    public int[] getTs_target_bitrate() {
        return ts_target_bitrate;
    }

    public void setTs_target_bitrate(int[] ts_target_bitrate) {
        for (int i = 1; i < ts_number_layers; i++) {
            if (ts_target_bitrate[i] <= ts_target_bitrate[i - 1] && rc_target_bitrate > 0) {
                VP8Exception.ERROR("ts_target_bitrate entries are not strictly increasing");
            }
        }
        this.ts_target_bitrate = ts_target_bitrate;
    }

    public int[] getTs_rate_decimator() {
        return ts_rate_decimator;
    }

    public void setTs_rate_decimator(int[] ts_rate_decimator) {
        rangeCheck(ts_rate_decimator[ts_number_layers - 1], 1, 1);
        for (int i = ts_number_layers - 2; i > 0; i--) {
            if (ts_rate_decimator[i - 1] != 2 * ts_rate_decimator[i])
                VP8Exception.ERROR("ts_rate_decimator factors are not powers of 2");
        }
        this.ts_rate_decimator = ts_rate_decimator;
    }

    public int getTs_periodicity() {
        return ts_periodicity;
    }

    public void setTs_periodicity(int ts_periodicity) {
        if (ts_number_layers > 1) {
            rangeCheck(ts_periodicity, 0, 16);
        }
        this.ts_periodicity = ts_periodicity;
    }

    public int[] getTs_layer_id() {
        return ts_layer_id;
    }

    public void setTs_layer_id(int[] ts_layer_id) {
        rangeCheck(ts_layer_id[0], 0, ts_number_layers - 1);
        this.ts_layer_id = ts_layer_id;
    }

    public int[] getLayer_target_bitrate() {
        return layer_target_bitrate;
    }

    public void setLayer_target_bitrate(int[] layer_target_bitrate) {
        this.layer_target_bitrate = layer_target_bitrate;
    }

    public int getTemporal_layering_mode() {
        return temporal_layering_mode;
    }

    public void setTemporal_layering_mode(int temporal_layering_mode) {
        this.temporal_layering_mode = temporal_layering_mode;
    }

    public boolean isUse_vizier_rc_params() {
        return use_vizier_rc_params;
    }

    public void setUse_vizier_rc_params(boolean use_vizier_rc_params) {
        this.use_vizier_rc_params = use_vizier_rc_params;
    }

    public Rational getActive_wq_factor() {
        return active_wq_factor;
    }

    public void setActive_wq_factor(Rational active_wq_factor) {
        rangeCheck(active_wq_factor.getDen(), 1, 1000);
        this.active_wq_factor = active_wq_factor;
    }

    public Rational getErr_per_mb_factor() {
        return err_per_mb_factor;
    }

    public void setErr_per_mb_factor(Rational err_per_mb_factor) {
        rangeCheck(err_per_mb_factor.getDen(), 1, 1000);
        this.err_per_mb_factor = err_per_mb_factor;
    }

    public Rational getSr_default_decay_limit() {
        return sr_default_decay_limit;
    }

    public void setSr_default_decay_limit(Rational sr_default_decay_limit) {
        rangeCheck(sr_default_decay_limit.getDen(), 1, 1000);
        this.sr_default_decay_limit = sr_default_decay_limit;
    }

    public Rational getSr_diff_factor() {
        return sr_diff_factor;
    }

    public void setSr_diff_factor(Rational sr_diff_factor) {
        rangeCheck(sr_diff_factor.getDen(), 1, 1000);
        this.sr_diff_factor = sr_diff_factor;
    }

    public Rational getKf_err_per_mb_factor() {
        return kf_err_per_mb_factor;
    }

    public void setKf_err_per_mb_factor(Rational kf_err_per_mb_factor) {
        rangeCheck(kf_err_per_mb_factor.getDen(), 1, 1000);
        this.kf_err_per_mb_factor = kf_err_per_mb_factor;
    }

    public Rational getKf_frame_min_boost_factor() {
        return kf_frame_min_boost_factor;
    }

    public void setKf_frame_min_boost_factor(Rational kf_frame_min_boost_factor) {
        rangeCheck(kf_frame_min_boost_factor.getDen(), 1, 1000);
        this.kf_frame_min_boost_factor = kf_frame_min_boost_factor;
    }

    public Rational getKf_frame_max_boost_first_factor() {
        return kf_frame_max_boost_first_factor;
    }

    public void setKf_frame_max_boost_first_factor(Rational kf_frame_max_boost_first_factor) {
        rangeCheck(kf_frame_max_boost_first_factor.getDen(), 1, 1000);
        this.kf_frame_max_boost_first_factor = kf_frame_max_boost_first_factor;
    }

    public Rational getKf_frame_max_boost_subs_factor() {
        return kf_frame_max_boost_subs_factor;
    }

    public void setKf_frame_max_boost_subs_factor(Rational kf_frame_max_boost_subs_factor) {
        rangeCheck(kf_frame_max_boost_subs_factor.getDen(), 1, 1000);
        this.kf_frame_max_boost_subs_factor = kf_frame_max_boost_subs_factor;
    }

    public Rational getKf_max_total_boost_factor() {
        return kf_max_total_boost_factor;
    }

    public void setKf_max_total_boost_factor(Rational kf_max_total_boost_factor) {
        rangeCheck(kf_max_total_boost_factor.getDen(), 1, 1000);
        this.kf_max_total_boost_factor = kf_max_total_boost_factor;
    }

    public Rational getGf_max_total_boost_factor() {
        return gf_max_total_boost_factor;
    }

    public void setGf_max_total_boost_factor(Rational gf_max_total_boost_factor) {
        rangeCheck(gf_max_total_boost_factor.getDen(), 1, 1000);
        this.gf_max_total_boost_factor = gf_max_total_boost_factor;
    }

    public Rational getGf_frame_max_boost_factor() {
        return gf_frame_max_boost_factor;
    }

    public void setGf_frame_max_boost_factor(Rational gf_frame_max_boost_factor) {
        rangeCheck(gf_frame_max_boost_factor.getDen(), 1, 1000);
        this.gf_frame_max_boost_factor = gf_frame_max_boost_factor;
    }

    public Rational getZm_factor() {
        return zm_factor;
    }

    public void setZm_factor(Rational zm_factor) {
        rangeCheck(zm_factor.getDen(), 1, 1000);
        this.zm_factor = zm_factor;
    }

    public Rational getRd_mult_inter_qp_fac() {
        return rd_mult_inter_qp_fac;
    }

    public void setRd_mult_inter_qp_fac(Rational rd_mult_inter_qp_fac) {
        rangeCheck(rd_mult_inter_qp_fac.getDen(), 1, 1000);
        this.rd_mult_inter_qp_fac = rd_mult_inter_qp_fac;
    }

    public Rational getRd_mult_arf_qp_fac() {
        return rd_mult_arf_qp_fac;
    }

    public void setRd_mult_arf_qp_fac(Rational rd_mult_arf_qp_fac) {
        rangeCheck(rd_mult_arf_qp_fac.getDen(), 1, 1000);
        this.rd_mult_arf_qp_fac = rd_mult_arf_qp_fac;
    }

    public Rational getRd_mult_key_qp_fac() {
        return rd_mult_key_qp_fac;
    }

    public void setRd_mult_key_qp_fac(Rational rd_mult_key_qp_fac) {
        rangeCheck(rd_mult_key_qp_fac.getDen(), 1, 1000);
        this.rd_mult_key_qp_fac = rd_mult_key_qp_fac;
    }

}
