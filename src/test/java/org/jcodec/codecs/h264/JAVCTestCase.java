package org.jcodec.codecs.h264;

import java.util.Arrays;

import junit.framework.TestCase;

import org.jcodec.codecs.util.FrameUtil;
import org.jcodec.common.model.Picture;

public class JAVCTestCase extends TestCase {

    protected void assertArrayEquals(int[] expected, int[] actual) {
        assertArrayEquals(expected, actual, Integer.MAX_VALUE);
    }

    protected void assertArrayEquals(int[] expected, int[] actual, int n) {
        if (n > expected.length)
            n = expected.length;

        if (n > actual.length)
            n = actual.length;

        boolean equals = true;
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != actual[i]) {
                equals = false;
                break;
            }
        }

        String msg = "\nExpected: " + Arrays.toString(expected) + "\nActual: " + Arrays.toString(actual);

        assertTrue(msg, equals);
    }

    protected void assertArrayEquals(Object[] expected, Object[] actual) {
        boolean equals = true;
        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equals(actual[i])) {
                equals = false;
                break;
            }
        }

        String msg = "\nExpected: " + Arrays.toString(expected) + "\nActual: " + Arrays.toString(actual);

        assertTrue(msg, equals);
    }

    protected void assertLumaEquals(Picture expected, Picture actual) {
        StringBuilder builder = new StringBuilder();
        if (!compareComponentsBlockwise(expected.getPlaneData(0), actual.getPlaneData(0), expected.getWidth(), expected.getHeight(),
                builder)) {
            assertTrue(builder.toString(), false);
        }
    }

    protected void assertCbEquals(Picture expected, Picture actual) {
        StringBuilder builder = new StringBuilder();
        if (!compareComponentsBlockwise(expected.getPlaneData(1), actual.getPlaneData(1), expected.getWidth() / 2,
                expected.getHeight() / 2, builder)) {
            assertTrue(builder.toString(), false);
        }
    }

    protected void assertCrEquals(Picture expected, Picture actual) {
        StringBuilder builder = new StringBuilder();
        if (!compareComponentsBlockwise(expected.getPlaneData(2), actual.getPlaneData(2), expected.getWidth() / 2,
                expected.getHeight() / 2, builder)) {
            assertTrue(builder.toString(), false);
        }
    }

    protected void assertFrameEquals(Picture expected, Picture actual) {
        StringBuilder builder = new StringBuilder();
        if (!compareComponentsBlockwise(expected.getPlaneData(0), actual.getPlaneData(0), expected.getWidth(), expected.getHeight(),
                builder)) {
            assertTrue("Luma doesn't match: " + builder.toString(), false);
        }
        if (!compareComponentsBlockwise(expected.getPlaneData(1), actual.getPlaneData(1), expected.getWidth() / 2,
                expected.getHeight() / 2, builder)) {
            assertTrue("CB doesn't match: " + builder.toString(), false);
        }
        if (!compareComponentsBlockwise(expected.getPlaneData(2), actual.getPlaneData(2), expected.getWidth() / 2,
                expected.getHeight() / 2, builder)) {
            assertTrue("CR doesn't match: " + builder.toString(), false);
        }
    }

    protected boolean compareComponentsBlockwise(int[] expected, int[] actual, int width, int height,
            StringBuilder builder) {
        int widthInBlks = width >> 2;
        int heightInBlks = height >> 2;
        int stride = width;

        for (int blkY = 0; blkY < heightInBlks; blkY++) {
            for (int blkX = 0; blkX < widthInBlks; blkX++) {
                StringBuilder blk1 = new StringBuilder();
                StringBuilder blk2 = new StringBuilder();
                boolean match = true;
                for (int j = 0; j < 4; j++) {
                    for (int i = 0; i < 4; i++) {
                        int offset = ((blkY << 2) + j) * stride + ((blkX << 2) + i);
                        int c1 = expected[offset];
                        int c2 = actual[offset];
                        if (c1 != c2)
                            match = false;
                        blk1.append("" + c1 + ", ");
                        blk2.append("" + c2 + ", ");
                    }
                    blk1.append('\n');
                    blk2.append('\n');
                }
                if (!match) {
                    int blkAddr = blkY * widthInBlks + blkX;
                    builder.append("Block " + blkAddr + " does not match:\n" + blk1.toString() + " ---- \n"
                            + blk2.toString());
                    return false;
                }
            }
        }
        return true;
    }

    protected void assertMBEquals(String message, int[] expected, int[] actual) {

        StringBuilder builder = new StringBuilder();
        if (!compareComponentsBlockwise(expected, actual, 16, 16, builder)) {
            FrameUtil.displayComponent(expected, 16, 16, true);
            FrameUtil.displayComponent(actual, 16, 16, true);
            assertTrue(builder.toString(), false);
        }
    }
}