package org.jcodec.codecs.vpx.vp8.data;

import java.util.EnumMap;
import java.util.EnumSet;

import org.jcodec.codecs.vpx.vp8.enums.FrameType;
import org.jcodec.codecs.vpx.vp8.enums.FrameTypeFlags;
import org.jcodec.codecs.vpx.vp8.enums.LoopFilterType;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.enums.TokenPartition;
import org.jcodec.codecs.vpx.vp8.enums.Scaling;
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
public class CommonData {
    public static final int MAXQ = 127;
    public static final int QINDEX_RANGE = (MAXQ + 1);
    public static final int NUM_YV12_BUFFERS = 4;

    public static enum Quant {
        Y1, Y2, UV
    }

    public static enum Comp {
        DC(0), AC(1);

        public final int baseIndex;

        private Comp(int base) {
            baseIndex = base;
        }
    }

    public EnumMap<Quant, FullAccessIntArrPointer[]> dequant = new EnumMap<CommonData.Quant, FullAccessIntArrPointer[]>(
            Quant.class);

    public int Width;
    public int Height;
    public Scaling horiz_scale = Scaling.NORMAL;
    public Scaling vert_scale = Scaling.NORMAL;

    public YV12buffer frame_to_show;

    public YV12buffer[] yv12_fb = new YV12buffer[NUM_YV12_BUFFERS];
    int[] fb_idx_ref_cnt = new int[NUM_YV12_BUFFERS];
    public EnumMap<MVReferenceFrame, Integer> frameIdxs = new EnumMap<MVReferenceFrame, Integer>(
            MVReferenceFrame.class);
    public int new_fb_idx;

    YV12buffer temp_scale_frame;

    YV12buffer post_proc_buffer;
    YV12buffer post_proc_buffer_int;
    int post_proc_buffer_int_used;
    int[] pp_limits_buffer; /* post-processing filter coefficients */ // uchar

    public FrameType last_frame_type; /* Save last frame's frame type for motion search. */
    public FrameType frame_type;

    public boolean show_frame;

    public EnumSet<FrameTypeFlags> frame_flags;
    public int MBs;
    public int mb_rows;
    public int mb_cols;
    public int mode_info_stride;

    /* profile settings */
    public boolean mb_no_coeff_skip;
    public boolean no_lpf;
    public boolean use_bilinear_mc_filter;
    public boolean full_pixel;

    public short base_qindex;

    public EnumMap<Quant, EnumMap<Comp, Short>> delta_q = new EnumMap<CommonData.Quant, EnumMap<Comp, Short>>(
            Quant.class);

    /*
     * We allocate a MODE_INFO struct for each macroblock, together with an extra
     * row on top and column on the left to simplify prediction.
     */

    public FullAccessGenArrPointer<ModeInfo> mip; /* Base of allocated array */
    public FullAccessGenArrPointer<ModeInfo> mi; /* Corresponds to upper left visible macroblock */
    /* MODE_INFO for the last decoded frame to show */
    ModeInfo show_frame_mi;
    public LoopFilterType filter_type;

    public LoopFilterInfoN lf_info;

    public short filter_level;
    public int last_sharpness_level;
    public int sharpness_level;

    public boolean refresh_last_frame; /* Two state 0 = NO, 1 = YES */
    public boolean refresh_golden_frame; /* Two state 0 = NO, 1 = YES */
    public boolean refresh_alt_ref_frame; /* Two state 0 = NO, 1 = YES */

    public int copy_buffer_to_gf; /* 0 none, 1 Last to GF, 2 ARF to GF */
    public int copy_buffer_to_arf; /* 0 none, 1 Last to ARF, 2 GF to ARF */

    public boolean refresh_entropy_probs; /* Two state 0 = NO, 1 = YES */

    public EnumMap<MVReferenceFrame, Boolean> ref_frame_sign_bias = new EnumMap<MVReferenceFrame, Boolean>(
            MVReferenceFrame.class); /* Two state 0, 1 */

    /* Y,U,V,Y2 */
    public FullAccessGenArrPointer<EntropyContextPlanes> above_context; /* row of context for each plane */
    public EntropyContextPlanes left_context = new EntropyContextPlanes(); /* (up to) 4 contexts "" */

    public FrameContext lfc; /* last frame entropy */
    public FrameContext fc = new FrameContext(); /* this frame entropy */

    public int current_video_frame; // uint

    private byte version;

    public TokenPartition multi_token_partition;

    PostprocState postproc_state;
    int cpu_caps;

