package org.jcodec.platform;

import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.common.tools.ToJSON;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;

public class Platform {

    public final static String UTF_8 = "UTF-8";
    public final static String UTF_16 = "UTF-16";

    public static <T> T newInstance(Class<T> clazz, Object[] params) {
        Class[] classes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            classes[i] = params[i].getClass();
        }
        try {
            return clazz.getConstructor(classes).newInstance(params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Field[] getDeclaredFields(Class<?> class1) {
        return class1.getDeclaredFields();
    }

    public static Field[] getFields(Class<?> class1) {
        return class1.getFields();
    }

    public static String stringFromCharset(byte[] data, String charset) {
        return new String(data, Charset.forName(charset));
    }

    public static byte[] getBytesForCharset(String url, String charset) {
        return url.getBytes(Charset.forName(charset));
    }

    public static String stringFromCharset4(byte[] data, int offset, int len, String charset) {
        return new String(data, offset, len, Charset.forName(charset));
    }

    public static URL getResource(Class<?> class1, String string) {
        return class1.getResource(string);
    }

    public static boolean arrayEqualsInt(int[] a, int[] a2) {
        return Arrays.equals(a, a2);
    }

    public static boolean arrayEqualsByte(byte[] a, byte[] a2) {
        return Arrays.equals(a, a2);
    }

    public static boolean arrayEqualsObj(Object[] a, Object[] a2) {
        return Arrays.equals(a, a2);
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
        return Arrays.toString(a);
    }

    public static void deleteFile(File file) {
        file.delete();
    }

    public static byte[] getBytes(String fourcc) {
        try {
            return fourcc.getBytes("iso8859-1");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String stringFromBytes(byte[] bytes) {
        try {
            return new String(bytes, "iso8859-1");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static boolean isAssignableFrom(Class class1, Class class2) {
        return class1.isAssignableFrom(class2);
    }

    public static InputStream stdin() {
        return System.in;
    }

    public static String toJSON(Object o) {
        return ToJSON.toJSON(o);
    }
}
