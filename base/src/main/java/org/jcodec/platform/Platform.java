package org.jcodec.platform;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;

public class Platform {

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

    public static void invokeMethod(Object target, String methodName, Object[] params) throws NoSuchMethodException {
        Class[] parameterTypes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            parameterTypes[i] = params[i].getClass();
        }
        try {
            target.getClass().getDeclaredMethod(methodName, parameterTypes).invoke(target, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Method[] getDeclaredMethods(Class<?> claz) {
        return claz.getDeclaredMethods();
    }

    public static Method[] getMethods(Class<?> class1) {
        return class1.getMethods();
    }

    public static Field[] getDeclaredFields(Class<?> class1) {
        return class1.getDeclaredFields();
    }

    public static Field[] getFields(Class<?> class1) {
        return class1.getFields();
    }

    public static String stringFromCharset(byte[] data, Charset charset) {
        return new String(data, charset);
    }

    public static byte[] getBytesForCharset(String url, Charset utf8) {
        return url.getBytes(utf8);
    }

    public static InputStream getResourceAsStream(Class<?> class1, String string) {
        return class1.getClassLoader().getResourceAsStream(string);
    }

    public static String stringFromCharset4(byte[] data, int offset, int len, Charset charset) {
        return new String(data, offset, len, charset);
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
        return fourcc.getBytes();
    }

    public static String stringFromBytes(byte[] bytes) {
        return new String(bytes);
    }

    public static boolean isAssignableFrom(Class class1, Class class2) {
        return class1.isAssignableFrom(class2);
    }

}
