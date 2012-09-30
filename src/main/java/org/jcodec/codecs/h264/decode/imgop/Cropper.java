package org.jcodec.codecs.h264.decode.imgop;

import static org.jcodec.common.model.ColorSpace.YUV420;

import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Crops an image to it's output dimensions
 * 
 * @author Jay Codec
 * 
 */
public class Cropper {
    private Rect lumaRect;
    private Rect chromaRect;

    public Cropper(SeqParameterSet sps) {
        calcCropRectangle(sps);
    }

    private void calcCropRectangle(SeqParameterSet sps) {
        int picWidth = (sps.pic_width_in_mbs_minus1 + 1) << 4;
        int picHeight = (sps.pic_height_in_map_units_minus1 + 1) << 4;
        if (sps.frame_cropping_flag) {
            int sX = sps.frame_crop_left_offset << 1;
            int sY = sps.frame_crop_top_offset << 1;
            int w = picWidth - (sps.frame_crop_right_offset << 1) - sX;
            int h = picHeight - (sps.frame_crop_bottom_offset << 1) - sY;
            lumaRect = new Rect(sX, sY, w, h);
            chromaRect = new Rect(sX >> 1, sY >> 1, w >> 1, h >> 1);
        } else {
            lumaRect = null;
            chromaRect = null;
        }
    }

    public Picture crop(Picture src) {

        if (lumaRect != null) {
            int[] luma = new int[lumaRect.getWidth() * lumaRect.getHeight()];
            int[] cb = new int[chromaRect.getWidth() * chromaRect.getHeight()];
            int[] cr = new int[chromaRect.getWidth() * chromaRect.getHeight()];

            copy(src.getData()[0], src.getWidth(), luma, lumaRect);
            copy(src.getData()[1], src.getWidth() >> 1, cb, chromaRect);
            copy(src.getData()[2], src.getWidth() >> 1, cr, chromaRect);

            return new Picture(lumaRect.getWidth(), lumaRect.getHeight(), new int[][] { luma, cb, cr }, YUV420);
        }

        return src;
    }

    private void copy(int[] src, int srcStride, int[] dst, Rect dstRect) {
        int dstStride = dstRect.getWidth();
        int srcOff = dstRect.getX() + dstRect.getY() * srcStride;
        int dstOff = 0;
        for (int j = 0; j < dstRect.getHeight(); j++) {
            System.arraycopy(src, srcOff, dst, dstOff, dstStride);
            srcOff += srcStride;
            dstOff += dstStride;
        }
    }
}
