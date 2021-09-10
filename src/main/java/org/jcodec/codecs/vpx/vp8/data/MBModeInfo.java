package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.enums.BlockEnum;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;

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
public class MBModeInfo {
    public MBPredictionMode mode = MBPredictionMode.DC_PRED;
    public MBPredictionMode uv_mode = MBPredictionMode.DC_PRED; // uint8
    public MVReferenceFrame ref_frame = MVReferenceFrame.INTRA_FRAME;
    public MV mv = new MV();

    public BlockEnum partitioning = BlockEnum.BLOCK_16X8; // uint8
    /*
     * does this mb has coefficients at all, 1=no coefficients, 0=need decode tokens
     */
    public boolean mb_skip_coeff = false; // uint8
    public boolean need_to_clamp_mvs = false;// uint8
    /* Which set of segmentation parameters should be used for this MB */
    public int segment_id = 0;// uint8

    public void copyIn(MBModeInfo other) {
        this.mode = other.mode;
        this.uv_mode = other.uv_mode;
        this.ref_frame = other.ref_frame;
        this.mv.set(other.mv);
        this.partitioning = other.partitioning;
        this.mb_skip_coeff = other.mb_skip_coeff;
        this.need_to_clamp_mvs = other.need_to_clamp_mvs;
        this.segment_id = other.segment_id;
    }
}
