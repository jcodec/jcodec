package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.LoopFilterInfo;
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
public class LFFilters {

    /* should we apply any filter at all ( 11111111 yes, 00000000 no) */
    static int vp8_filter_mask(int limit, int blimit, int p3, int p2, int p1, int p0, int q0, int q1, int q2, int q3) {
        boolean mask = false;
        mask |= (Math.abs(p3 - p2) > limit);
        mask |= (Math.abs(p2 - p1) > limit);
        mask |= (Math.abs(p1 - p0) > limit);
        mask |= (Math.abs(q1 - q0) > limit);
        mask |= (Math.abs(q2 - q1) > limit);
        mask |= (Math.abs(q3 - q2) > limit);
        mask |= (filtermaskcore(p1, p0, q0, q1) > blimit);
        return mask ? 0 : -1;
    }

    /* is there high variance internal edge ( 11111111 yes, 00000000 no) */
    static int vp8_hevmask(int thresh, int p1, int p0, int q0, int q1) {
        int hev = 0;
        hev |= Math.abs(p1 - p0) > thresh ? -1 : 0;
        hev |= (Math.abs(q1 - q0) > thresh) ? -1 : 0;
        return hev;
    }

    private static short unsignedToSigned(short in) {
        return (short) (in - 128);
    }

    private static short signedToUnsigned(short in) {
        return (short) (in + 128);
    }

    static void vp8_filter(int mask, int hev, FullAccessIntArrPointer ptr, int op1, int op0, int oq0, int oq1) {
        int filter_value, Filter1, Filter2;
        short u;

        final short ps1 = unsignedToSigned(ptr.getRel(op1));
        final short ps0 = unsignedToSigned(ptr.getRel(op0));
        final short qs0 = unsignedToSigned(ptr.getRel(oq0));
        final short qs1 = unsignedToSigned(ptr.getRel(oq1));

        /* add outer taps if we have high edge variance */
        filter_value = CommonUtils.byteClamp((short) (ps1 - qs1));
        filter_value &= hev;

        /* inner taps */
        filter_value = CommonUtils.byteClamp((short) (filter_value + 3 * (qs0 - ps0)));
        filter_value &= mask;

        /*
         * save bottom 3 bits so that we round one side +4 and the other +3 if it equals
         * 4 we'll set it to adjust by -1 to account for the fact we'd round it by 3 the
         * other way
         */
        Filter1 = CommonUtils.byteClamp((short) (filter_value + 4));
        Filter2 = CommonUtils.byteClamp((short) (filter_value + 3));
        Filter1 >>= 3;
        Filter2 >>= 3;
        u = CommonUtils.byteClamp((short) (qs0 - Filter1));
        ptr.setRel(oq0, signedToUnsigned(u));
        u = CommonUtils.byteClamp((short) (ps0 + Filter2));
        ptr.setRel(op0, signedToUnsigned(u));
        filter_value = Filter1;

        /* outer tap adjustments */
        filter_value += 1;
        filter_value >>= 1;
        filter_value &= ~hev;

        u = CommonUtils.byteClamp((short) (qs1 - filter_value));
        ptr.setRel(oq1, signedToUnsigned(u));
        u = CommonUtils.byteClamp((short) (ps1 + filter_value));
        ptr.setRel(op1, signedToUnsigned(u));
    }

    static void loop_filter_horizontal_edge(FullAccessIntArrPointer s, int p, /* pitch */
            ReadOnlyIntArrPointer blimit, ReadOnlyIntArrPointer limit, ReadOnlyIntArrPointer thresh, int count) {
        int i = 0;
        do {
            filterCore(s, blimit, limit, thresh, p);
            s.inc();
        } while (++i < count * 8);
    }

    private static int getMask(ReadOnlyIntArrPointer s, ReadOnlyIntArrPointer blimit, ReadOnlyIntArrPointer limit,
            int p) {
        return vp8_filter_mask(limit.get(), blimit.get(), s.getRel(-4 * p), s.getRel(-3 * p), s.getRel(-2 * p),
                s.getRel(-1 * p), s.getRel(0 * p), s.getRel(1 * p), s.getRel(2 * p), s.getRel(3 * p));
    }

    private static int getHEV(FullAccessIntArrPointer s, ReadOnlyIntArrPointer thresh, int p) {
        /* high edge variance */
        return vp8_hevmask(thresh.get(), s.getRel(-2 * p), s.getRel(-1 * p), s.getRel(0 * p), s.getRel(1 * p));

    }

