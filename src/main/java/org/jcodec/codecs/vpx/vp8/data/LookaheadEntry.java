package org.jcodec.codecs.vpx.vp8.data;

import java.util.EnumSet;

import org.jcodec.codecs.vpx.vp8.enums.FrameTypeFlags;

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
public class LookaheadEntry {
    public YV12buffer img;
    public long ts_start;
    public long ts_end;
    public EnumSet<FrameTypeFlags> flags; // uint
}