    public CommonData() {
        cpu_caps = 0;
        refresh_golden_frame = false;
        refresh_last_frame = true;
        refresh_entropy_probs = true;

        mb_no_coeff_skip = true;
        no_lpf = false;
        filter_type = LoopFilterType.NORMAL;
        use_bilinear_mc_filter = false;
        full_pixel = false;
        multi_token_partition = TokenPartition.ONE_PARTITION;

        /* Default disable buffer to buffer copying */
        copy_buffer_to_gf = 0;
        copy_buffer_to_arf = 0;
        lf_info = new LoopFilterInfoN(this);
        for (MVReferenceFrame rf : MVReferenceFrame.validFrames) {
            /* Initialize reference frame sign bias structure to defaults */
            ref_frame_sign_bias.put(rf, false);
            frameIdxs.put(rf, 0);
        }

        for (Quant q : Quant.values()) {
            FullAccessIntArrPointer[] temp = new FullAccessIntArrPointer[OnyxInt.QINDEX_RANGE];
            for (int i = 0; i < temp.length; i++) {
                temp[i] = new FullAccessIntArrPointer(2);
            }
            dequant.put(q, temp);
            EnumMap<Comp, Short> tempMap = new EnumMap<Comp, Short>(Comp.class);
            for (Comp c : Comp.values()) {
                tempMap.put(c, (short) 0);
            }
            delta_q.put(q, tempMap);
        }
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
        vp8_setup_version();
    }

    void vp8_setup_version() {
        switch (version) {
        case 0:
            no_lpf = false;
            filter_type = LoopFilterType.NORMAL;
            use_bilinear_mc_filter = false;
            full_pixel = false;
            break;
        case 1:
            no_lpf = false;
            filter_type = LoopFilterType.SIMPLE;
            use_bilinear_mc_filter = true;
            full_pixel = false;
            break;
        case 2:
            no_lpf = true;
            filter_type = LoopFilterType.NORMAL;
            use_bilinear_mc_filter = true;
            full_pixel = false;
            break;
        case 3:
            no_lpf = true;
            filter_type = LoopFilterType.SIMPLE;
            use_bilinear_mc_filter = true;
            full_pixel = true;
            break;
        default:
            /* 4,5,6,7 are reserved for future use */
            no_lpf = false;
            filter_type = LoopFilterType.NORMAL;
            use_bilinear_mc_filter = false;
            full_pixel = false;
            break;
        }
    }

    public void vp8_alloc_frame_buffers(int width, int height) {
        int i;

        vp8_de_alloc_frame_buffers();

        /* our internal buffers are always multiples of 16 */
        if ((width & 0xf) != 0)
            width += 16 - (width & 0xf);

        if ((height & 0xf) != 0)
            height += 16 - (height & 0xf);

        for (i = 0; i < NUM_YV12_BUFFERS; ++i) {
            fb_idx_ref_cnt[i] = 0;
            yv12_fb[i] = new YV12buffer(width, height);
            yv12_fb[i].flags = EnumSet.noneOf(MVReferenceFrame.class);
        }

        new_fb_idx = 0;
        frameIdxs.clear();
        frameIdxs.put(MVReferenceFrame.LAST_FRAME, 1);
        frameIdxs.put(MVReferenceFrame.GOLDEN_FRAME, 2);
        frameIdxs.put(MVReferenceFrame.ALTREF_FRAME, 3);

        fb_idx_ref_cnt[0] = 1;
        fb_idx_ref_cnt[1] = 1;
        fb_idx_ref_cnt[2] = 1;
        fb_idx_ref_cnt[3] = 1;

        temp_scale_frame = new YV12buffer(width, 16);

        mb_rows = height >> 4;
        mb_cols = width >> 4;
        MBs = mb_rows * mb_cols;
        mode_info_stride = mb_cols + 1;
        mip = new FullAccessGenArrPointer<ModeInfo>((mb_cols + 1) * (mb_rows + 1));
        for (i = 0; i < mip.size(); i++) {
            mip.setRel(i, new ModeInfo());
        }

        mi = mip.shallowCopyWithPosInc(mode_info_stride + 1);

        /*
         * Allocation of previous mode info will be done in vp8_decode_frame() as it is
         * a decoder only data
         */

        above_context = new FullAccessGenArrPointer<EntropyContextPlanes>(mb_cols);
        for (i = 0; i < mb_cols; i++) {
            above_context.setRel(i, new EntropyContextPlanes());
        }
    }

    public void vp8_de_alloc_frame_buffers() {
        temp_scale_frame = null;
        above_context = null;
        mip = null;
    }

}