    private static void filterCore(FullAccessIntArrPointer s, ReadOnlyIntArrPointer blimit, ReadOnlyIntArrPointer limit,
            ReadOnlyIntArrPointer thresh, int p) {
        int mask = getMask(s, blimit, limit, p), hev = getHEV(s, thresh, p);
        vp8_filter(mask, hev, s, -(p << 1), -p, 0, p);

    }

    static void loop_filter_vertical_edge(FullAccessIntArrPointer s, int p, /* pitch */
            ReadOnlyIntArrPointer blimit, ReadOnlyIntArrPointer limit, ReadOnlyIntArrPointer thresh, int count) {
        int i = 0;

        /*
         * loop filter designed to work using chars so that we can make maximum use of 8
         * bit simd instrinttions.
         */
        do {
            filterCore(s, blimit, limit, thresh, 1);
            s.incBy(p);
        } while (++i < count * 8);
    }

    static void vp8_mbfilter(int mask, int hev, FullAccessIntArrPointer ptr, int op2, int op1, int op0, int oq0,
            int oq1, int oq2) {
        short s, u;
        short filter_value, Filter1, Filter2;
        final short ps2 = unsignedToSigned(ptr.getRel(op2));
        final short ps1 = unsignedToSigned(ptr.getRel(op1));
        short ps0 = unsignedToSigned(ptr.getRel(op0));
        short qs0 = unsignedToSigned(ptr.getRel(oq0));
        final short qs1 = unsignedToSigned(ptr.getRel(oq1));
        final short qs2 = unsignedToSigned(ptr.getRel(oq2));

        /* add outer taps if we have high edge variance */
        filter_value = CommonUtils.byteClamp((short) (ps1 - qs1));
        filter_value = CommonUtils.byteClamp((short) (filter_value + 3 * (qs0 - ps0)));
        filter_value &= mask;

        Filter2 = filter_value;
        Filter2 &= hev;

        /* save bottom 3 bits so that we round one side +4 and the other +3 */
        Filter1 = CommonUtils.byteClamp((short) (Filter2 + 4));
        Filter2 = CommonUtils.byteClamp((short) (Filter2 + 3));
        Filter1 >>= 3;
        Filter2 >>= 3;
        qs0 = CommonUtils.byteClamp((short) (qs0 - Filter1));
        ps0 = CommonUtils.byteClamp((short) (ps0 + Filter2));

        /* only apply wider filter if not high edge variance */
        filter_value &= ~hev;
        Filter2 = filter_value;

        /* roughly 3/7th difference across boundary */
        u = CommonUtils.byteClamp((short) ((63 + Filter2 * 27) >> 7));
        s = CommonUtils.byteClamp((short) (qs0 - u));
        ptr.setRel(oq0, signedToUnsigned(s));
        s = CommonUtils.byteClamp((short) (ps0 + u));
        ptr.setRel(op0, signedToUnsigned(s));

        /* roughly 2/7th difference across boundary */
        u = CommonUtils.byteClamp((short) ((63 + Filter2 * 18) >> 7));
        s = CommonUtils.byteClamp((short) (qs1 - u));
        ptr.setRel(oq1, signedToUnsigned(s));
        s = CommonUtils.byteClamp((short) (ps1 + u));
        ptr.setRel(op1, signedToUnsigned(s));

        /* roughly 1/7th difference across boundary */
        u = CommonUtils.byteClamp((short) ((63 + Filter2 * 9) >> 7));
        s = CommonUtils.byteClamp((short) (qs2 - u));
        ptr.setRel(oq2, signedToUnsigned(s));
        s = CommonUtils.byteClamp((short) (ps2 + u));
        ptr.setRel(op2, signedToUnsigned(s));
    }

    static void mbloop_filter_horizontal_edge(FullAccessIntArrPointer s, int p, /* pitch */
            ReadOnlyIntArrPointer blimit, ReadOnlyIntArrPointer limit, ReadOnlyIntArrPointer thresh, int count) {
        int i = 0;
        /*
         * loop filter designed to work using chars so that we can make maximum use of 8
         * bit simd instrinttions.
         */
        s = s.shallowCopy();
        do {
            mbfiltercore(s, blimit, limit, thresh, p);
            s.inc();
        } while (++i < count * 8);
    }

    private static void mbfiltercore(FullAccessIntArrPointer s, ReadOnlyIntArrPointer blimit,
            ReadOnlyIntArrPointer limit, ReadOnlyIntArrPointer thresh, int p) {
        int mask = getMask(s, blimit, limit, p), hev = getHEV(s, thresh, p);
        vp8_mbfilter(mask, hev, s, -3 * p, -2 * p, -p, 0, p, 2 * p);
    }

