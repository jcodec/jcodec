package org.jcodec.codecs.h264;

import static org.junit.Assert.assertArrayEquals;

import org.jcodec.codecs.h264.decode.Intra4x4PredictionBuilder;
import org.junit.Test;

public class TestIntra4x4PredictionBuilder {

    @Test
    public void testDC() throws Exception {

        int[] pred = new int[256];

        int[] top = new int[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        int[] left = new int[] { 9, 10, 11, 12 };

        Intra4x4PredictionBuilder.predictDC(pred, true, true, left, top, 0, 0, 0);

        assertArrayEquals(inMB(new int[] { 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7 }), pred);
    }

    private int[] inMB(int[] is) {
        int[] result = new int[256];
        result[0] = is[0];
        result[1] = is[1];
        result[2] = is[2];
        result[3] = is[3];
        result[16] = is[4];
        result[17] = is[5];
        result[18] = is[6];
        result[19] = is[7];
        result[32] = is[8];
        result[33] = is[9];
        result[34] = is[10];
        result[35] = is[11];
        result[48] = is[12];
        result[49] = is[13];
        result[50] = is[14];
        result[51] = is[15];
        
        return result;
    }

    @Test
    public void testVertical() throws Exception {

        int[] pred = new int[256];

        int[] top = new int[] { 1, 2, 3, 4, 5, 6, 7, 8 };
//        int[] left = new int[] { 13, 9, 10, 11, 12 };

        Intra4x4PredictionBuilder.predictVertical(pred, true, top, 0, 0, 0);

        assertArrayEquals(inMB(new int[] { 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4 }), pred);
    }

    @Test
    public void testHorizontal() throws Exception {

        int[] pred = new int[256];

//        int[] top = new int[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        int[] left = new int[] { 9, 10, 11, 12 };
        Intra4x4PredictionBuilder.predictHorizontal(pred, true, left, 0, 0, 0);

        assertArrayEquals(inMB(new int[] { 9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11, 12, 12, 12, 12 }), pred);
    }

    @Test
    public void testDiagonalDownLeft() throws Exception {

        int[] pred = new int[256];

        int[] top = new int[] { 209, 212, 218, 222, 216, 219, 225, 229 };
        Intra4x4PredictionBuilder.predictDiagonalDownLeft(pred, true, true, top, 0, 0, 0);

        assertArrayEquals(inMB(new int[] { 213, 218, 220, 218, 218, 220, 218, 220, 220, 218, 220, 225, 218, 220, 225, 228 }),
                pred);
    }

    @Test
    public void testDiagonalDownRight() throws Exception {

        int[] pred = new int[256];

        int[] top =  { 183, 196, 170, 131, 0, 0, 0, 0 };
        int[] left =  { 207, 207, 207, 207 };
        int[] tl = {196,0, 0, 0};
        Intra4x4PredictionBuilder.predictDiagonalDownRight(pred, true, true, left, top, tl, 0, 0, 0);

        assertArrayEquals(inMB(new int[] { 196, 190, 186, 167, 204, 196, 190, 186, 207, 204, 196, 190, 207, 207, 204, 196 }),
                pred);
    }
    
    @Test
    public void testDiagonalDownRight1() throws Exception {

        int[] pred = new int[256];

        int[] top = new int[] { 236, 236, 236, 236, 0, 0, 0, 0 };
        int[] left = new int[] { 233, 233, 233, 233 };
        int[] tl = {226, 0, 0, 0};
        Intra4x4PredictionBuilder.predictDiagonalDownRight(pred, true, true, left, top, tl, 0, 0, 0);

        assertArrayEquals(inMB(new int[] { 230, 234, 236, 236, 231, 230, 234, 236, 233, 231, 230, 234, 233, 233, 231, 230 }),
                pred);
    }

    @Test
    public void testVerticalRight() throws Exception {

        int[] pred = new int[256];

        int[] top = { 207, 201, 197, 175, 0, 0, 0, 0 };
        int[] left = { 208, 176, 129, 122 };
        int[] tl = {206, 0, 0, 0};
        
        Intra4x4PredictionBuilder.predictVerticalRight(pred, true, true, left, top, tl, 0, 0, 0);

        assertArrayEquals(inMB(new int[] { 207, 204, 199, 186, 207, 205, 202, 193, 200, 207, 204, 199, 172, 207, 205, 202 }),
                pred);
    }

    @Test
    public void testHorizontalDown() throws Exception {

        int[] pred = new int[256];

        int[] top = { 209, 157, 114, 118 };
        int[] left = { 197, 198, 202, 205 };
        int[] tl = {204, 0, 0, 0};
        Intra4x4PredictionBuilder.predictHorizontalDown(pred, true, true, left, top, tl, 0, 0, 0);

        assertArrayEquals(inMB(new int[] { 201, 204, 195, 159, 198, 199, 201, 204, 200, 199, 198, 199, 204, 202, 200, 199 }),
                pred);
    }

    @Test
    public void testVerticalLeft() throws Exception {

        int[] pred = new int[256];

        int[] top = new int[] { 215, 201, 173, 159, 137, 141, 150, 155 };
        Intra4x4PredictionBuilder.predictVerticalLeft(pred, true, true, top, 0, 0, 0);

        assertArrayEquals(inMB(new int[] { 208, 187, 166, 148, 198, 177, 157, 144, 187, 166, 148, 139, 177, 157, 144, 142 }),
                pred);
    }

    @Test
    public void testHorizontalUp() throws Exception {

        int[] pred = new int[256];

        int[] left = new int[] { 175, 180, 216, 221 };
        Intra4x4PredictionBuilder.predictHorizontalUp(pred, true, left, 0, 0, 0);

        assertArrayEquals(inMB(new int[] { 178, 188, 198, 208, 198, 208, 219, 220, 219, 220, 221, 221, 221, 221, 221, 221 }),
                pred);
    }

}
