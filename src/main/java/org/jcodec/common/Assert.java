package org.jcodec.common;

public class Assert {

    public static void assertTrue(boolean b) {
        if (!b)
            throw new AssertionError();
    }

    public static void assertTrue(String msg, boolean b) {
        if (!b)
            throw new AssertionError(msg);
    }

    public static void assertEquals(String msg, int i, int j) {
        if (i != j)
            throw new AssertionError(msg+" expected "+i+" actual "+j);
    }

    public static void assertEquals(int i, int j) {
        if (i != j)
            throw new AssertionError();
    }

    public static void assertEquals(long i, int j) {
        if (i != j)
            throw new AssertionError();
    }

    public static void assertNotNull(Object obj) {
        if (obj == null)
            throw new AssertionError();
    }
}
