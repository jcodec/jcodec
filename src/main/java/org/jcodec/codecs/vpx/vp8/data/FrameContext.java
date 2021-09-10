package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.VPXConst;
import org.jcodec.codecs.vpx.vp8.CommonUtils;
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
public class FrameContext {
    public FullAccessIntArrPointer bmode_prob = new FullAccessIntArrPointer(EntropyMode.vp8_bmode_prob.size());
    public FullAccessIntArrPointer ymode_prob = new FullAccessIntArrPointer(
            EntropyMode.vp8_ymode_prob.size());/* interframe intra mode probs */
    public FullAccessIntArrPointer uv_mode_prob = new FullAccessIntArrPointer(EntropyMode.vp8_uv_mode_prob.size());
    FullAccessIntArrPointer sub_mv_ref_prob = new FullAccessIntArrPointer(EntropyMode.sub_mv_ref_prob.size());
    public short[][][][] coef_probs = new short[Entropy.BLOCK_TYPES][Entropy.COEF_BANDS][Entropy.PREV_COEF_CONTEXTS][Entropy.ENTROPY_NODES];
    public MVContext[] mvc = new MVContext[2];

    public FrameContext(FrameContext other) {
        CommonUtils.vp8_copy(other.bmode_prob, bmode_prob);
        CommonUtils.vp8_copy(other.ymode_prob, ymode_prob);
        CommonUtils.vp8_copy(other.uv_mode_prob, uv_mode_prob);
        CommonUtils.vp8_copy(other.sub_mv_ref_prob, sub_mv_ref_prob);
        CommonUtils.vp8_copy(other.coef_probs, this.coef_probs);
        for (int i = 0; i < mvc.length; i++) {
            this.mvc[i] = new MVContext(other.mvc[i]);
        }
    }

    public FrameContext() {
        vp8_init_mbmode_probs();
        toDefault();
    }

    public void vp8_init_mbmode_probs() {
        CommonUtils.vp8_copy(EntropyMode.vp8_bmode_prob, bmode_prob);
        CommonUtils.vp8_copy(EntropyMode.vp8_ymode_prob, ymode_prob);
        CommonUtils.vp8_copy(EntropyMode.vp8_uv_mode_prob, uv_mode_prob);
        CommonUtils.vp8_copy(EntropyMode.sub_mv_ref_prob, sub_mv_ref_prob);
    }

    public void vp8_default_coef_probs() {
        CommonUtils.vp8_copy(VPXConst.tokenDefaultBinProbs, this.coef_probs);
    }

    public void vp8_default_mvctxt() {
        mvc[0] = new MVContext(MVContext.vp8_default_mv_context[0]);
        mvc[1] = new MVContext(MVContext.vp8_default_mv_context[1]);
    }

    public void toDefault() {
        vp8_default_coef_probs();
        vp8_default_mvctxt();
    }
}
