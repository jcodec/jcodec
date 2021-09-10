package org.jcodec.codecs.vpx.vp8.enums;

import java.util.EnumSet;

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
public enum MVReferenceFrame {
    INTRA_FRAME, LAST_FRAME, GOLDEN_FRAME, ALTREF_FRAME;

    public static EnumSet<MVReferenceFrame> validFrames = EnumSet.allOf(MVReferenceFrame.class);
    public static EnumSet<MVReferenceFrame> interFrames = EnumSet.of(LAST_FRAME, GOLDEN_FRAME, ALTREF_FRAME);

    public static final int count = validFrames.size();
}
