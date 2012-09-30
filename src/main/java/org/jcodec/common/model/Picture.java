package org.jcodec.common.model;

import static org.jcodec.common.model.ColorSpace.MAX_PLANES;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A YUV picture
 * 
 * @author The JCodec project
 * 
 */
public class Picture {
    private ColorSpace color;

    private int width;
    private int height;

    private int[][] data;

    public Picture(int width, int height, int[][] data, ColorSpace color) {
        this.width = width;
        this.height = height;
        this.data = data;
        this.color = color;
    }

    public Picture(Picture other) {
        this(other.width, other.height, other.data, other.color);
    }

    public static Picture create(int width, int height, ColorSpace colorSpace) {
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

        return new Picture(width, height, data, colorSpace);
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

    public int getPlaneWidth(int plane) {
        return width >> color.compWidth[plane];
    }
}
