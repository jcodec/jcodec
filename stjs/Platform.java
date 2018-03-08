package org.jcodec.platform;
import js.lang.IllegalArgumentException;
import js.lang.IllegalStateException;
import js.lang.Comparable;
import js.lang.StringBuilder;
import js.lang.System;
import js.lang.Runtime;
import js.lang.Runnable;
import js.lang.Process;
import js.lang.ThreadLocal;
import js.lang.IndexOutOfBoundsException;
import js.lang.Thread;
import js.lang.NullPointerException;

import static org.stjs.javascript.JSFunctionAdapter.call;
import static org.stjs.javascript.JSObjectAdapter.$get;

import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.common.tools.ToJSON;
import org.stjs.javascript.Global;
import org.stjs.javascript.JSCollections;
import org.stjs.javascript.JSFunctionAdapter;
import org.stjs.javascript.JSGlobal;
import org.stjs.javascript.JSObjectAdapter;
import org.stjs.javascript.JSStringAdapter;

import js.io.File;
import js.io.InputStream;
import js.io.UnsupportedEncodingException;
import js.lang.reflect.Field;
import js.net.URL;
import js.nio.charset.Charset;
import js.util.Arrays;

public class Platform {

    public final static String UTF_8 = "UTF-8";
    public final static String UTF_16 = "UTF-16";
    public final static String UTF_16BE = "UTF-16BE";
    public final static String ISO8859_1 = "iso8859-1";

    public static <T> T newInstance(Class<T> clazz, Object[] params) {
        return JSObjectAdapter.$js("new (Function.prototype.bind.apply(clazz, [null].concat(params)))");
    }

    public static Field[] getDeclaredFields(Class<?> class1) {
        return null;
    }

    public static Field[] getFields(Class<?> class1) {
        return null;
    }

    public static String stringFromCharset(byte[] data, String charset) {
        return null;
    }

    public static byte[] getBytesForCharset(String url, String charset) {
        return null;
    }

    public static String stringFromCharset4(byte[] data, int offset, int len, String charset) {
        return null;
    }

    public static URL getResource(Class<?> class1, String string) {
        return null;
    }

    public static boolean arrayEqualsInt(int[] a, int[] a2) {
        return Arrays.arrayEquals(a, a2);
    }

    public static boolean arrayEqualsByte(byte[] a, byte[] a2) {
        return Arrays.arrayEquals(a, a2);
    }

    public static boolean arrayEqualsObj(Object[] a, Object[] a2) {
        return Arrays.arrayEquals(a, a2);
    }

    public static <T> T[] copyOfRangeO(T[] original, int from, int to) {
        return Arrays.copyOfRange(original, from, to);
    }

    public static long[] copyOfRangeL(long[] original, int from, int to) {
        return Arrays.copyOfRange(original, from, to);
    }

    public static int[] copyOfRangeI(int[] original, int from, int to) {
        return Arrays.copyOfRange(original, from, to);
    }

    public static byte[] copyOfRangeB(byte[] original, int from, int to) {
        return Arrays.copyOfRange(original, from, to);
    }

    public static <T> T[] copyOfObj(T[] original, int newLength) {
        return Arrays.copyOf(original, newLength);
    }

    public static long[] copyOfLong(long[] original, int newLength) {
        return Arrays.copyOf(original, newLength);
    }

    public static int[] copyOfInt(int[] original, int newLength) {
        return Arrays.copyOf(original, newLength);
    }

    public static boolean[] copyOfBool(boolean[] original, int newLength) {
        return Arrays.copyOf(original, newLength);
    }

    public static byte[] copyOfByte(byte[] original, int newLength) {
        return Arrays.copyOf(original, newLength);
    }

    public static String arrayToString(Object[] a) {
        return Arrays.arrayToString(a);
    }

    public static void deleteFile(File file) {
        throw new RuntimeException("deleteFile not implemented");
    }

    public static byte[] getBytes(String fourcc) {
        try {
            return fourcc.getBytes("iso8859-1");
        } catch (Exception e) {
            return null;
        }
    }

    public static String stringFromBytes(byte[] bytes) {
        try {
            return JSObjectAdapter.$js("String.fromCharCode.apply(null, bytes)");
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isAssignableFrom(Class class1, Class class2) {
        return class1.isAssignableFrom(class2);
    }

    public static InputStream stdin() {
        return null;
    }

    public static String toJSON(Object o) {
        return JSGlobal.JSON.stringify(o);
    }

    public static <T> T invokeStaticMethod(Class<?> cls, String methodName, Object[] params) {
        Object method = $get(cls, methodName);
        return (T) call(method, cls, params);
    }
}
