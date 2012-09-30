package org.jcodec.codecs.h264.decode.imgop;

import static org.jcodec.common.model.ColorSpace.YUV420;

import org.jcodec.codecs.h264.decode.model.DecodedMBlock;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Converts a collection of macroblocks into picture
 * 
 * @author Jay Codec
 * 
 */
public class Flattener {
    private Rect lumaRect;
    private Rect chromaRect;
    private int picWidthInMbs;
    private int picHeightInMbs;

    public Flattener(SeqParameterSet sps) {
        calcCropRectangle(sps);
        this.picWidthInMbs = sps.pic_width_in_mbs_minus1 + 1;
        this.picHeightInMbs = sps.pic_height_in_map_units_minus1 + 1;
    }

    private void calcCropRectangle(SeqParameterSet sps) {
        int picWidth = (sps.pic_width_in_mbs_minus1 + 1) << 4;
        int picHeight = (sps.pic_height_in_map_units_minus1 + 1) << 4;
        if (sps.frame_cropping_flag) {
            int sX = sps.frame_crop_left_offset << 1;
            int sY = sps.frame_crop_top_offset << 1;
            int w = picWidth - (sps.frame_crop_right_offset << 1);
            int h = picHeight - (sps.frame_crop_bottom_offset << 1);
            lumaRect = new Rect(sX, sY, w, h);
            chromaRect = new Rect(sX >> 1, sY >> 1, w >> 1, h >> 1);
        } else {
            lumaRect = new Rect(0, 0, picWidth, picHeight);
            chromaRect = new Rect(0, 0, picWidth >> 1, picHeight >> 1);
        }
    }

    public Picture flattern(DecodedMBlock[] decoded) {

        ClippingPlane luma = new ClippingPlane(lumaRect);
        ClippingPlane cb = new ClippingPlane(chromaRect);
        ClippingPlane cr = new ClippingPlane(chromaRect);

        for (int mbY = 0; mbY < picHeightInMbs; mbY++) {
            for (int mbX = 0; mbX < picWidthInMbs; mbX++) {
                DecodedMBlock mb = decoded[mbY * picWidthInMbs + mbX];
                luma.putBlock(mbX << 4, mbY << 4, mb.getLuma(), 16, 16);
                cb.putBlock(mbX << 3, mbY << 3, mb.getChroma().getCb(), 8, 8);
                cr.putBlock(mbX << 3, mbY << 3, mb.getChroma().getCr(), 8, 8);
            }
        }

        return new Picture(lumaRect.getWidth(), lumaRect.getHeight(), new int[][] { luma.getBuf(), cb.getBuf(),
                cr.getBuf() }, YUV420);
    }

    public static Picture flattern(DecodedMBlock[] decoded, int picWidthInMbs, int picHeightInMbs) {
        int[] luma = new int[decoded.length << 8];
        int stride = picWidthInMbs << 4;

        int[] cb = new int[decoded.length << 6];
        int[] cr = new int[decoded.length << 6];
        int strideChroma = picWidthInMbs << 3;

        for (int mbY = 0; mbY < picHeightInMbs; mbY++) {
            for (int mbX = 0; mbX < picWidthInMbs; mbX++) {
                int mbAddr = mbY * picWidthInMbs + mbX;

                int dOff = 0;
                for (int i = 0; i < 16; i++) {
                    System.arraycopy(decoded[mbAddr].getLuma(), dOff, luma, (mbY * 16 + i) * stride + mbX * 16, 16);
                    dOff += 16;
                }
                for (int i = 0; i < 8; i++) {
                    System.arraycopy(decoded[mbAddr].getChroma().getCb(), i * 8, cb, (mbY * 8 + i) * strideChroma + mbX
                            * 8, 8);
                }
                for (int i = 0; i < 8; i++) {
                    System.arraycopy(decoded[mbAddr].getChroma().getCr(), i * 8, cr, (mbY * 8 + i) * strideChroma + mbX
                            * 8, 8);
                }
            }
        }

        return new Picture(picWidthInMbs * 16, picHeightInMbs * 16, new int[][] { luma, cb, cr }, YUV420);
    }
}