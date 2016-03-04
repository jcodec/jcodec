package org.jcodec.common.model;
import static java.lang.System.arraycopy;
import static org.jcodec.common.model.ColorSpace.MAX_PLANES;

import java.lang.IllegalArgumentException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A YUV picture
 * 
 * @author The JCodec project
 * @deprecated Use org.jcodec.common.model.Picture8Bit and related APIs
 * 
 */
public class Picture {
    private ColorSpace color;

    private int width;
    private int height;

    private int[][] data;

    private Rect crop;

    private int bitDepth;

    public static Picture createPicture(int width, int height, int[][] data, ColorSpace color) {
        return new Picture(width, height, data, color, 8, new Rect(0, 0, width, height));
    }

    public static Picture createPictureWithDepth(int width, int height, int[][] data, ColorSpace color, int bitDepth) {
        return new Picture(width, height, data, color, bitDepth, new Rect(0, 0, width, height));
    }

    public static Picture createPictureCropped(int width, int height, int[][] data, ColorSpace color, Rect crop) {
        return new Picture(width, height, data, color, 8, crop);
    }

    public Picture(int width, int height, int[][] data, ColorSpace color, int bitDepth, Rect crop) {
        this.width = width;
        this.height = height;
        this.data = data;
        this.color = color;
        this.crop = crop;
        this.bitDepth = bitDepth;
    }

    public static Picture clonePicture(Picture other) {
        return new Picture(other.width, other.height, other.data, other.color, other.bitDepth, other.crop);
    }
    
    public static Picture create(int width, int height, ColorSpace colorSpace) {
        return doCreate(width, height, colorSpace, 8, null);
    }

    public static Picture createWithDepth(int width, int height, ColorSpace colorSpace, int bitDepth) {
        return doCreate(width, height, colorSpace, bitDepth, null);
    }

    public static Picture createCropped(int width, int height, ColorSpace colorSpace, Rect crop) {
        return doCreate(width, height, colorSpace, 8, crop);
    }

    public static Picture doCreate(int width, int height, ColorSpace colorSpace, int bitDepth, Rect crop) {
        int[] planeSizes = new int[MAX_PLANES];
        for (int i = 0; i < colorSpace.nComp; i++) {
            planeSizes[colorSpace.compPlane[i]] += (width >> colorSpace.compWidth[i])
                    * (height >> colorSpace.compHeight[i]);
        }
        int nPlanes = 0;
        for (int i = 0; i < MAX_PLANES; i++)
            nPlanes += planeSizes[i] != 0 ? 1 : 0;

        int[][] data = new int[nPlanes][];
        for (int i = 0, plane = 0; i < MAX_PLANES; i++) {
            if (planeSizes[i] != 0) {
                data[plane++] = new int[planeSizes[i]];
            }
        }

        return new Picture(width, height, data, colorSpace, 8, crop);
    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getPlaneData(int plane) {
        return data[plane];
    }

    public ColorSpace getColor() {
        return color;
    }

    public int[][] getData() {
        return data;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public Rect getCrop() {
        return crop;
    }

    public int getPlaneWidth(int plane) {
        return width >> color.compWidth[plane];
    }

    public int getPlaneHeight(int plane) {
        return height >> color.compHeight[plane];
    }

    public boolean compatible(Picture src) {
        return src.color == color && src.width == width && src.height == height && src.bitDepth == bitDepth;
    }

    public Picture createCompatible() {
        return Picture.create(width, height, color);
    }

    public void copyFrom(Picture src) {
        if (!compatible(src))
            throw new IllegalArgumentException("Can not copy to incompatible picture");
        for (int plane = 0; plane < color.nComp; plane++) {
            if (data[plane] == null)
                continue;
            arraycopy(src.data[plane], 0, data[plane], 0,
                    (width >> color.compWidth[plane]) * (height >> color.compHeight[plane]));
        }
    }

    public Picture cropped() {
        if (crop == null
                || (crop.getX() == 0 && crop.getY() == 0 && crop.getWidth() == width && crop.getHeight() == height))
            return this;
        Picture result = Picture.createWithDepth(crop.getWidth(), crop.getHeight(), color, bitDepth);

        for (int plane = 0; plane < color.nComp; plane++) {
            if (data[plane] == null)
                continue;
            cropSub(data[plane], crop.getX() >> color.compWidth[plane], crop.getY() >> color.compHeight[plane],
                    crop.getWidth() >> color.compWidth[plane], crop.getHeight() >> color.compHeight[plane],
                    width >> color.compWidth[plane], result.data[plane]);
        }

        return result;
    }

    private void cropSub(int[] src, int x, int y, int w, int h, int srcStride, int[] tgt) {
        int srcOff = y * srcStride + x, dstOff = 0;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++)
                tgt[dstOff + j] = src[srcOff + j];

            srcOff += srcStride;
            dstOff += w;
        }
    }

    public void setCrop(Rect crop) {
        this.crop = crop;
    }

    public int getCroppedWidth() {
        return crop == null ? width : crop.getWidth();
    }

    public int getCroppedHeight() {
        return crop == null ? height : crop.getHeight();
    }

    public void setBitDepth(int bitDepth) {
        this.bitDepth = bitDepth;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Picture))
            return false;
        Picture other = (Picture) obj;

        if (other.getCroppedWidth() != getCroppedWidth() || other.getCroppedHeight() != getCroppedHeight()
                || other.getColor() != color)
            return false;

        for (int i = 0; i < getData().length; i++)
            if (!planeEquals(other, i))
                return false;
        return true;
    }

    private boolean planeEquals(Picture other, int plane) {
        int cw = color.compWidth[plane];
        int ch = color.compHeight[plane];
        int offA = other.getCrop() == null ? 0
                : ((other.getCrop().getX() >> cw) + (other.getCrop().getY() >> ch) * (other.getWidth() >> cw));
        int offB = crop == null ? 0 : ((crop.getX() >> cw) + (crop.getY() >> ch) * (width >> cw));

        int[] planeData = other.getPlaneData(plane);
        for (int i = 0; i < getCroppedHeight() >> ch; i++, offA += (other.getWidth() >> cw), offB += (width >> cw)) {
            for (int j = 0; j < getCroppedWidth() >> cw; j++) {
                if (planeData[offA + j] != data[plane][offB + j])
                    return false;
            }
        }
        return true;
    }
}