package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
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
public class BlockD {
    public static final boolean SEGMENT_DELTADATA = false;
    public static final boolean SEGMENT_ABSDATA = true;

    public static final int MAX_MB_SEGMENTS = 4;
    public static final int MB_FEATURE_TREE_PROBS = 3;
    public static final int MAX_REF_LF_DELTAS = 4;
    public static final int MAX_MODE_LF_DELTAS = 4;
    public static final int VP8_YMODES = MBPredictionMode.B_PRED.ordinal() + 1;
    public static final int VP8_UV_MODES = MBPredictionMode.TM_PRED.ordinal() + 1;
    public static final int VP8_SUBMVREFS = (1 + BPredictionMode.NEW4X4.ordinal() - BPredictionMode.LEFT4X4.ordinal());
    public static final int VP8_BINTRAMODES = (BPredictionMode.B_HU_PRED.ordinal() + 1); /* 10 */
    public static final int VP8_MVREFS = (1 + MBPredictionMode.SPLITMV.ordinal()
            - MBPredictionMode.NEARESTMV.ordinal());
    public static final int[] vp8_block2left = { 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7,
            8 };
    public static final int[] vp8_block2above = { 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 4, 5, 4, 5, 6, 7, 6,
            7, 8 };

    public FullAccessIntArrPointer qcoeff; // short
    public FullAccessIntArrPointer dqcoeff; // short
    public FullAccessIntArrPointer predictor; // uchar
    public FullAccessIntArrPointer dequant; // short

    private int offset;
    public FullAccessIntArrPointer eob; // just a single number at pos that we are interested in!

    private int prevPos = -1;
    private FullAccessIntArrPointer prevBase;
    private FullAccessIntArrPointer currPointer;

    public BModeInfo bmi = new BModeInfo();

    public BlockD(FullAccessIntArrPointer pred) {
        predictor = pred;
    }

    public void calcBlockYOffset(final int blockIdx, final int stride) {
        offset = ((blockIdx >> 2) * stride + (blockIdx & 3)) << 2;
        prevBase = null;
    }

    public void calcBlockUVOffset(final int blockIdx, final int stride) {
        offset = ((blockIdx - 16 >> 1) * stride + (blockIdx & 1)) << 2;
        prevBase = null;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
        prevBase = null;
    }

    public FullAccessIntArrPointer getOffsetPointer(final FullAccessIntArrPointer base) {
        if (base != prevBase || base.getPos() != prevPos) {
            prevBase = base;
            prevPos = base.getPos();
            currPointer = base.shallowCopyWithPosInc(offset);
        }
        return currPointer;
    }
}
