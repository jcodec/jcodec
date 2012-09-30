package org.jcodec.codecs.mjpeg.tools;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Jay Codec
 *
 */
public class Asserts {
    public static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new IllegalStateException("assert failed: " + expected
                    + " != " + actual);
        }
    }

    public static void assertInRange(String message, int low, int up, int val) {
        if (val < low || val > up) {
            throw new IllegalStateException(message);
        }
    }

    public static void assertEpsilonEquals(int[] expected, int[] actual, int eps) {
        if (expected.length != actual.length)
            throw new IllegalStateException("arrays of different size");
        for (int i = 0; i < expected.length; i++) {
            int e = expected[i];
            int a = actual[i];
            if (Math.abs(e - a) > eps) {
                throw new IllegalStateException(
                        "array element out of expected diff range");
            }
        }
    }

    public static void assertEpsilonEquals(byte[] expected, byte[] actual,
            int eps) {
        if (expected.length != actual.length)
            throw new IllegalStateException("arrays of different size");
        for (int i = 0; i < expected.length; i++) {
            int e = expected[i] & 0xff;
            int a = actual[i] & 0xff;
            if (Math.abs(e - a) > eps) {
                throw new IllegalStateException(
                        "array element out of expected diff range: "
                                + (Math.abs(e - a)));
            }
        }
    }
}
