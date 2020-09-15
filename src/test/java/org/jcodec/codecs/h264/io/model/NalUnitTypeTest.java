package org.jcodec.codecs.h264.io.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NalUnitTypeTest
{

    @Test
    public void testAllValidNalTypesAreSupported()
    {
        assertEquals("type not supported as expected", NALUnitType.NON_IDR_SLICE, NALUnitType.fromValue(1));
        assertEquals("type not supported as expected", NALUnitType.SLICE_PART_A, NALUnitType.fromValue(2));
        assertEquals("type not supported as expected", NALUnitType.SLICE_PART_B, NALUnitType.fromValue(3));
        assertEquals("type not supported as expected", NALUnitType.SLICE_PART_C, NALUnitType.fromValue(4));
        assertEquals("type not supported as expected", NALUnitType.IDR_SLICE, NALUnitType.fromValue(5));
        assertEquals("type not supported as expected", NALUnitType.SEI, NALUnitType.fromValue(6));
        assertEquals("type not supported as expected", NALUnitType.SPS, NALUnitType.fromValue(7));
        assertEquals("type not supported as expected", NALUnitType.PPS, NALUnitType.fromValue(8));
        assertEquals("type not supported as expected", NALUnitType.ACC_UNIT_DELIM, NALUnitType.fromValue(9));
        assertEquals("type not supported as expected", NALUnitType.END_OF_SEQ, NALUnitType.fromValue(10));
        assertEquals("type not supported as expected", NALUnitType.END_OF_STREAM, NALUnitType.fromValue(11));
        assertEquals("type not supported as expected", NALUnitType.FILLER_DATA, NALUnitType.fromValue(12));
        assertEquals("type not supported as expected", NALUnitType.SEQ_PAR_SET_EXT, NALUnitType.fromValue(13));
        assertEquals("type not supported as expected", NALUnitType.AUX_SLICE, NALUnitType.fromValue(19));
        assertEquals("type not supported as expected", NALUnitType.FU_A, NALUnitType.fromValue(28));
    }


    @Test
    public void testReportsDataCorrectly()
    {
        assertEquals("name not as expected", NALUnitType.FU_A.getName(), "fragmented unit a");
        assertEquals("string not as expected", NALUnitType.FU_A.toString(), "FU_A");
        assertEquals("value not as expected", NALUnitType.FU_A.getValue(), 28);
    }


    @Test
    public void testIdentifiesBadTypeCorrectly()
    {
        assertEquals("not as expected", NALUnitType.fromValue(1), NALUnitType.NON_IDR_SLICE);
        assertEquals("not as expected", NALUnitType.fromValue(100), null);
        assertEquals("not as expected", NALUnitType.fromValue(-1), null);
    }

}
