package org.jcodec.common.model;

import static java.lang.System.arraycopy;
import static org.jcodec.common.model.ColorSpace.MAX_PLANES;

import java.lang.IllegalArgumentException;
import java.util.Arrays;

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
public class Picture {
    private ColorSpace color;

    private int width;
    private int height;

    private byte[][] data;

    private Rect crop;

    public static Picture createPicture(int width, int height, byte[][] data, ColorSpace color) {
        return new Picture(width, height, data, color, new Rect(0, 0, width, height));
    }
    
    public Picture(int width, int height, byte[][] data, ColorSpace color, Rect crop) {
        this.width = width;
        this.height = height;
        this.data = data;
        this.color = color;
        this.crop = crop;

        if (color != null) {
            for (int i = 0; i < color.nComp; i++) {
                int mask = 0xff >> (8 - color.compWidth[i]);
                if ((width & mask) != 0)
                    throw new IllegalArgumentException("Component " + i + " width should be a multiple of "
                            + (1 << color.compWidth[i]) + " for colorspace: " + color);
                if (crop != null && (crop.getWidth() & mask) != 0)
                    throw new IllegalArgumentException("Component " + i + " cropped width should be a multiple of "
                            + (1 << color.compWidth[i]) + " for colorspace: " + color);
                mask = 0xff >> (8 - color.compHeight[i]);
                if ((height & mask) != 0)
                    throw new IllegalArgumentException("Component " + i + " height should be a multiple of "
                            + (1 << color.compHeight[i]) + " for colorspace: " + color);
                if (crop != null && (crop.getHeight() & mask) != 0)
                    throw new IllegalArgumentException("Component " + i + " cropped height should be a multiple of "
                            + (1 << color.compHeight[i]) + " for colorspace: " + color);
            }
        }
    }

    public static Picture copyPicture(Picture other) {
        return new Picture(other.width, other.height, other.data, other.color, other.crop);
    }
    
    public static Picture create(int width, int height, ColorSpace colorSpace) {
        return createCropped(width, height, colorSpace, null);
    }

    public static Picture createCropped(int width, int height, ColorSpace colorSpace, Rect crop) {
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

        return new Picture(width, height, data, colorSpace, crop);
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

    public boolean compatible(Picture src) {
        return src.color == color && src.width == width && src.height == height;
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
            arraycopy(src.data[plane], 0, data[plane], 0, (width >> color.compWidth[plane])
                    * (height >> color.compHeight[plane]));
        }
    }

    /**
     * Creates a cropped clone of this picture.
     * 
     * @return
     */
    public Picture cloneCropped() {
        if (cropNeeded()) {
            return cropped();
        } else {
            Picture clone = createCompatible();
            clone.copyFrom(this);
            return clone;
        }
    }

    public Picture cropped() {
        if (!cropNeeded())
            return this;
        Picture result = Picture.create(crop.getWidth(), crop.getHeight(), color);

        if(color.planar) {
            for (int plane = 0; plane < data.length; plane++) {
                if (data[plane] == null)
                    continue;
                cropSub(data[plane], crop.getX() >> color.compWidth[plane], crop.getY() >> color.compHeight[plane],
                        crop.getWidth() >> color.compWidth[plane], crop.getHeight() >> color.compHeight[plane],
                        width >> color.compWidth[plane], crop.getWidth() >> color.compWidth[plane], result.data[plane]);
            }
        } else {
           cropSub(data[0], crop.getX(), crop.getY(), crop.getWidth(), 
            crop.getHeight(), width * color.nComp, crop.getWidth() * color.nComp, result.data[0]);
        }

        return result;
    }

    protected boolean cropNeeded() {
        return crop != null
                && (crop.getX() != 0 || crop.getY() != 0 || crop.getWidth() != width || crop.getHeight() != height);
    }

    private void cropSub(byte[] src, int x, int y, int w, int h, int srcStride, int dstStride, byte[] tgt) {
        int srcOff = y * srcStride + x, dstOff = 0;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < dstStride; j++)
                tgt[dstOff + j] = src[srcOff + j];

            srcOff += srcStride;
            dstOff += dstStride;
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

    public static Picture fromPictureHiBD(PictureHiBD pic) {
        Picture create = Picture.createCropped(pic.getWidth(), pic.getHeight(), pic.getColor(), pic.getCrop());

        for (int i = 0; i < Math.min(pic.getData().length, create.getData().length); i++) {
            for (int j = 0; j < Math.min(pic.getData()[i].length, create.getData()[i].length); j++) {
                create.getData()[i][j] = (byte) (((pic.getData()[i][j] << 8) >> pic.getBitDepth()) - 128);
            }
        }

        return create;
    }

    public PictureHiBD toPictureHiBD(int bitDepth) {
        PictureHiBD create = PictureHiBD.doCreate(width, height, color, bitDepth, crop);

        return toPictureHiBDInternal(bitDepth, create);
    }

    public PictureHiBD toPictureHiBDWithBuffer(int bitDepth, int[][] buffer) {
        PictureHiBD create = new PictureHiBD(width, height, buffer, color, bitDepth, crop);

        return toPictureHiBDInternal(bitDepth, create);
    }

    private PictureHiBD toPictureHiBDInternal(int bitDepth, PictureHiBD create) {
        for (int i = 0; i < data.length; i++) {
            int planeSize = getPlaneWidth(i) * getPlaneHeight(i);
            for (int j = 0; j < planeSize; j++) {
                create.getData()[i][j] = ((data[i][j] + 128) << bitDepth) >> 8;
            }
        }

        return create;
    }

    public void fill(int val) {
        for (int i = 0; i < data.length; i++) {
            Arrays.fill(data[i], (byte) val);
        }
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
        int offA = other.getCrop() == null ? 0 : ((other.getCrop().getX() >> cw) + (other.getCrop().getY() >> ch)
                * (other.getWidth() >> cw));
        int offB = crop == null ? 0 : ((crop.getX() >> cw) + (crop.getY() >> ch) * (width >> cw));

        byte[] planeData = other.getPlaneData(plane);
        for (int i = 0; i < getCroppedHeight() >> ch; i++, offA += (other.getWidth() >> cw), offB += (width >> cw)) {
            for (int j = 0; j < getCroppedWidth() >> cw; j++) {
                if (planeData[offA + j] != data[plane][offB + j])
                    return false;
            }
        }
        return true;
    }

    public int getStartX() {
        return crop == null ? 0 : crop.getX();
    }

    public int getStartY() {
        return crop == null ? 0 : crop.getY();
    }
}