    static void mbloop_filter_vertical_edge(FullAccessIntArrPointer s, int p, /* pitch */
            ReadOnlyIntArrPointer blimit, ReadOnlyIntArrPointer limit, ReadOnlyIntArrPointer thresh, int count) {
        int i = 0;
        s = s.shallowCopy();
        do {
            mbfiltercore(s, blimit, limit, thresh, 1);
            s.incBy(p);
        } while (++i < count * 8);
    }

    /* should we apply any filter at all ( 11111111 yes, 00000000 no) */
    static int vp8_simple_filter_mask(int blimit, int p1, int p0, int q0, int q1) {
        return filtermaskcore(p1, p0, q0, q1) <= blimit ? -1 : 0;
    }

    private static int filtermaskcore(int p1, int p0, int q0, int q1) {
        return Math.abs(p0 - q0) * 2 + Math.abs(p1 - q1) / 2;
    }

    static void vp8_simple_filter(int mask, FullAccessIntArrPointer ptr, int op1, int op0, int oq0, int oq1) {
        int filter_value, Filter1, Filter2;
        final short p1 = unsignedToSigned(ptr.getRel(op1));
        final short p0 = unsignedToSigned(ptr.getRel(op0));
        final short q0 = unsignedToSigned(ptr.getRel(oq0));
        final short q1 = unsignedToSigned(ptr.getRel(oq1));
        short u;

        filter_value = CommonUtils.byteClamp((short) (p1 - q1));
        filter_value = CommonUtils.byteClamp((short) (filter_value + 3 * (q0 - p0)));
        filter_value &= mask;

        /* save bottom 3 bits so that we round one side +4 and the other +3 */
        Filter1 = CommonUtils.byteClamp((short) (filter_value + 4));
        Filter1 >>= 3;
        u = CommonUtils.byteClamp((short) (q0 - Filter1));
        ptr.setRel(oq0, signedToUnsigned(u));

        Filter2 = CommonUtils.byteClamp((short) (filter_value + 3));
        Filter2 >>= 3;
        u = CommonUtils.byteClamp((short) (p0 + Filter2));
        ptr.setRel(op0, signedToUnsigned(u));
    }

    static void vp8_loop_filter_simple_horizontal_edge(FullAccessIntArrPointer y_ptr, int y_stride,
            ReadOnlyIntArrPointer blimit) {
        int mask = 0;
        int i = 0;
        y_ptr.savePos();

        do {
            mask = vp8_simple_filter_mask(blimit.get(), y_ptr.getRel(-2 * y_stride), y_ptr.getRel(-1 * y_stride),
                    y_ptr.getRel(0 * y_stride), y_ptr.getRel(1 * y_stride));
            vp8_simple_filter(mask, y_ptr, -2 * y_stride, -y_stride, 0, y_stride);
            y_ptr.inc();
        } while (++i < 16);
        y_ptr.rewindToSaved();
    }

    static void vp8_loop_filter_simple_vertical_edge(FullAccessIntArrPointer y_ptr, int y_stride,
            ReadOnlyIntArrPointer blimit) {
        int mask = 0;
        int i = 0;
        y_ptr.savePos();

        do {
            mask = vp8_simple_filter_mask(blimit.get(), y_ptr.getRel(-2), y_ptr.getRel(-1), y_ptr.get(),
                    y_ptr.getRel(1));
            vp8_simple_filter(mask, y_ptr, -2, -1, 0, 1);
            y_ptr.incBy(y_stride);
        } while (++i < 16);
        y_ptr.rewindToSaved();
    }

    /* Horizontal MB filtering */
    static void vp8_loop_filter_mbh(FullAccessIntArrPointer y_ptr, FullAccessIntArrPointer u_ptr,
            FullAccessIntArrPointer v_ptr, int y_stride, int uv_stride, LoopFilterInfo lfi) {
        mbloop_filter_horizontal_edge(y_ptr, y_stride, lfi.mblim, lfi.lim, lfi.hev_thr, 2);

        if (u_ptr != null) {
            mbloop_filter_horizontal_edge(u_ptr, uv_stride, lfi.mblim, lfi.lim, lfi.hev_thr, 1);
        }

        if (v_ptr != null) {
            mbloop_filter_horizontal_edge(v_ptr, uv_stride, lfi.mblim, lfi.lim, lfi.hev_thr, 1);
        }
    }

