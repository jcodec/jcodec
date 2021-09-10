package org.jcodec.codecs.vpx.vp8.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jcodec.codecs.vpx.vp8.CommonUtils;
import org.jcodec.codecs.vpx.vp8.OnyxIf;
import org.jcodec.codecs.vpx.vp8.enums.CompressMode;
import org.jcodec.codecs.vpx.vp8.enums.EndUsage;
import org.jcodec.codecs.vpx.vp8.enums.TokenPartition;
import org.jcodec.codecs.vpx.vp8.enums.Tuning;
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
public class Config {
    /*
     * 4 versions of bitstream defined: 0 best quality/slowest decode, 3 lowest
     * quality/fastest decode
     */
    int Version;
    public int Width;
    public int Height;
    Rational timebase = Rational.ONE;
    public int target_bandwidth; /* kilobits per second */

    /*
     * Parameter used for applying denoiser. For temporal denoiser:
     * noise_sensitivity = 0 means off, noise_sensitivity = 1 means temporal
     * denoiser on for Y channel only, noise_sensitivity = 2 means temporal denoiser
     * on for all channels. noise_sensitivity = 3 means aggressive denoising mode.
     * noise_sensitivity >= 4 means adaptive denoising mode. Temporal denoiser is
     * enabled via the configuration option: CONFIG_TEMPORAL_DENOISING. For spatial
     * denoiser: noise_sensitivity controls the amount of pre-processing blur:
     * noise_sensitivity = 0 means off. Spatial denoiser invoked under
     * !CONFIG_TEMPORAL_DENOISING.
     */
    int noise_sensitivity;

    /* parameter used for sharpening output: recommendation 0: */
    public int Sharpness;
    private int cpu_used;

    public int rc_max_intra_bitrate_pct; // uint
    /* percent of rate boost for golden frame in CBR mode. */
    public int gf_cbr_boost_pct;// uint
    public int screen_content_mode; // uint could be 2!

    public CompressMode Mode = CompressMode.BESTQUALITY;

    /* Key Framing Operations */
    public boolean auto_key; /* automatically detect cut scenes */
    public int key_freq; /* maximum distance to key frame. */

    /* lagged compression (if allow_lag == 0 lag_in_frames is ignored) */
    int allow_lag;
    public int lag_in_frames; /* how many frames lag before we start encoding */

    /*
     * DATARATE CONTROL OPTIONS
     */

    public EndUsage end_usage; /* vbr or cbr */

    /* buffer targeting aggressiveness */
    public int under_shoot_pct;
    public int over_shoot_pct;

    /* buffering parameters */
    public long starting_buffer_level;
    public long optimal_buffer_level;
    public long maximum_buffer_size;

    public long starting_buffer_level_in_ms;
    public long optimal_buffer_level_in_ms;
    public long maximum_buffer_size_in_ms;

    /* controlling quality */
    public short fixed_q;
    short worst_allowed_q;
    short best_allowed_q;
    public short cq_level;

    /* allow internal resizing */
    boolean allow_spatial_resampling;
    int resample_down_water_mark;
    int resample_up_water_mark;

    /* allow internal frame rate alterations */
    boolean allow_df;
    public int drop_frames_water_mark;

    /*
     * END DATARATE CONTROL OPTIONS
     */

    /* these parameters aren't to be used in final build don't use!!! */
    public boolean play_alternate;
    int alt_freq;
    public short alt_q;
    public short key_q;
    public short gold_q;

    TokenPartition token_partitions; /* how many token partitions to create */

    /* early breakout threshold: for video conf recommend 800 */
    public int encode_breakout;

    /*
     * Bitfield defining the error resiliency features to enable. Can provide
     * decodable frames after losses in previous frames and decodable partitions
     * after losses in the same frame.
     */
    public boolean error_resilient_mode = false;

    public int arnr_max_frames;
    public int arnr_strength;
    public int arnr_type;

    List<CodecPkt> output_pkt_list = new ArrayList<CodecPkt>();

    public Tuning tuning;

    /* Temporal scaling parameters */
    public int number_of_layers;
    public int[] target_bitrate = new int[OnyxIf.VPX_TS_MAX_PERIODICITY];
    public int[] rate_decimator = new int[OnyxIf.VPX_TS_MAX_PERIODICITY];
    public int periodicity;
    public int[] layer_id = new int[OnyxIf.VPX_TS_MAX_PERIODICITY];

    public FrameRepeatHint hinter = null;

    private Config() {
        // for copying
    }

