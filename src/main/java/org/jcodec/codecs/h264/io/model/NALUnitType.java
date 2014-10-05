package org.jcodec.codecs.h264.io.model;

import java.util.EnumSet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * NAL unit type
 * 
 * @author Jay Codec
 * 
 */
public enum NALUnitType {
    NON_IDR_SLICE(1, "non IDR slice"), SLICE_PART_A(2, "slice part a"), SLICE_PART_B(3, "slice part b"), SLICE_PART_C(
            4, "slice part c"), IDR_SLICE(5, "idr slice"), SEI(6, "sei"), SPS(7, "sequence parameter set"), PPS(8,
            "picture parameter set"), ACC_UNIT_DELIM(9, "access unit delimiter"), END_OF_SEQ(10, "end of sequence"), END_OF_STREAM(
            11, "end of stream"), FILLER_DATA(12, "filter data"), SEQ_PAR_SET_EXT(13,
            "sequence parameter set extension"), AUX_SLICE(19, "auxilary slice");

    private final static NALUnitType[] lut;
    static {
        lut = new NALUnitType[256];
        for (NALUnitType nalUnitType : EnumSet.allOf(NALUnitType.class)) {
            lut[nalUnitType.value] = nalUnitType;
        }
    }

    private final int value;
    private final String name;

    private NALUnitType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public static NALUnitType fromValue(int value) {
        return value < lut.length ? lut[value] : null;
    }
}