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
public enum BlockEnum {
    BLOCK_16X8, BLOCK_8X16, BLOCK_8X8, BLOCK_4X4, BLOCK_16X16;

    public static EnumSet<BlockEnum> allBut1616 = EnumSet.range(BLOCK_16X8, BLOCK_4X4);
}
