package org.jcodec.codecs.h264.io.model;

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
    public final static NALUnitType NON_IDR_SLICE = new NALUnitType(1, "NON_IDR_SLICE", "non IDR slice");
    public final static NALUnitType SLICE_PART_A = new NALUnitType(2, "SLICE_PART_A", "slice part a");
    public final static NALUnitType SLICE_PART_B = new NALUnitType(3, "SLICE_PART_B", "slice part b");
    public final static NALUnitType SLICE_PART_C = new NALUnitType(4, "SLICE_PART_C", "slice part c");
    public final static NALUnitType IDR_SLICE = new NALUnitType(5, "IDR_SLICE", "idr slice");
    public final static NALUnitType SEI = new NALUnitType(6, "SEI", "sei");
    public final static NALUnitType SPS = new NALUnitType(7, "SPS", "sequence parameter set");
    public final static NALUnitType PPS = new NALUnitType(8, "PPS", "picture parameter set");
    public final static NALUnitType ACC_UNIT_DELIM = new NALUnitType(9, "ACC_UNIT_DELIM", "access unit delimiter");
    public final static NALUnitType END_OF_SEQ = new NALUnitType(10, "END_OF_SEQ", "end of sequence");
    public final static NALUnitType END_OF_STREAM = new NALUnitType(11, "END_OF_STREAM", "end of stream");
    public final static NALUnitType FILLER_DATA = new NALUnitType(12, "FILLER_DATA", "filler data");
    public final static NALUnitType SEQ_PAR_SET_EXT = new NALUnitType(13, "SEQ_PAR_SET_EXT", "sequence parameter set extension");
    public final static NALUnitType AUX_SLICE = new NALUnitType(19, "AUX_SLICE", "auxilary slice");

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
    private String _name;

    private NALUnitType(int value, String name, String displayName) {
        this.value = value;
        this._name = name;
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
    
    @Override
    public String toString() {
        return _name;
    }
}