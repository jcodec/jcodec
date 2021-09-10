package org.jcodec.codecs.vpx.vp8.data;

import java.util.EnumSet;

import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.codecs.vpx.vp8.CommonUtils;
import org.jcodec.codecs.vpx.vp8.VP8Exception;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.enums.CodecError;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.common.model.Picture;

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
public class YV12buffer {
    public static final int VP8BORDERINPIXELS = 32;

    public int y_width;
    public int y_height;
    public int y_crop_width;
    public int y_crop_height;
    public int y_stride;

    public int uv_width;
    public int uv_height;
    public int uv_crop_width;
    public int uv_crop_height;
    public int uv_stride;

    public FullAccessIntArrPointer y_buffer; // uint8
    public FullAccessIntArrPointer u_buffer; // uint8
    public FullAccessIntArrPointer v_buffer; // uint8

    public FullAccessIntArrPointer buffer_alloc; // uint8
    public int border;
    int frame_size;
    int subsampling_x;
    int subsampling_y;
    /* unsigned */ int bit_depth;
    int render_width;
    int render_height;

    int corrupted;
    public EnumSet<MVReferenceFrame> flags;

    private YV12buffer() { // allows shallow copy
    }

    YV12buffer shallowCopy() {
        YV12buffer sh = new YV12buffer();
        sh.y_width = this.y_width;
        sh.y_height = this.y_height;
        sh.y_crop_width = this.y_crop_width;
        sh.y_crop_height = this.y_crop_height;
        sh.y_stride = this.y_stride;

        sh.uv_width = this.uv_width;
        sh.uv_height = this.uv_height;
        sh.uv_crop_width = this.uv_crop_width;
        sh.uv_crop_height = this.uv_crop_height;
        sh.uv_stride = this.uv_stride;

        sh.y_buffer = this.y_buffer.shallowCopy();
        sh.u_buffer = this.u_buffer.shallowCopy();
        sh.v_buffer = this.v_buffer.shallowCopy();

        sh.buffer_alloc = this.buffer_alloc.shallowCopy();

        sh.border = this.border;
        sh.frame_size = this.frame_size;
        sh.subsampling_x = this.subsampling_x;
        sh.subsampling_y = this.subsampling_y;
        sh.bit_depth = this.bit_depth;
        sh.render_width = this.render_width;
        sh.render_height = this.render_height;
        sh.corrupted = this.corrupted;
        sh.flags = this.flags == null ? EnumSet.noneOf(MVReferenceFrame.class) : EnumSet.copyOf(this.flags);

        return sh;
    }

    private static void extend_plane(FullAccessIntArrPointer src, int src_stride, int width, int height, int extend_top,
            int extend_left, int extend_bottom, int extend_right) {
        /* copy the left and right most columns out */

        for (int i = 0; i < height; ++i) {
            final int strideshift = i * src_stride;
            src.memset(strideshift - extend_left, src.getRel(strideshift), extend_left);
            src.memset(strideshift + width, src.getRel(strideshift + width - 1), extend_right);
        }

        /*
         * Now copy the top and bottom lines into each line of the respective borders
         */

        final int linesize = extend_left + extend_right + width;
        for (int i = 0; i < extend_top; ++i) {
            src.memcopyin(src_stride * (i - extend_top) - extend_left, src, -extend_left, linesize);
        }

        final int bottomrowstart = src_stride * (height - 1) - extend_left;
        for (int i = 0; i < extend_bottom; ++i) {
            src.memcopyin(src_stride * (i + height) - extend_left, src, bottomrowstart, linesize);
        }
    }

    public void extend_frame_borders() {
        final int uv_border = border / 2;

        assert (border % 2 == 0);
        assert (y_height - y_crop_height < 16);
        assert (y_width - y_crop_width < 16);
        assert (y_height - y_crop_height >= 0);
        assert (y_width - y_crop_width >= 0);

        YV12buffer.extend_plane(y_buffer, y_stride, y_crop_width, y_crop_height, border, border,
                border + y_height - y_crop_height, border + y_width - y_crop_width);

        YV12buffer.extend_plane(u_buffer, uv_stride, uv_crop_width, uv_crop_height, uv_border, uv_border,
                uv_border + uv_height - uv_crop_height, uv_border + uv_width - uv_crop_width);

        YV12buffer.extend_plane(v_buffer, uv_stride, uv_crop_width, uv_crop_height, uv_border, uv_border,
                uv_border + uv_height - uv_crop_height, uv_border + uv_width - uv_crop_width);
    }

    private static void leftcolhelper(FullAccessIntArrPointer target, int st, int h) {
        for (int i = 0; i < h; ++i) {
            target.setRel(st * i - 1, (short) 129);
        }
    }

    private static void toplinehelper(FullAccessIntArrPointer target, int stride, int width) {
        target.memset(-1 - stride, (short) 127, width + 5);
    }

    private void vp8_setup_intra_recon_left_col() {
        leftcolhelper(y_buffer, y_stride, y_height);
        leftcolhelper(u_buffer, uv_stride, uv_height);
        leftcolhelper(v_buffer, uv_stride, uv_height);
    }