    /* Vertical MB Filtering */
    static void vp8_loop_filter_mbv(FullAccessIntArrPointer y_ptr, FullAccessIntArrPointer u_ptr,
            FullAccessIntArrPointer v_ptr, int y_stride, int uv_stride, LoopFilterInfo lfi) {
        mbloop_filter_vertical_edge(y_ptr, y_stride, lfi.mblim, lfi.lim, lfi.hev_thr, 2);

        if (u_ptr != null) {
            mbloop_filter_vertical_edge(u_ptr, uv_stride, lfi.mblim, lfi.lim, lfi.hev_thr, 1);
        }

        if (v_ptr != null) {
            mbloop_filter_vertical_edge(v_ptr, uv_stride, lfi.mblim, lfi.lim, lfi.hev_thr, 1);
        }
    }

    /* Horizontal B Filtering */
    static void vp8_loop_filter_bh(FullAccessIntArrPointer y_ptr, FullAccessIntArrPointer u_ptr,
            FullAccessIntArrPointer v_ptr, int y_stride, int uv_stride, LoopFilterInfo lfi) {
        loop_filter_horizontal_edge(y_ptr.shallowCopyWithPosInc(y_stride << 2), y_stride, lfi.blim, lfi.lim,
                lfi.hev_thr, 2);
        loop_filter_horizontal_edge(y_ptr.shallowCopyWithPosInc(y_stride << 3), y_stride, lfi.blim, lfi.lim,
                lfi.hev_thr, 2);
        loop_filter_horizontal_edge(y_ptr.shallowCopyWithPosInc(12 * y_stride), y_stride, lfi.blim, lfi.lim,
                lfi.hev_thr, 2);

        if (u_ptr != null) {
            loop_filter_horizontal_edge(u_ptr.shallowCopyWithPosInc(uv_stride << 2), uv_stride, lfi.blim, lfi.lim,
                    lfi.hev_thr, 1);
        }

        if (v_ptr != null) {
            loop_filter_horizontal_edge(v_ptr.shallowCopyWithPosInc(uv_stride << 2), uv_stride, lfi.blim, lfi.lim,
                    lfi.hev_thr, 1);
        }
    }

    static void vp8_loop_filter_bhs(FullAccessIntArrPointer y_ptr, int y_stride, ReadOnlyIntArrPointer blimit) {
        vp8_loop_filter_simple_horizontal_edge(y_ptr.shallowCopyWithPosInc(y_stride << 2), y_stride, blimit);
        vp8_loop_filter_simple_horizontal_edge(y_ptr.shallowCopyWithPosInc(y_stride << 3), y_stride, blimit);
        vp8_loop_filter_simple_horizontal_edge(y_ptr.shallowCopyWithPosInc(12 * y_stride), y_stride, blimit);
    }

    /* Vertical B Filtering */
    static void vp8_loop_filter_bv(FullAccessIntArrPointer y_ptr, FullAccessIntArrPointer u_ptr,
            FullAccessIntArrPointer v_ptr, int y_stride, int uv_stride, LoopFilterInfo lfi) {
        loop_filter_vertical_edge(y_ptr.shallowCopyWithPosInc(4), y_stride, lfi.blim, lfi.lim, lfi.hev_thr, 2);
        loop_filter_vertical_edge(y_ptr.shallowCopyWithPosInc(8), y_stride, lfi.blim, lfi.lim, lfi.hev_thr, 2);
        loop_filter_vertical_edge(y_ptr.shallowCopyWithPosInc(12), y_stride, lfi.blim, lfi.lim, lfi.hev_thr, 2);

        if (u_ptr != null) {
            loop_filter_vertical_edge(u_ptr.shallowCopyWithPosInc(4), uv_stride, lfi.blim, lfi.lim, lfi.hev_thr, 1);
        }

        if (v_ptr != null) {
            loop_filter_vertical_edge(v_ptr.shallowCopyWithPosInc(4), uv_stride, lfi.blim, lfi.lim, lfi.hev_thr, 1);
        }
    }

    static void vp8_loop_filter_bvs(FullAccessIntArrPointer y_ptr, int y_stride, ReadOnlyIntArrPointer blimit) {
        vp8_loop_filter_simple_vertical_edge(y_ptr.shallowCopyWithPosInc(4), y_stride, blimit);
        vp8_loop_filter_simple_vertical_edge(y_ptr.shallowCopyWithPosInc(8), y_stride, blimit);
        vp8_loop_filter_simple_vertical_edge(y_ptr.shallowCopyWithPosInc(12), y_stride, blimit);
    }

}
