package org.jcodec.codecs.h264;

import static org.junit.Assert.assertArrayEquals;

import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.junit.Test;

public class TestCoeffTransformer {

    @Test
    public void testDCT() {

        int[] coeffs = { 2560, -4160, -2816, -1920, 960, -1200, 320, 0, 768, -640, -512, 320, 0, 0, 0, 0 };

        int[] expected = { -86, 92, 136, 126, -80, 72, 66, 84, -81, 71, 47, 45, -89, 91, 97, 49 };

        CoeffTransformer.idct4x4(coeffs);

        assertArrayEquals(expected, coeffs);
    }

    @Test
    public void testHadamard() {

        int[] coeffs = { 61, -11, -13, -14, 9, -4, -2, 2, -12, 6, 6, 4, 8, 1, -2, 2 };

        int[] expected = { 41, 75, 79, 69, 15, 89, 97, 95, 23, 97, 85, 83, 13, 47, 23, 45 };

        CoeffTransformer.invDC4x4(coeffs);

        assertArrayEquals(expected, coeffs);

    }

    @Test
    public void testRescaleAfterIHadamard4x4() {
        int[] coeffs = { 41, 75, 79, 69, 15, 89, 97, 95, 23, 97, 85, 83, 13, 47, 23, 45 };
        int[] expected = { 2624, 4800, 5056, 4416, 960, 5696, 6208, 6080, 1472, 6208, 5440, 5312, 832, 3008, 1472, 2880 };

        CoeffTransformer.dequantizeDC4x4(coeffs, 28);

        assertArrayEquals(expected, coeffs);
    }

    @Test
    public void testRescaleBeforeIDCT4x4() throws Exception {
        int[] coeffs = { 10, -13, -11, -6, 3, -3, 1, 0, 3, -2, -2, 1, 0, 0, 0, 0 };
        int[] expected = { 2560, -4160, -2816, -1920, 960, -1200, 320, 0, 768, -640, -512, 320, 0, 0, 0, 0 };

        CoeffTransformer.dequantizeAC(coeffs, 28);

        assertArrayEquals(expected, coeffs);
    }

    @Test
    public void testReorder() {
        int[] coeffs = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
        int[] expected = { 0, 1, 5, 6, 2, 4, 7, 12, 3, 8, 11, 13, 9, 10, 14, 15 };

        int[] reordered = CoeffTransformer.unzigzagAC(coeffs);

        assertArrayEquals(expected, reordered);
    }

    @Test
    public void testQuantizeDC4x4() {
        int[] dc = { 1765, -3340, 1340, -3432, 1435, -4056, 6743, -2454, 3432, -1234, 7643, -1432, 1654, -5643, 2345,
                -1786 };
        int[] expected = { 11, -21, 8, -21, 9, -25, 42, -15, 21, -8, 48, -9, 10, -35, 14, -11 };

        CoeffTransformer.quantizeDC4x4(dc, 30);

        assertArrayEquals(expected, dc);
    }
    
    @Test
    public void testQuantizeAC() {
        int[] dc = { 0, 573, 232, 123, 402, 123, 754, 74 , 12, 15, 13, 12 , 14, 15, 43, 57 };
        int[] expected = { 0, 4, 3, 1, 3, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        CoeffTransformer.quantizeAC(dc, 30);

        assertArrayEquals(expected, dc);
    }
    
    @Test
    public void testQuantizeDC2x2() {
        int[] dc = { 1765, -3340, 1435, -4056 };
        int[] expected = { 11, -21, 9, -25 };

        CoeffTransformer.quantizeDC2x2(dc, 30);

        assertArrayEquals(expected, dc);
    }
}
