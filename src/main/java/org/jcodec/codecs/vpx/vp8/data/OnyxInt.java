package org.jcodec.codecs.vpx.vp8.data;

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
public class OnyxInt {
    public static final short MINQ = 0;
    public static final short MAXQ = 127;
    public static final short QINDEX_RANGE = (MAXQ + 1);
    public static final int MIN_THRESHMULT = 32;
    public static final int MAX_THRESHMULT = 512;
    public static final int GF_ZEROMV_ZBIN_BOOST = 12;
    public static final int LF_ZEROMV_ZBIN_BOOST = 6;
    public static final int MV_ZBIN_BOOST = 4;
    public static final int ZBIN_OQ_MAX = 192;
    public static final int TICKS_PER_SEC = 10000000;
    public static final int MAX_LAG_BUFFERS = 25;
    public static final int MIN_GF_INTERVAL = 4;
    public static final int DEFAULT_GF_INTERVAL = 7;
    public static final int KEY_FRAME_CONTEXT = 5;

}
