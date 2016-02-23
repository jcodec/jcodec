package org.jcodec.codecs.h264;

import static org.jcodec.common.ArrayUtil.padLeft;
import static org.jcodec.common.ArrayUtil.toByteArrayShifted;
import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;

import org.jcodec.codecs.h264.decode.Intra8x8PredictionBuilder;
import org.jcodec.common.tools.MathUtil;
import org.junit.Test;

public class Intra8x8PredictionBuilderTest {

    private static int[] testResidual = new int[64];
    private static int[] emptyResidual = new int[64];

    static {
        for (int i = 0; i < 64; i++) {
            testResidual[i] = (i << 2) - 128;
        }
    }

    private byte[] addResidual(byte[] pred, int[] residual) {
        byte[] result = new byte[pred.length];
        for (int i = 0; i < pred.length; i++)
            result[i] = (byte) MathUtil.clip(pred[i] + residual[i], -128, 127);
        return result;
    }

    private byte[] inMB(byte[] is, int blkX, int blkY) {
        byte[] result = new byte[256];
        int off = (blkY << 4) + blkX;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++)
                result[off + (i << 4) + j] = is[(i << 3) + j];
        }

        return result;
    }

    @Test
    public void testDC() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 64, 65, 66, 67, 68, 69, 70,
                71);
        byte[] left = toByteArrayShifted(16, 17, 18, 19, 20, 21, 22, 23);
        byte[] topLeft = toByteArrayShifted(24);

        byte[] expected = new byte[64];
        Arrays.fill(expected, (byte) (13 - 128));

        new Intra8x8PredictionBuilder().predictDC(emptyResidual, true, true, true, true, topLeft, left, top, 0, 0, 0,
                pred);
        assertArrayEquals(inMB(expected, 0, 0), pred);

        for (int blkY = 0; blkY < 16; blkY += 8) {
            for (int blkX = 0; blkX < 16; blkX += 8) {
                Arrays.fill(pred, (byte) 0);
                new Intra8x8PredictionBuilder().predictDC(testResidual, true, true, true, true,
                        padLeft(topLeft, blkY >> 2), padLeft(left, blkY), padLeft(top, blkX), 0, blkX, blkY, pred);
                assertArrayEquals(inMB(addResidual(expected, testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testVertical() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 64, 65, 66, 67, 68, 69, 70,
                71);
        byte[] topLeft = toByteArrayShifted(24, 0, 0, 0);

        byte[] expected = toByteArrayShifted(7, 2, 3, 4, 5, 6, 7, 8, 7, 2, 3, 4, 5, 6, 7, 8, 7, 2, 3, 4, 5, 6, 7, 8, 7,
                2, 3, 4, 5, 6, 7, 8, 7, 2, 3, 4, 5, 6, 7, 8, 7, 2, 3, 4, 5, 6, 7, 8, 7, 2, 3, 4, 5, 6, 7, 8, 7, 2, 3,
                4, 5, 6, 7, 8);

        new Intra8x8PredictionBuilder().predictVertical(emptyResidual, true, true, topLeft, top, 0, 0, 0, pred);
        assertArrayEquals(inMB(expected, 0, 0), pred);

        for (int blkY = 0; blkY < 16; blkY += 8) {
            for (int blkX = 0; blkX < 16; blkX += 8) {
                Arrays.fill(pred, (byte) 0);
                new Intra8x8PredictionBuilder().predictVertical(testResidual, true, true, padLeft(topLeft, blkY >> 2),
                        padLeft(top, blkX), 0, blkX, blkY, pred);
                assertArrayEquals(inMB(addResidual(expected, testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testHorizontal() throws Exception {

        byte[] pred = new byte[256];

        byte[] left = toByteArrayShifted(16, 17, 18, 19, 20, 21, 22, 23);
        byte[] topLeft = toByteArrayShifted(24, 0, 0, 0);

        byte[] expected = toByteArrayShifted(18, 18, 18, 18, 18, 18, 18, 18, 17, 17, 17, 17, 17, 17, 17, 17, 18, 18,
                18, 18, 18, 18, 18, 18, 19, 19, 19, 19, 19, 19, 19, 19, 20, 20, 20, 20, 20, 20, 20, 20, 21, 21, 21, 21,
                21, 21, 21, 21, 22, 22, 22, 22, 22, 22, 22, 22, 23, 23, 23, 23, 23, 23, 23, 23);

        new Intra8x8PredictionBuilder().predictHorizontal(emptyResidual, true, topLeft, left, 0, 0, 0, pred);
        assertArrayEquals(inMB(expected, 0, 0), pred);

        for (int blkY = 0; blkY < 16; blkY += 8) {
            for (int blkX = 0; blkX < 16; blkX += 8) {
                Arrays.fill(pred, (byte) 0);
                new Intra8x8PredictionBuilder().predictHorizontal(testResidual, true, padLeft(topLeft, blkY >> 2),
                        padLeft(left, blkY), 0, blkX, blkY, pred);
                assertArrayEquals(inMB(addResidual(expected, testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testDiagonalDownLeft() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160);
        byte[] topLeft = toByteArrayShifted(170, 180, 190, 200);

        byte[] expected = toByteArrayShifted(31, 30, 40, 50, 60, 70, 80, 90, 30, 40, 50, 60, 70, 80, 90, 100, 40, 50,
                60, 70, 80, 90, 100, 110, 50, 60, 70, 80, 90, 100, 110, 120, 60, 70, 80, 90, 100, 110, 120, 130, 70,
                80, 90, 100, 110, 120, 130, 140, 80, 90, 100, 110, 120, 130, 140, 150, 90, 100, 110, 120, 130, 140,
                150, 156);

        new Intra8x8PredictionBuilder().predictDiagonalDownLeft(emptyResidual, true, true, true, topLeft, top, 0, 0, 0,
                pred);
        assertArrayEquals(inMB(expected, 0, 0), pred);

        for (int blkY = 0; blkY < 16; blkY += 8) {
            for (int blkX = 0; blkX < 16; blkX += 8) {
                Arrays.fill(pred, (byte) 0);
                new Intra8x8PredictionBuilder().predictDiagonalDownLeft(testResidual, true, true, true,
                        padLeft(topLeft, blkY >> 2), padLeft(top, blkX), 0, blkX, blkY, pred);
                assertArrayEquals(inMB(addResidual(expected, testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testDiagonalDownRight() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160);
        byte[] left = toByteArrayShifted(210, 215, 220, 225, 230, 235, 240, 245);
        byte[] tl = toByteArrayShifted(170, 180, 190, 200);

        byte[] expected = toByteArrayShifted(134, 67, 31, 30, 40, 50, 60, 70, 189, 134, 67, 31, 30, 40, 50, 60, 213,
                189, 134, 67, 31, 30, 40, 50, 220, 213, 189, 134, 67, 31, 30, 40, 225, 220, 213, 189, 134, 67, 31, 30,
                230, 225, 220, 213, 189, 134, 67, 31, 235, 230, 225, 220, 213, 189, 134, 67, 240, 235, 230, 225, 220,
                213, 189, 134);

        new Intra8x8PredictionBuilder().predictDiagonalDownRight(emptyResidual, true, tl, left, top, 0, 0, 0, pred);
        assertArrayEquals(inMB(expected, 0, 0), pred);

        for (int blkY = 0; blkY < 16; blkY += 8) {
            for (int blkX = 0; blkX < 16; blkX += 8) {
                Arrays.fill(pred, (byte) 0);
                new Intra8x8PredictionBuilder().predictDiagonalDownRight(testResidual, true, padLeft(tl, blkY >> 2),
                        padLeft(left, blkY), padLeft(top, blkX), 0, blkX, blkY, pred);
                assertArrayEquals(inMB(addResidual(expected, testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testVerticalRight() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160);
        byte[] left = toByteArrayShifted(210, 215, 220, 225, 230, 235, 240, 245);
        byte[] tl = toByteArrayShifted(170, 180, 190, 200);

        byte[] expected = toByteArrayShifted(97, 37, 25, 35, 45, 55, 65, 75, 134, 67, 31, 30, 40, 50, 60, 70, 189, 97,
                37, 25, 35, 45, 55, 65, 213, 134, 67, 31, 30, 40, 50, 60, 220, 189, 97, 37, 25, 35, 45, 55, 225, 213,
                134, 67, 31, 30, 40, 50, 230, 220, 189, 97, 37, 25, 35, 45, 235, 225, 213, 134, 67, 31, 30, 40);

        new Intra8x8PredictionBuilder().predictVerticalRight(emptyResidual, true, tl, left, top, 0, 0, 0, pred);
        assertArrayEquals(inMB(expected, 0, 0), pred);

        for (int blkY = 0; blkY < 16; blkY += 8) {
            for (int blkX = 0; blkX < 16; blkX += 8) {
                Arrays.fill(pred, (byte) 0);
                new Intra8x8PredictionBuilder().predictVerticalRight(testResidual, true, padLeft(tl, blkY >> 2),
                        padLeft(left, blkY), padLeft(top, blkX), 0, blkX, blkY, pred);
                assertArrayEquals(inMB(addResidual(expected, testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testHorizontalDown() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160);
        byte[] left = toByteArrayShifted(210, 215, 220, 225, 230, 235, 240, 245);
        byte[] tl = toByteArrayShifted(170, 180, 190, 200);

        byte[] expected = toByteArrayShifted(171, 134, 67, 31, 30, 40, 50, 60, 208, 189, 171, 134, 67, 31, 30, 40, 218,
                213, 208, 189, 171, 134, 67, 31, 223, 220, 218, 213, 208, 189, 171, 134, 228, 225, 223, 220, 218, 213,
                208, 189, 233, 230, 228, 225, 223, 220, 218, 213, 238, 235, 233, 230, 228, 225, 223, 220, 242, 240,
                238, 235, 233, 230, 228, 225);

        new Intra8x8PredictionBuilder().predictHorizontalDown(emptyResidual, true, tl, left, top, 0, 0, 0, pred);
        assertArrayEquals(inMB(expected, 0, 0), pred);

        for (int blkY = 0; blkY < 16; blkY += 8) {
            for (int blkX = 0; blkX < 16; blkX += 8) {
                Arrays.fill(pred, (byte) 0);
                new Intra8x8PredictionBuilder().predictHorizontalDown(testResidual, true, padLeft(tl, blkY >> 2),
                        padLeft(left, blkY), padLeft(top, blkX), 0, blkX, blkY, pred);
                assertArrayEquals(inMB(addResidual(expected, testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testVerticalLeft() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160);
        byte[] tl = toByteArrayShifted(170, 180, 190, 200);

        byte[] expected = toByteArrayShifted(37, 25, 35, 45, 55, 65, 75, 85, 31, 30, 40, 50, 60, 70, 80, 90, 25, 35,
                45, 55, 65, 75, 85, 95, 30, 40, 50, 60, 70, 80, 90, 100, 35, 45, 55, 65, 75, 85, 95, 105, 40, 50, 60,
                70, 80, 90, 100, 110, 45, 55, 65, 75, 85, 95, 105, 115, 50, 60, 70, 80, 90, 100, 110, 120);

        new Intra8x8PredictionBuilder().predictVerticalLeft(emptyResidual, true, true, tl, top, 0, 0, 0, pred);
        assertArrayEquals(inMB(expected, 0, 0), pred);

        for (int blkY = 0; blkY < 16; blkY += 8) {
            for (int blkX = 0; blkX < 16; blkX += 8) {
                Arrays.fill(pred, (byte) 0);
                new Intra8x8PredictionBuilder().predictVerticalLeft(testResidual, true, true, padLeft(tl, blkY >> 2),
                        padLeft(top, blkX), 0, blkX, blkY, pred);
                assertArrayEquals(inMB(addResidual(expected, testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testHorizontalUp() throws Exception {

        byte[] pred = new byte[256];

        byte[] left = toByteArrayShifted(210, 215, 220, 225, 230, 235, 240, 245);
        byte[] tl = toByteArrayShifted(170, 180, 190, 200);

        byte[] expected = toByteArrayShifted(208, 213, 218, 220, 223, 225, 228, 230, 218, 220, 223, 225, 228, 230, 233,
                235, 223, 225, 228, 230, 233, 235, 238, 240, 228, 230, 233, 235, 238, 240, 242, 243, 233, 235, 238,
                240, 242, 243, 244, 244, 238, 240, 242, 243, 244, 244, 244, 244, 242, 243, 244, 244, 244, 244, 244,
                244, 244, 244, 244, 244, 244, 244, 244, 244);

        new Intra8x8PredictionBuilder().predictHorizontalUp(emptyResidual, true, tl, left, 0, 0, 0, pred);
        assertArrayEquals(inMB(expected, 0, 0), pred);

        for (int blkY = 0; blkY < 16; blkY += 8) {
            for (int blkX = 0; blkX < 16; blkX += 8) {
                Arrays.fill(pred, (byte) 0);
                new Intra8x8PredictionBuilder().predictHorizontalUp(testResidual, true, padLeft(tl, blkY >> 2),
                        padLeft(left, blkY), 0, blkX, blkY, pred);
                assertArrayEquals(inMB(addResidual(expected, testResidual), blkX, blkY), pred);
            }
        }
    }

}
