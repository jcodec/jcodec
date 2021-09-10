package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;
import org.jcodec.codecs.vpx.vp8.enums.BlockEnum;
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
public class BestSegInfo {
    public MV ref_mv;
    public MV mvp;

    public long segment_rd;
    public BlockEnum segment_num;
    public int r;
    public int d;
    public int segment_yrate;
    public BPredictionMode[] modes = new BPredictionMode[16];
    public MV[] mvs = new MV[modes.length];
    public short[] eobs = new short[modes.length];

    public int mvthresh;
    public int[] mdcounts;

    public MV[] sv_mvp = new MV[4]; /* save 4 mvp from 8x8 */
    public FullAccessIntArrPointer sv_istep = new FullAccessIntArrPointer(2); /* save 2 initial step_param for 16x8/8x16 */

    public BestSegInfo(long best_rd, MV best_ref_mv, int mvthresh, int[] mdcounts) {
        segment_rd = best_rd;
        ref_mv = best_ref_mv;
        mvp = best_ref_mv.copy();
        this.mvthresh = mvthresh;
        this.mdcounts = mdcounts;
        segment_num = BlockEnum.BLOCK_16X8;
        r = d = 0;

        for (int i = 0; i < modes.length; ++i) {
            modes[i] = BPredictionMode.ZERO4X4;
            eobs[i] = 0;
            mvs[i] = new MV();
        }
        for (int i = 0; i < sv_mvp.length; i++) {
            sv_mvp[i] = new MV();
        }
    }
}
