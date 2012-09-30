package org.jcodec.codecs.h264;

import java.io.ByteArrayInputStream;

import org.jcodec.codecs.h264.io.model.IntraNxNPrediction;
import org.jcodec.codecs.h264.io.read.IntraPredictionReader;
import org.jcodec.codecs.util.BinUtil;
import org.jcodec.common.io.BitstreamReader;
import org.jcodec.common.io.InBits;
import org.junit.Test;

public class TestIntraPrediction extends JAVCTestCase {

    @Test
    public void testI4x4PredModeRead1() throws Exception {

        String bits = "10001101110001011101111000000000000011100101011101111";
        InBits reader = new BitstreamReader(new ByteArrayInputStream(BinUtil.binaryStringToBytes(bits)));

        IntraNxNPrediction actual = IntraPredictionReader.readPrediction4x4(reader, null, null, false, false);
        int[] expected = new int[] { 2, 1, 2, 8, 1, 8, 8, 8, 0, 1, 0, 8, 3, 3, 8, 8 };
        assertArrayEquals(expected, actual.getLumaModes());
    }
    
    @Test
    public void testI4x4PredModeRead2() throws Exception {

        String bits = "0001 1 1 1 1 1 1 0100 1 1 1 1 1 0000 1 01001";
        InBits reader = new BitstreamReader(new ByteArrayInputStream(BinUtil.binaryStringToBytes(bits)));

        IntraNxNPrediction actual = IntraPredictionReader.readPrediction4x4(reader, null, null, true, true);
        int[] expected = new int[] { 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1, 1, 1, 0, 1, 5 };
        assertArrayEquals(expected, actual.getLumaModes());
    }
}
