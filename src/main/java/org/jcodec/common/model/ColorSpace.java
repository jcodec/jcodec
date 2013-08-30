package org.jcodec.common.model;

/**
 * 
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public enum ColorSpace {
    RGB(3, new int[] { 0, 0, 0 }, new int[] { 0, 0, 0 }, new int[] { 0, 0, 0 }),

    YUV420(3, new int[] { 0, 1, 2 }, new int[] { 0, 1, 1 }, new int[] { 0, 1, 1 }),

    YUV420J(3, new int[] { 0, 1, 2 }, new int[] { 0, 1, 1 }, new int[] { 0, 1, 1 }),

    YUV422(3, new int[] { 0, 1, 2 }, new int[] { 0, 1, 1 }, new int[] { 0, 0, 0 }),
    
    YUV422J(3, new int[] { 0, 1, 2 }, new int[] { 0, 1, 1 }, new int[] { 0, 0, 0 }),

    YUV444(3, new int[] { 0, 1, 2 }, new int[] { 0, 0, 0 }, new int[] { 0, 0, 0 }),
    
    YUV444J(3, new int[] { 0, 1, 2 }, new int[] { 0, 0, 0 }, new int[] { 0, 0, 0 }),

    YUV422_10(3, new int[] { 0, 1, 2 }, new int[] { 0, 1, 1 }, new int[] { 0, 0, 0 }),

    GREY(1, new int[] { 0 }, new int[] { 0 }, new int[] { 0 }),

    MONO(1, new int[] { 0, 0, 0 }, new int[] { 0, 0, 0 }, new int[] { 0, 0, 0 }),

    YUV444_10(3, new int[] { 0, 1, 2 }, new int[] { 0, 0, 0 }, new int[] { 0, 0, 0 });

    public static final int MAX_PLANES = 4;

    public int nComp;

    public int[] compPlane;

    public int[] compWidth;

    public int[] compHeight;

    private ColorSpace(int nComp, int[] compPlane, int[] compWidth, int[] compHeight) {
        this.nComp = nComp;
        this.compPlane = compPlane;
        this.compWidth = compWidth;
        this.compHeight = compHeight;
    }
}
