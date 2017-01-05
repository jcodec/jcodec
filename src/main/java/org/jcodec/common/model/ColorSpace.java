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
    public final static ColorSpace BGR = new ColorSpace("BGR", 3, _000, _000, _000, false);
    public final static ColorSpace RGB = new ColorSpace("RGB", 3, _000, _000, _000, false);
    public final static ColorSpace YUV420 = new ColorSpace("YUV420", 3, _012, _011, _011, true);
    public final static ColorSpace YUV420J = new ColorSpace("YUV420J", 3, _012, _011, _011, true);
    public final static ColorSpace YUV422 = new ColorSpace("YUV422", 3, _012, _011, _000, true);
    public final static ColorSpace YUV422J = new ColorSpace("YUV422J", 3, _012, _011, _000, true);
    public final static ColorSpace YUV444 = new ColorSpace("YUV444", 3, _012, _000, _000, true);
    public final static ColorSpace YUV444J = new ColorSpace("YUV444J", 3, _012, _000, _000, true);
    public final static ColorSpace YUV422_10 = new ColorSpace("YUV422_10", 3, _012, _011, _000, true);
    public final static ColorSpace GREY = new ColorSpace("GREY", 1, new int[] { 0 }, new int[] { 0 }, new int[] { 0 }, true);
    public final static ColorSpace MONO = new ColorSpace("MONO", 1, _000, _000, _000, true);
    public final static ColorSpace YUV444_10 = new ColorSpace("YUV444_10", 3, _012, _000, _000, true);

    public static final int MAX_PLANES = 4;

    public int nComp;

    public int[] compPlane;

    public int[] compWidth;

    public int[] compHeight;
    
    public boolean planar;
    
    private String _name;

    private ColorSpace(String name, int nComp, int[] compPlane, int[] compWidth, int[] compHeight, boolean planar) {
        this._name = name;
        this.nComp = nComp;
        this.compPlane = compPlane;
        this.compWidth = compWidth;
        this.compHeight = compHeight;
        this.planar = planar;
    }

    @Override
    public String toString() {
        return _name;
    }
}
