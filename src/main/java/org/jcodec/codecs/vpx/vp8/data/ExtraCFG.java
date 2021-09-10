package org.jcodec.codecs.vpx.vp8.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.codecs.vpx.vp8.enums.TokenPartition;
import org.jcodec.codecs.vpx.vp8.enums.Tuning;

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
public class ExtraCFG {
    private static Map<String, FrameRepeatHint> allHinters = Collections
            .synchronizedMap(new HashMap<String, FrameRepeatHint>());

    public static FrameRepeatHint getHinter(String hinterId) {
        return allHinters.get(hinterId);
    }

    private FrameRepeatHint hinter = new FrameRepeatHint();
    private List<CodecPkt> pkt_list = null;
    private short cpu_used = 0;
    /** available cpu percentage in 1/16 */
    /** if encoder decides to uses alternate reference frame */
    private boolean enable_auto_alt_ref = false;
    private int noise_sensitivity = 0;
    private int Sharpness = 0;
    private int static_thresh = 0;
    private TokenPartition token_partitions = TokenPartition.ONE_PARTITION;
    private int arnr_max_frames = 0; /* alt_ref Noise Reduction Max Frame Count */
    private int arnr_strength = 3; /* alt_ref Noise Reduction Strength */
    private int arnr_type = 3; /* alt_ref filter type */
    private Tuning tuning = Tuning.TUNE_PSNR;
    private short cq_level = 10; /* constrained quality level */
    private int rc_max_intra_bitrate_pct = 0;
    private int gf_cbr_boost_pct = 0;
    // 0 = off, 1 = on, 2 = on & aggressive rate control
    private int screen_content_mode = 0;
    private String hinterId = null;

    public void setHinterId(String hinterId) {
        this.hinterId = hinterId;
        allHinters.put(hinterId, hinter);
    }

    public String getHinterId() {
        return hinterId;
    }

    public FrameRepeatHint getHinter() {
        return hinterId == null ? null : hinter;
    }

    public List<CodecPkt> getPkt_list() {
        return pkt_list;
    }

    public void setPkt_list(List<CodecPkt> pkt_list) {
        this.pkt_list = pkt_list;
    }

    public short getCpu_used() {
        return cpu_used;
    }

    public void setCpu_used(short cpu_used) {
        CodecEncCfg.rangeCheck(cpu_used, -16, 16);
        this.cpu_used = cpu_used;
    }

    public boolean isEnable_auto_alt_ref() {
        return enable_auto_alt_ref;
    }

    public void setEnable_auto_alt_ref(boolean enable_auto_alt_ref) {
        this.enable_auto_alt_ref = enable_auto_alt_ref;
    }

    public int getNoise_sensitivity() {
        return noise_sensitivity;
    }

    public void setNoise_sensitivity(int noise_sensitivity) {
        CodecEncCfg.rangeCheck(noise_sensitivity, 0, 6);
        this.noise_sensitivity = noise_sensitivity;
    }

    public int getSharpness() {
        return Sharpness;
    }

    public void setSharpness(int sharpness) {
        CodecEncCfg.rangeCheck(sharpness, 0, 7);
        Sharpness = sharpness;
    }

    public int getStatic_thresh() {
        return static_thresh;
    }

    public void setStatic_thresh(int static_thresh) {
        this.static_thresh = static_thresh;
    }

    public TokenPartition getToken_partitions() {
        return token_partitions;
    }

    public void setToken_partitions(TokenPartition token_partitions) {
        this.token_partitions = token_partitions;
    }

    public int getArnr_max_frames() {
        return arnr_max_frames;
    }

    public void setArnr_max_frames(int arnr_max_frames) {
        CodecEncCfg.rangeCheck(arnr_max_frames, 0, 15);
        this.arnr_max_frames = arnr_max_frames;
    }

    public int getArnr_strength() {
        return arnr_strength;
    }

    public void setArnr_strength(int arnr_strength) {
        CodecEncCfg.rangeCheck(arnr_strength, 0, 6);
        this.arnr_strength = arnr_strength;
    }

    public int getArnr_type() {
        return arnr_type;
    }

    public void setArnr_type(int arnr_type) {
        CodecEncCfg.rangeCheck(arnr_type, 1, 3);
        this.arnr_type = arnr_type;
    }

    public Tuning getTuning() {
        return tuning;
    }

    public void setTuning(Tuning tuning) {
        this.tuning = tuning;
    }

    public short getCq_level() {
        return cq_level;
    }

    public void setCq_level(short cq_level) {
        CodecEncCfg.rangeCheck(cq_level, 0, 63);
        this.cq_level = cq_level;
    }

    public int getRc_max_intra_bitrate_pct() {
        return rc_max_intra_bitrate_pct;
    }

    public void setRc_max_intra_bitrate_pct(int rc_max_intra_bitrate_pct) {
        this.rc_max_intra_bitrate_pct = rc_max_intra_bitrate_pct;
    }

    public int getGf_cbr_boost_pct() {
        return gf_cbr_boost_pct;
    }

    public void setGf_cbr_boost_pct(int gf_cbr_boost_pct) {
        this.gf_cbr_boost_pct = gf_cbr_boost_pct;
    }

    public int getScreen_content_mode() {
        return screen_content_mode;
    }

    public void setScreen_content_mode(int screen_content_mode) {
        CodecEncCfg.rangeCheck(screen_content_mode, 0, 2);
        this.screen_content_mode = screen_content_mode;
    }

}