    public Config(CodecEncCfg cfg, ExtraCFG vp8_cfg) {
        Version = cfg.getG_profile();

        Width = cfg.getG_w();
        Height = cfg.getG_h();
        timebase = cfg.getG_timebase();

        error_resilient_mode = cfg.isG_error_resilient();
        Mode = CompressMode.BESTQUALITY;

        allow_lag = 0;
        lag_in_frames = 0;

        allow_df = (cfg.getRc_dropframe_thresh() > 0);
        drop_frames_water_mark = cfg.getRc_dropframe_thresh();

        allow_spatial_resampling = cfg.isRc_resize_allowed();
        resample_up_water_mark = cfg.getRc_resize_up_thresh();
        resample_down_water_mark = cfg.getRc_resize_down_thresh();

        switch (cfg.getRc_end_usage()) {
        case VPX_VBR:
            end_usage = EndUsage.LOCAL_FILE_PLAYBACK;
            break;
        case VPX_CBR:
            end_usage = EndUsage.STREAM_FROM_SERVER;
            break;
        case VPX_CQ:
            end_usage = EndUsage.CONSTRAINED_QUALITY;
            break;
        case VPX_Q:
            end_usage = EndUsage.CONSTANT_QUALITY;
            break;
        }

        target_bandwidth = cfg.getRc_target_bitrate();
        rc_max_intra_bitrate_pct = vp8_cfg.getRc_max_intra_bitrate_pct();
        gf_cbr_boost_pct = vp8_cfg.getGf_cbr_boost_pct();

        best_allowed_q = cfg.getRc_min_quantizer();
        worst_allowed_q = cfg.getRc_max_quantizer();
        cq_level = vp8_cfg.getCq_level();
        fixed_q = -1;

        under_shoot_pct = cfg.getRc_undershoot_pct();
        over_shoot_pct = cfg.getRc_overshoot_pct();

        maximum_buffer_size_in_ms = cfg.getRc_buf_sz();
        starting_buffer_level_in_ms = cfg.getRc_buf_initial_sz();
        optimal_buffer_level_in_ms = cfg.getRc_buf_optimal_sz();

        maximum_buffer_size = cfg.getRc_buf_sz();
        starting_buffer_level = cfg.getRc_buf_initial_sz();
        optimal_buffer_level = cfg.getRc_buf_optimal_sz();

        auto_key = cfg.getKf_mode() == CodecEncCfg.vpx_kf_mode.VPX_KF_AUTO
                && cfg.getKf_min_dist() != cfg.getKf_max_dist();
        key_freq = cfg.getKf_max_dist();

        number_of_layers = cfg.getTs_number_layers();
        periodicity = cfg.getTs_periodicity();

        if (number_of_layers > 1) {
            CommonUtils.vp8_copy(cfg.getTs_target_bitrate(), target_bitrate);
            CommonUtils.vp8_copy(cfg.getTs_rate_decimator(), rate_decimator);
            CommonUtils.vp8_copy(cfg.getTs_layer_id(), layer_id);
        }

        encode_breakout = vp8_cfg.getStatic_thresh();
        play_alternate = vp8_cfg.isEnable_auto_alt_ref();
        noise_sensitivity = vp8_cfg.getNoise_sensitivity();
        Sharpness = vp8_cfg.getSharpness();
        token_partitions = vp8_cfg.getToken_partitions();
        output_pkt_list = vp8_cfg.getPkt_list();

        arnr_max_frames = vp8_cfg.getArnr_max_frames();
        arnr_strength = vp8_cfg.getArnr_strength();
        arnr_type = vp8_cfg.getArnr_type();

        tuning = vp8_cfg.getTuning();

        screen_content_mode = vp8_cfg.getScreen_content_mode();
        setCpu_used(vp8_cfg.getCpu_used());
        hinter = vp8_cfg.getHinter();
    }

    Config copy() {
        Config n = new Config();
        n.allow_df = allow_df;
        n.allow_lag = allow_lag;
        n.allow_spatial_resampling = allow_spatial_resampling;
        n.alt_freq = alt_freq;
        n.alt_q = alt_q;
        n.arnr_max_frames = arnr_max_frames;
        n.arnr_strength = arnr_strength;
        n.arnr_type = arnr_type;
        n.auto_key = auto_key;
        n.best_allowed_q = best_allowed_q;
        n.cpu_used = cpu_used;
        n.cq_level = cq_level;
        n.drop_frames_water_mark = drop_frames_water_mark;
        n.encode_breakout = encode_breakout;
        n.end_usage = end_usage;
        n.error_resilient_mode = error_resilient_mode;
        n.fixed_q = fixed_q;
        n.gf_cbr_boost_pct = gf_cbr_boost_pct;
        n.gold_q = gold_q;
        n.Height = Height;
        n.key_freq = key_freq;
        n.key_q = key_q;
        n.lag_in_frames = lag_in_frames;
        n.layer_id = Arrays.copyOf(layer_id, layer_id.length);
        n.maximum_buffer_size = maximum_buffer_size;
        n.maximum_buffer_size_in_ms = maximum_buffer_size_in_ms;
        n.Mode = Mode;
        n.noise_sensitivity = noise_sensitivity;
        n.number_of_layers = number_of_layers;
        n.optimal_buffer_level = optimal_buffer_level;
        n.optimal_buffer_level_in_ms = optimal_buffer_level_in_ms;
        n.output_pkt_list = new ArrayList<CodecPkt>(output_pkt_list);
        n.over_shoot_pct = over_shoot_pct;
        n.periodicity = periodicity;
        n.play_alternate = play_alternate;
        n.rate_decimator = Arrays.copyOf(rate_decimator, rate_decimator.length);
        n.rc_max_intra_bitrate_pct = rc_max_intra_bitrate_pct;
        n.resample_down_water_mark = resample_down_water_mark;
        n.resample_up_water_mark = resample_up_water_mark;
        n.screen_content_mode = screen_content_mode;
        n.Sharpness = Sharpness;
        n.starting_buffer_level = starting_buffer_level;
        n.starting_buffer_level_in_ms = starting_buffer_level_in_ms;
        n.target_bandwidth = target_bandwidth;
        n.target_bitrate = Arrays.copyOf(target_bitrate, target_bitrate.length);
        n.timebase = timebase;
        n.token_partitions = token_partitions;
        n.tuning = tuning;
        n.under_shoot_pct = under_shoot_pct;
        n.Version = Version;
        n.Width = Width;
        n.worst_allowed_q = worst_allowed_q;
        n.hinter = hinter;
        return n;
    }

    public int getCpu_used() {
        return cpu_used;
    }

    public void setCpu_used(short cpu_used) {
        short low = -16, up = 16;
        if (Mode == CompressMode.GOODQUALITY) {
            low = -5;
            up = 5;
        }
        this.cpu_used = CommonUtils.clamp(cpu_used, low, up);
    }

}
