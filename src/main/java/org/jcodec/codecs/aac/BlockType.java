package org.jcodec.codecs.aac;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * AAC block types
 * 
 * @author The JCodec project
 * 
 */
public enum BlockType {
    TYPE_SCE(0), TYPE_CPE(1), TYPE_CCE(2), TYPE_LFE(3), TYPE_DSE(4), TYPE_PCE(5), TYPE_FIL(6), TYPE_END(7);

    private int code;

    private BlockType(int code) {
        this.code = code;
    }

    public static BlockType fromCode(long readNBit) {
        return null;
    }

    public int getCode() {
        return code;
    }
}
