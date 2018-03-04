package org.jcodec.common;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Assert {

    public static void assertTrue(String msg, boolean b) {
        if (!b)
            throw new AssertionError(msg);
    }

    public static void assertEquals(int i, int j) {
        if (i != j)
            throw new AssertionError();
    }

    public static void assertEqualsLong(long i, long j) {
        if (i != j)
            throw new AssertionError();
    }
    
    public static void assertNotNull(Object obj) {
        if (obj == null)
            throw new AssertionError();
    }

    public static void assertArrayEquals(int[] a, int[] b) {
        if (a == b)
            return;
        if (a == null || b == null)
            throw new AssertionError();
        if (a.length != b.length)
            throw new AssertionError();
        for (int i = 0; i < a.length; i++)
            if (a[i] != b[i])
                throw new AssertionError();
    }
}
