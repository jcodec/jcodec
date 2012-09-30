package org.jcodec.codecs.aac.blocks;

import java.util.EnumSet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public enum RawDataBlockType {
    TYPE_SCE, TYPE_CPE, TYPE_CCE, TYPE_LFE, TYPE_DSE, TYPE_PCE, TYPE_FIL, TYPE_END;

    public static RawDataBlockType fromOrdinal(int ordinal) {
        for (RawDataBlockType val : EnumSet.allOf(RawDataBlockType.class)) {
            if (val.ordinal() == ordinal)
                return val;
        }
        return null;
    }
}