    private void vp8_setup_intra_recon_top_line() {
        toplinehelper(y_buffer, y_stride, y_width);
        toplinehelper(u_buffer, uv_stride, uv_width);
        toplinehelper(v_buffer, uv_stride, uv_width);
    }

    public void vp8_setup_intra_recon() {
        /* set up frame new frame for intra coded blocks */
        vp8_setup_intra_recon_top_line();
        vp8_setup_intra_recon_left_col();
    }

    private static void copyBufPart(final FullAccessIntArrPointer src_ptr, final FullAccessIntArrPointer dst_ptr,
            int width, int height, int src_stride, int dst_stride) {
        CommonUtils.genericCopy(src_ptr, src_stride, dst_ptr, dst_stride, height, width);
    }

    public static void copyY(YV12buffer src_ybc, YV12buffer dst_ybc) {
        YV12buffer.copyBufPart(src_ybc.y_buffer, dst_ybc.y_buffer, src_ybc.y_width, src_ybc.y_height, src_ybc.y_stride,
                dst_ybc.y_stride);
    }

    // Copies the source image into the destination image and updates the
    // destination's UMV borders.
    // Note: The frames are assumed to be identical in size.
    public static void copyFrame(YV12buffer src_ybc, YV12buffer dst_ybc) {
        YV12buffer.copyY(src_ybc, dst_ybc);
        YV12buffer.copyBufPart(src_ybc.u_buffer, dst_ybc.u_buffer, src_ybc.uv_width, src_ybc.uv_height, src_ybc.uv_stride,
                dst_ybc.uv_stride);
        YV12buffer.copyBufPart(src_ybc.v_buffer, dst_ybc.v_buffer, src_ybc.uv_width, src_ybc.uv_height, src_ybc.uv_stride,
                dst_ybc.uv_stride);
        dst_ybc.extend_frame_borders();
    }

    public YV12buffer(Picture img) {
        byte[][] jcodec_raw_data = img.getData();
        FullAccessIntArrPointer img_data = new FullAccessIntArrPointer(
                jcodec_raw_data[0].length + jcodec_raw_data[1].length + jcodec_raw_data[2].length);
        for (byte[] planeData : jcodec_raw_data) {
            for (byte pix : planeData) {
                img_data.setAndInc((short) (pix + VP8Encoder.INT_TO_BYTE_OFFSET));
            }
        }
        img_data.rewind();

        final int y_w = img.getWidth();
        final int y_h = img.getHeight();
        final int uv_w = img.getPlaneWidth(1);
        final int uv_h = img.getPlaneHeight(1);
        y_buffer = img_data;
        u_buffer = img_data.shallowCopyWithPosInc(jcodec_raw_data[0].length);
        v_buffer = u_buffer.shallowCopyWithPosInc(jcodec_raw_data[1].length);

        y_crop_width = y_w;
        y_crop_height = y_h;
        y_width = y_w;
        y_height = y_h;
        uv_crop_width = uv_w;
        uv_crop_height = uv_h;
        uv_width = uv_w;
        uv_height = uv_h;

        y_stride = img.getWidth();
        uv_stride = img.getPlaneWidth(1);

        border = 0;
    }

    public YV12buffer(int width, int height) {
        this(width, height, VP8BORDERINPIXELS);
    }

    public YV12buffer(int width, int height, int border) {
        y_width = (width + 15) & ~15;
        y_height = (height + 15) & ~15;
        y_stride = ((y_width + 2 * border) + 31) & ~31;
        y_crop_width = width;
        y_crop_height = height;

        /**
         * There is currently a bunch of code which assumes uv_stride == y_stride/2, so
         * enforce this here.
         */
        uv_width = y_width >> 1;
        uv_height = y_height >> 1;
        uv_stride = y_stride >> 1;
        uv_crop_width = (width + 1) / 2;
        uv_crop_height = (height + 1) / 2;

        this.border = border;

        int yplane_size = (y_height + 2 * border) * y_stride;
        int uvplane_size = (uv_height + border) * uv_stride;
        frame_size = yplane_size + 2 * uvplane_size;
        /*
         * Only support allocating buffers that have a border that's a multiple of 32.
         * The border restriction is required to get 16-byte alignment of the start of
         * the chroma rows without introducing an arbitrary gap between planes, which
         * would break the semantics of things like vpx_img_set_rect().
         */
        if ((border & 0x1f) != 0)
            VP8Exception.vp8_internal_error(CodecError.UNSUP_FEATURE,
                    "Border size (%d) is not suitable for VP8", border);

        buffer_alloc = new FullAccessIntArrPointer(frame_size);

        y_buffer = buffer_alloc.shallowCopyWithPosInc((border * y_stride) + border);
        u_buffer = buffer_alloc.shallowCopyWithPosInc(yplane_size + (border / 2 * uv_stride) + border / 2);
        v_buffer = buffer_alloc
                .shallowCopyWithPosInc(yplane_size + uvplane_size + (border / 2 * uv_stride) + border / 2);

        corrupted = 0; /* assume not currupted by errors */

    }

}
