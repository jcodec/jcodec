package org.jcodec.common.model;

import static org.jcodec.common.model.ColorSpace.MAX_PLANES;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * The data is -128 shifted, so 0 is represented by -128 and 255 is represented
 * by +127
 * 
 * @author The JCodec project
 * 
 */
public class Picture8Bit {
    private ColorSpace color;

    private int width;
    private int height;

    private byte[][] data;

    private Rect crop;

    public Picture8Bit(int width, int height, byte[][] data, ColorSpace color) {
        this(width, height, data, color, new Rect(0, 0, width, height));
    }

    public Picture8Bit(int width, int height, byte[][] data, ColorSpace color, Rect crop) {
        this.width = width;
        this.height = height;
        this.data = data;
        this.color = color;
        this.crop = crop;
    }

    public Picture8Bit(Picture8Bit other) {
        this(other.width, other.height, other.data, other.color, other.crop);
    }

    public static Picture8Bit create(int width, int height, ColorSpace colorSpace) {
        return create(width, height, colorSpace, null);
    }

    public static Picture8Bit create(int width, int height, ColorSpace colorSpace, Rect crop) {
        int[] planeSizes = new int[MAX_PLANES];
        for (int i = 0; i < colorSpace.nComp; i++) {
            planeSizes[colorSpace.compPlane[i]] += (width >> colorSpace.compWidth[i])
                    * (height >> colorSpace.compHeight[i]);
        }
        int nPlanes = 0;
        for (int i = 0; i < MAX_PLANES; i++)
            nPlanes += planeSizes[i] != 0 ? 1 : 0;

        byte[][] data = new byte[nPlanes][];
        for (int i = 0, plane = 0; i < MAX_PLANES; i++) {
            if (planeSizes[i] != 0) {
                data[plane++] = new byte[planeSizes[i]];
            }
        }

        return new Picture8Bit(width, height, data, colorSpace, crop);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getPlaneData(int plane) {
        return data[plane];
    }

    public ColorSpace getColor() {
        return color;
    }

    public byte[][] getData() {
        return data;
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

    public boolean compatible(Picture8Bit src) {
        return src.color == color && src.width == width && src.height == height;
    }

    public Picture8Bit createCompatible() {
        return Picture8Bit.create(width, height, color);
    }

    public void copyFrom(Picture8Bit src) {
        if (!compatible(src))
            throw new IllegalArgumentException("Can not copy to incompatible picture");
        for (int plane = 0; plane < color.nComp; plane++) {
            if (data[plane] == null)
                continue;
            System.arraycopy(src.data[plane], 0, data[plane], 0, (width >> color.compWidth[plane])
                    * (height >> color.compHeight[plane]));
        }
    }

    public Picture8Bit cropped() {
        if (crop == null
                || (crop.getX() == 0 && crop.getY() == 0 && crop.getWidth() == width && crop.getHeight() == height))
            return this;
        Picture8Bit result = Picture8Bit.create(crop.getWidth(), crop.getHeight(), color);

        for (int plane = 0; plane < color.nComp; plane++) {
            if (data[plane] == null)
                continue;
            cropSub(data[plane], crop.getX() >> color.compWidth[plane], crop.getY() >> color.compHeight[plane],
                    crop.getWidth() >> color.compWidth[plane], crop.getHeight() >> color.compHeight[plane],
                    width >> color.compWidth[plane], result.data[plane]);
        }

        return result;
    }

    private void cropSub(byte[] src, int x, int y, int w, int h, int srcStride, byte[] tgt) {
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

    public static Picture8Bit fromPicture(Picture pic) {
        Picture8Bit create = Picture8Bit.create(pic.getWidth(), pic.getHeight(), pic.getColor(), pic.getCrop());

        for (int i = 0; i < pic.getData().length; i++) {
            for (int j = 0; j < pic.getData()[i].length; j++) {
                create.getData()[i][j] = (byte) (((pic.getData()[i][j] << 8) >> pic.getBitDepth() ) - 128);
            }
        }

        return create;
    }
    
    public Picture toPicture(int bitDepth) {
        Picture create = Picture.create(width, height, color, bitDepth, crop);

        return toPictureInternal(bitDepth, create);
    }

    public Picture toPicture(int bitDepth, int[][] buffer) {
        Picture create = new Picture(width, height, buffer, color, bitDepth, crop);

        return toPictureInternal(bitDepth, create);
    }
    
    private Picture toPictureInternal(int bitDepth, Picture create) {
        for (int i = 0; i < data.length; i++) {
            int planeSize = getPlaneWidth(i)*getPlaneHeight(i);
            for (int j = 0; j < planeSize; j++) {
                create.getData()[i][j] = ((data[i][j] + 128) << bitDepth) >> 8;
            }
        }

        return create;
    }
}