package org.jcodec.common.model;

/**
 * 
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public final class ColorSpace {
    private static final int[] _000 = new int[] { 0, 0, 0 };
    private static final int[] _011 = new int[] { 0, 1, 1 };
    private static final int[] _012 = new int[] { 0, 1, 2 };
    public final static ColorSpace BGR = new ColorSpace(3, _000, _000, _000);
    public final static ColorSpace RGB = new ColorSpace(3, _000, _000, _000);
    public final static ColorSpace YUV420 = new ColorSpace(3, _012, _011, _011);
    public final static ColorSpace YUV420J = new ColorSpace(3, _012, _011, _011);
    public final static ColorSpace YUV422 = new ColorSpace(3, _012, _011, _000);
    public final static ColorSpace YUV422J = new ColorSpace(3, _012, _011, _000);
    public final static ColorSpace YUV444 = new ColorSpace(3, _012, _000, _000);
    public final static ColorSpace YUV444J = new ColorSpace(3, _012, _000, _000);
    public final static ColorSpace YUV422_10 = new ColorSpace(3, _012, _011, _000);
    public final static ColorSpace GREY = new ColorSpace(1, new int[] { 0 }, new int[] { 0 }, new int[] { 0 });
    public final static ColorSpace MONO = new ColorSpace(1, _000, _000, _000);
    public final static ColorSpace YUV444_10 = new ColorSpace(3, _012, _000, _000);

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
