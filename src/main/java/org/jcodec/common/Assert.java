package org.jcodec.common;

import static org.stjs.javascript.Global.console;

import js.lang.AssertionError;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Assert {
    public static void assertEquals(Object... arguments) {
        if (arguments.length == 2) {
            if (arguments[0] != arguments[1]) {
                throw new AssertionError(arguments[0] + " != " + arguments[1]);
            }
        } else {
            console.log("arguments", arguments);
            throw new RuntimeException("TODO jcodec Assert.assertEquals");
        }
    }

    public static void assertTrue(Object... arguments) {
        if (arguments.length == 1) {
            boolean expected = (boolean) arguments[0];
            if (!expected) {
                throw new AssertionError();
            }
        } else if (arguments.length == 2) {
            String msg = (String) arguments[0];
            boolean expected = (boolean) arguments[1];
            if (!expected) {
                throw new AssertionError(msg);
            }
        } else {
            console.log("arguments", arguments);
            throw new RuntimeException("TODO Assert.assertTrue " + arguments.length);
        }
    }

    //    public static void assertTrue(boolean b) {
    //        if (!b)
    //            throw new AssertionError();
    //    }
    //
    //    public static void assertTrue(String msg, boolean b) {
    //        if (!b)
    //            throw new AssertionError(msg);
    //    }
    //
    //    public static void assertEquals(String msg, int i, int j) {
    //        if (i != j)
    //            throw new AssertionError(msg + " expected " + i + " actual " + j);
    //    }
    //
    //    public static void assertEquals(String i, String j) {
    //        if (i != j)
    //            throw new AssertionError("Expected " + i + " actual " + j);
    //    }
    //
    //    public static void assertEquals(int i, int j) {
    //        if (i != j)
    //            throw new AssertionError();
    //    }
    //
    //    public static void assertEquals(long i, long j) {
    //        if (i != j)
    //            throw new AssertionError();
    //    }

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
