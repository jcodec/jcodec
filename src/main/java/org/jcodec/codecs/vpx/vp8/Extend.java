package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.YV12buffer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;

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
public class Extend {

    private static void copy4(FullAccessIntArrPointer YPtr, FullAccessIntArrPointer UPtr,
            FullAccessIntArrPointer VPtr) {
        YPtr.memset(0, YPtr.getRel(-1), 4);
        UPtr.memset(0, UPtr.getRel(-1), 4);
        VPtr.memset(0, VPtr.getRel(-1), 4);
    }

    /* note the extension is only for the last row, for intra prediction purpose */
    static void vp8_extend_mb_row(YV12buffer ybf, FullAccessIntArrPointer YPtr, FullAccessIntArrPointer UPtr,
            FullAccessIntArrPointer VPtr) {
        YPtr.incBy(ybf.y_stride * 14);
        UPtr.incBy(ybf.uv_stride * 6);
        VPtr.incBy(ybf.uv_stride * 6);

        copy4(YPtr, UPtr, VPtr);

        YPtr.incBy(ybf.y_stride);
        UPtr.incBy(ybf.uv_stride);
        VPtr.incBy(ybf.uv_stride);

        copy4(YPtr, UPtr, VPtr);

        YPtr.incBy(-ybf.y_stride * 15);
        UPtr.incBy(-ybf.uv_stride * 7);
        VPtr.incBy(-ybf.uv_stride * 7);
    }

    static void copy_and_extend_plane(PositionableIntArrPointer s, /* source */
            int sp, /* source pitch */
            FullAccessIntArrPointer d, /* destination */
            int dp, /* destination pitch */
            int h, /* height */
            int w, /* width */
            int et, /* extend top border */
            int el, /* extend left border */
            int eb, /* extend bottom border */
            int er, /* extend right border */
            int interleave_step) { /* step between pixels of the current plane */
        int i, j;
        PositionableIntArrPointer src_ptr1, src_ptr2;
        FullAccessIntArrPointer dest_ptr1, dest_ptr2;
        int linesize;

        if (interleave_step < 1)
            interleave_step = 1;

        /* copy the left and right most columns out */
        src_ptr1 = PositionableIntArrPointer.makePositionable(s);
        src_ptr2 = PositionableIntArrPointer.makePositionableAndInc(s, (w - 1) * interleave_step);
        dest_ptr1 = d.shallowCopyWithPosInc(-el);
        dest_ptr2 = d.shallowCopyWithPosInc(w);

        for (i = 0; i < h; ++i) {
            dest_ptr1.memset(0, src_ptr1.get(), el);
            if (interleave_step == 1) {
                dest_ptr1.memcopyin(el, src_ptr1, 0, w);
            } else {
                for (j = 0; j < w; j++) {
                    dest_ptr1.setRel(el + j, src_ptr1.getRel(interleave_step * j));
                }
            }
            dest_ptr2.memset(0, src_ptr2.get(), er);
            src_ptr1.incBy(sp);
            src_ptr2.incBy(sp);
            dest_ptr1.incBy(dp);
            dest_ptr2.incBy(dp);
        }

        /*
         * Now copy the top and bottom lines into each line of the respective borders
         */
        src_ptr1 = PositionableIntArrPointer.makePositionableAndInc(d, -el);
        src_ptr2 = PositionableIntArrPointer.makePositionableAndInc(d, dp * (h - 1) - el);
        dest_ptr1 = d.shallowCopyWithPosInc(dp * (-et) - el);
        dest_ptr2 = d.shallowCopyWithPosInc(dp * (h) - el);
        linesize = el + er + w;

        CommonUtils.genericCopy(src_ptr1, 0, dest_ptr1, dp, et, linesize);
        CommonUtils.genericCopy(src_ptr2, 0, dest_ptr2, dp, eb, linesize);

    }

    public static void vp8_copy_and_extend_frame(YV12buffer src, YV12buffer dst) {
        int et = dst.border;
        int el = dst.border;
        int eb = dst.border + dst.y_height - src.y_height;
        int er = dst.border + dst.y_width - src.y_width;

// detect nv12 colorspace
        int chroma_step = src.u_buffer.pointerDiff(src.v_buffer) == 1 ? 2 : 1;

        copy_and_extend_plane(src.y_buffer, src.y_stride, dst.y_buffer, dst.y_stride, src.y_height, src.y_width, et, el,
                eb, er, 1);

        et = dst.border >> 1;
        el = dst.border >> 1;
        eb = (dst.border >> 1) + dst.uv_height - src.uv_height;
        er = (dst.border >> 1) + dst.uv_width - src.uv_width;

        copy_and_extend_plane(src.u_buffer, src.uv_stride, dst.u_buffer, dst.uv_stride, src.uv_height, src.uv_width, et,
                el, eb, er, chroma_step);

        copy_and_extend_plane(src.v_buffer, src.uv_stride, dst.v_buffer, dst.uv_stride, src.uv_height, src.uv_width, et,
                el, eb, er, chroma_step);
    }

    public static void vp8_copy_and_extend_frame_with_rect(YV12buffer src, YV12buffer dst, int srcy, int srcx, int srch,
            int srcw) {
        int et = dst.border;
        int el = dst.border;
        int eb = dst.border + dst.y_height - src.y_height;
        int er = dst.border + dst.y_width - src.y_width;
        int src_y_offset = srcy * src.y_stride + srcx;
        int dst_y_offset = srcy * dst.y_stride + srcx;
        int src_uv_offset = ((srcy * src.uv_stride) >> 1) + (srcx >> 1);
        int dst_uv_offset = ((srcy * dst.uv_stride) >> 1) + (srcx >> 1);
// detect nv12 colorspace
        int chroma_step = src.u_buffer.pointerDiff(src.v_buffer) == 1 ? 2 : 1;

        /* If the side is not touching the bounder then don't extend. */
        if (srcy != 0)
            et = 0;
        if (srcx != 0)
            el = 0;
        if (srcy + srch != src.y_height)
            eb = 0;
        if (srcx + srcw != src.y_width)
            er = 0;

        copy_and_extend_plane(src.y_buffer.shallowCopyWithPosInc(src_y_offset), src.y_stride,
                dst.y_buffer.shallowCopyWithPosInc(dst_y_offset), dst.y_stride, srch, srcw, et, el, eb, er, 1);

        et = (et + 1) >> 1;
        el = (el + 1) >> 1;
        eb = (eb + 1) >> 1;
        er = (er + 1) >> 1;
        srch = (srch + 1) >> 1;
        srcw = (srcw + 1) >> 1;

        copy_and_extend_plane(src.u_buffer.shallowCopyWithPosInc(src_uv_offset), src.uv_stride,
                dst.u_buffer.shallowCopyWithPosInc(dst_uv_offset), dst.uv_stride, srch, srcw, et, el, eb, er,
                chroma_step);

        copy_and_extend_plane(src.v_buffer.shallowCopyWithPosInc(src_uv_offset), src.uv_stride,
                dst.v_buffer.shallowCopyWithPosInc(dst_uv_offset), dst.uv_stride, srch, srcw, et, el, eb, er,
                chroma_step);
    }

}
