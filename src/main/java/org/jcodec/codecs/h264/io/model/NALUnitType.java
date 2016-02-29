package org.jcodec.codecs.h264.io.model;

import java.util.EnumSet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * NAL unit type
 * 
 * @author The JCodec project
 * 
 */
public final class NALUnitType {
    public final static NALUnitType NON_IDR_SLICE = new NALUnitType(1, "non IDR slice");
    public final static NALUnitType SLICE_PART_A = new NALUnitType(2, "slice part a");
    public final static NALUnitType SLICE_PART_B = new NALUnitType(3, "slice part b");
    public final static NALUnitType SLICE_PART_C = new NALUnitType(4, "slice part c");
    public final static NALUnitType IDR_SLICE = new NALUnitType(5, "idr slice");
    public final static NALUnitType SEI = new NALUnitType(6, "sei");
    public final static NALUnitType SPS = new NALUnitType(7, "sequence parameter set");
    public final static NALUnitType PPS = new NALUnitType(8, "picture parameter set");
    public final static NALUnitType ACC_UNIT_DELIM = new NALUnitType(9, "access unit delimiter");
    public final static NALUnitType END_OF_SEQ = new NALUnitType(10, "end of sequence");
    public final static NALUnitType END_OF_STREAM = new NALUnitType(11, "end of stream");
    public final static NALUnitType FILLER_DATA = new NALUnitType(12, "filler data");
    public final static NALUnitType SEQ_PAR_SET_EXT = new NALUnitType(13, "sequence parameter set extension");
    public final static NALUnitType AUX_SLICE = new NALUnitType(19, "auxilary slice");

    private final static NALUnitType[] lut;
    private final static NALUnitType[] _values;

    static {
        _values = new NALUnitType[] { NON_IDR_SLICE, SLICE_PART_A, SLICE_PART_B, SLICE_PART_C, IDR_SLICE, SEI, SPS, PPS,
                ACC_UNIT_DELIM, END_OF_SEQ, END_OF_STREAM, FILLER_DATA, SEQ_PAR_SET_EXT, AUX_SLICE };
        lut = new NALUnitType[256];
        for (int i = 0; i < _values.length; i++) {
            NALUnitType nalUnitType = _values[i];
            lut[nalUnitType.value] = nalUnitType;
        }
    }

    private final int value;
    private final String displayName;

    private NALUnitType(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getName() {
        return displayName;
    }

    public int getValue() {
        return value;
    }

    public static NALUnitType fromValue(int value) {
        return value < lut.length ? lut[value] : null;
    }
}