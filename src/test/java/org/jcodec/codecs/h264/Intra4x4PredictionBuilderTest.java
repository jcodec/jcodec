package org.jcodec.codecs.h264;
import static org.jcodec.common.ArrayUtil.padLeft;
import static org.jcodec.common.ArrayUtil.toByteArrayShifted;
import static org.junit.Assert.assertArrayEquals;

import org.jcodec.codecs.h264.decode.Intra4x4PredictionBuilder;
import org.jcodec.common.tools.MathUtil;
import org.junit.Test;

import java.util.Arrays;

public class Intra4x4PredictionBuilderTest {
    private static int[] emptyResidual = new int[16];
    private static int[] testResidual = new int[16];

    static {
        for (int i = 0; i < 16; i++) {
            testResidual[i] = 16 * i - 128;
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
        int[] idxMap = { 0, 1, 2, 3, 16, 17, 18, 19, 32, 33, 34, 35, 48, 49, 50, 51 };
        int off = (blkY << 4) + blkX;
        for (int i = 0; i < 16; i++) {
            result[idxMap[i] + off] = is[i];
        }

        return result;
    }

    @Test
    public void testDC() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(1, 2, 3, 4, 5, 6, 7, 8);
        byte[] left = toByteArrayShifted(9, 10, 11, 12);

        Intra4x4PredictionBuilder.predictDC(emptyResidual, true, true, left, top, 0, 0, 0, pred);
        assertArrayEquals(inMB(toByteArrayShifted(7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7), 0, 0), pred);

        for (int blkY = 0; blkY < 16; blkY += 4) {
            for (int blkX = 0; blkX < 16; blkX += 4) {
                Arrays.fill(pred, (byte) 0);
                Intra4x4PredictionBuilder.predictDC(testResidual, true, true, padLeft(left, blkY), padLeft(top, blkX),
                        0, blkX, blkY, pred);
                assertArrayEquals(
                        "@[" + blkX + ", " + blkY + "]",
                        inMB(addResidual(toByteArrayShifted(7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7),
                                testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testVertical() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(1, 2, 3, 4, 5, 6, 7, 8);

        Intra4x4PredictionBuilder.predictVertical(emptyResidual, true, top, 0, 0, 0, pred);
        assertArrayEquals(inMB(toByteArrayShifted(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4), 0, 0), pred);

        for (int blkY = 0; blkY < 16; blkY += 4) {
            for (int blkX = 0; blkX < 16; blkX += 4) {
                Arrays.fill(pred, (byte) 0);
                Intra4x4PredictionBuilder.predictVertical(testResidual, true, padLeft(top, blkX), 0, blkX, blkY, pred);
                assertArrayEquals(
                        "@[" + blkX + ", " + blkY + "]",
                        inMB(addResidual(toByteArrayShifted(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4),
                                testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testHorizontal() throws Exception {

        byte[] pred = new byte[256];

        // byte[] top = toByteArrayShifted( 1, 2, 3, 4, 5, 6, 7, 8 };
        byte[] left = toByteArrayShifted(9, 10, 11, 12);

        Intra4x4PredictionBuilder.predictHorizontal(emptyResidual, true, left, 0, 0, 0, pred);
        assertArrayEquals(inMB(toByteArrayShifted(9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11, 12, 12, 12, 12), 0, 0),
                pred);
        for (int blkY = 0; blkY < 16; blkY += 4) {
            for (int blkX = 0; blkX < 16; blkX += 4) {
                Arrays.fill(pred, (byte) 0);
                Intra4x4PredictionBuilder.predictHorizontal(testResidual, true, padLeft(left, blkY), 0, blkX, blkY,
                        pred);
                assertArrayEquals(
                        "@[" + blkX + ", " + blkY + "]",
                        inMB(addResidual(
                                toByteArrayShifted(9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11, 12, 12, 12, 12),
                                testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testDiagonalDownLeft() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(209, 212, 218, 222, 216, 219, 225, 229);

        Intra4x4PredictionBuilder.predictDiagonalDownLeft(emptyResidual, true, true, top, 0, 0, 0, pred);
        assertArrayEquals(
                inMB(toByteArrayShifted(213, 218, 220, 218, 218, 220, 218, 220, 220, 218, 220, 225, 218, 220, 225, 228),
                        0, 0), pred);
        for (int blkY = 0; blkY < 16; blkY += 4) {
            for (int blkX = 0; blkX < 16; blkX += 4) {
                Arrays.fill(pred, (byte) 0);
                Intra4x4PredictionBuilder.predictDiagonalDownLeft(testResidual, true, true, padLeft(top, blkX), 0,
                        blkX, blkY, pred);
                assertArrayEquals(
                        "@[" + blkX + ", " + blkY + "]",
                        inMB(addResidual(
                                toByteArrayShifted(213, 218, 220, 218, 218, 220, 218, 220, 220, 218, 220, 225, 218,
                                        220, 225, 228), testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testDiagonalDownRight() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(183, 196, 170, 131, 0, 0, 0, 0);
        byte[] left = toByteArrayShifted(207, 207, 207, 207);
        byte[] tl = toByteArrayShifted(196, 0, 0, 0);

        Intra4x4PredictionBuilder.predictDiagonalDownRight(emptyResidual, true, true, left, top, tl, 0, 0, 0, pred);
        assertArrayEquals(
                inMB(toByteArrayShifted(196, 190, 186, 167, 204, 196, 190, 186, 207, 204, 196, 190, 207, 207, 204, 196),
                        0, 0), pred);
        for (int blkY = 0; blkY < 16; blkY += 4) {
            for (int blkX = 0; blkX < 16; blkX += 4) {
                Arrays.fill(pred, (byte) 0);
                Intra4x4PredictionBuilder.predictDiagonalDownRight(testResidual, true, true, padLeft(left, blkY),
                        padLeft(top, blkX), padLeft(tl, blkY >> 2), 0, blkX, blkY, pred);
                assertArrayEquals(
                        "@[" + blkX + ", " + blkY + "]",
                        inMB(addResidual(
                                toByteArrayShifted(196, 190, 186, 167, 204, 196, 190, 186, 207, 204, 196, 190, 207,
                                        207, 204, 196), testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testDiagonalDownRight1() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(236, 236, 236, 236, 0, 0, 0, 0);
        byte[] left = toByteArrayShifted(233, 233, 233, 233);
        byte[] tl = toByteArrayShifted(226, 0, 0, 0);

        Intra4x4PredictionBuilder.predictDiagonalDownRight(emptyResidual, true, true, left, top, tl, 0, 0, 0, pred);
        assertArrayEquals(
                inMB(toByteArrayShifted(230, 234, 236, 236, 231, 230, 234, 236, 233, 231, 230, 234, 233, 233, 231, 230),
                        0, 0), pred);
        for (int blkY = 0; blkY < 16; blkY += 4) {
            for (int blkX = 0; blkX < 16; blkX += 4) {
                Arrays.fill(pred, (byte) 0);
                Intra4x4PredictionBuilder.predictDiagonalDownRight(testResidual, true, true, padLeft(left, blkY),
                        padLeft(top, blkX), padLeft(tl, blkY >> 2), 0, blkX, blkY, pred);
                assertArrayEquals(
                        "@[" + blkX + ", " + blkY + "]",
                        inMB(addResidual(
                                toByteArrayShifted(230, 234, 236, 236, 231, 230, 234, 236, 233, 231, 230, 234, 233,
                                        233, 231, 230), testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testVerticalRight() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(207, 201, 197, 175, 0, 0, 0, 0);
        byte[] left = toByteArrayShifted(208, 176, 129, 122);
        byte[] tl = toByteArrayShifted(206, 0, 0, 0);

        Intra4x4PredictionBuilder.predictVerticalRight(emptyResidual, true, true, left, top, tl, 0, 0, 0, pred);
        assertArrayEquals(
                inMB(toByteArrayShifted(207, 204, 199, 186, 207, 205, 202, 193, 200, 207, 204, 199, 172, 207, 205, 202),
                        0, 0), pred);
        for (int blkY = 0; blkY < 16; blkY += 4) {
            for (int blkX = 0; blkX < 16; blkX += 4) {
                Arrays.fill(pred, (byte) 0);
                Intra4x4PredictionBuilder.predictVerticalRight(testResidual, true, true, padLeft(left, blkY),
                        padLeft(top, blkX), padLeft(tl, blkY >> 2), 0, blkX, blkY, pred);
                assertArrayEquals(
                        "@[" + blkX + ", " + blkY + "]",
                        inMB(addResidual(
                                toByteArrayShifted(207, 204, 199, 186, 207, 205, 202, 193, 200, 207, 204, 199, 172,
                                        207, 205, 202), testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testHorizontalDown() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(209, 157, 114, 118);
        byte[] left = toByteArrayShifted(197, 198, 202, 205);
        byte[] tl = toByteArrayShifted(204, 0, 0, 0);

        Intra4x4PredictionBuilder.predictHorizontalDown(emptyResidual, true, true, left, top, tl, 0, 0, 0, pred);
        assertArrayEquals(
                inMB(toByteArrayShifted(201, 204, 195, 159, 198, 199, 201, 204, 200, 199, 198, 199, 204, 202, 200, 199),
                        0, 0), pred);
        for (int blkY = 0; blkY < 16; blkY += 4) {
            for (int blkX = 0; blkX < 16; blkX += 4) {
                Arrays.fill(pred, (byte) 0);
                Intra4x4PredictionBuilder.predictHorizontalDown(testResidual, true, true, padLeft(left, blkY),
                        padLeft(top, blkX), padLeft(tl, blkY >> 2), 0, blkX, blkY, pred);
                assertArrayEquals(
                        "@[" + blkX + ", " + blkY + "]",
                        inMB(addResidual(
                                toByteArrayShifted(201, 204, 195, 159, 198, 199, 201, 204, 200, 199, 198, 199, 204,
                                        202, 200, 199), testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testVerticalLeft() throws Exception {

        byte[] pred = new byte[256];

        byte[] top = toByteArrayShifted(215, 201, 173, 159, 137, 141, 150, 155);

        Intra4x4PredictionBuilder.predictVerticalLeft(emptyResidual, true, true, top, 0, 0, 0, pred);
        assertArrayEquals(
                inMB(toByteArrayShifted(208, 187, 166, 148, 198, 177, 157, 144, 187, 166, 148, 139, 177, 157, 144, 142),
                        0, 0), pred);
        for (int blkY = 0; blkY < 16; blkY += 4) {
            for (int blkX = 0; blkX < 16; blkX += 4) {
                Arrays.fill(pred, (byte) 0);
                Intra4x4PredictionBuilder.predictVerticalLeft(testResidual, true, true, padLeft(top, blkX), 0, blkX,
                        blkY, pred);
                assertArrayEquals(
                        "@[" + blkX + ", " + blkY + "]",
                        inMB(addResidual(
                                toByteArrayShifted(208, 187, 166, 148, 198, 177, 157, 144, 187, 166, 148, 139, 177,
                                        157, 144, 142), testResidual), blkX, blkY), pred);
            }
        }
    }

    @Test
    public void testHorizontalUp() throws Exception {

        byte[] pred = new byte[256];

        byte[] left = toByteArrayShifted(175, 180, 216, 221);

        Intra4x4PredictionBuilder.predictHorizontalUp(emptyResidual, true, left, 0, 0, 0, pred);
        assertArrayEquals(
                inMB(toByteArrayShifted(178, 188, 198, 208, 198, 208, 219, 220, 219, 220, 221, 221, 221, 221, 221, 221),
                        0, 0), pred);
        for (int blkY = 0; blkY < 16; blkY += 4) {
            for (int blkX = 0; blkX < 16; blkX += 4) {
                Arrays.fill(pred, (byte) 0);
                Intra4x4PredictionBuilder.predictHorizontalUp(testResidual, true, padLeft(left, blkY), 0, blkX, blkY,
                        pred);
                assertArrayEquals(
                        "@[" + blkX + ", " + blkY + "]",
                        inMB(addResidual(
                                toByteArrayShifted(178, 188, 198, 208, 198, 208, 219, 220, 219, 220, 221, 221, 221,
                                        221, 221, 221), testResidual), blkX, blkY), pred);
            }
        }
    }

}
