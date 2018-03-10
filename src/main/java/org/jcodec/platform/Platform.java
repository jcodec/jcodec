package org.jcodec.platform;

import org.jcodec.common.tools.ToJSON;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Platform {

    public final static String UTF_8 = "UTF-8";
    public final static String UTF_16 = "UTF-16";
    public final static String UTF_16BE = "UTF-16BE";
    public final static String ISO8859_1 = "iso8859-1";

    private final static Map<Class, Class> boxed2primitive = new HashMap<Class, Class>();
    static {
        boxed2primitive.put(Void.class, void.class);
        boxed2primitive.put(Byte.class, byte.class);
        boxed2primitive.put(Short.class, short.class);
        boxed2primitive.put(Character.class, char.class);
        boxed2primitive.put(Integer.class, int.class);
        boxed2primitive.put(Long.class, long.class);
        boxed2primitive.put(Float.class, float.class);
        boxed2primitive.put(Double.class, double.class);
    }

    public static <T> T newInstance(Class<T> clazz, Object[] params) {
        try {
            return clazz.getConstructor(classes(params)).newInstance(params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Class[] classes(Object[] params) {
        Class[] classes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            Class<?> cls = params[i].getClass();
            if (boxed2primitive.containsKey(cls)) {
                classes[i] = boxed2primitive.get(cls);
            } else {
                classes[i] = cls;
            }
        }
        return classes;
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

    public static boolean deleteFile(File file) {
        return file.delete();
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
        if (class1 == class2 || class1.equals(class2)) {
            return true;
        }
        return class1.isAssignableFrom(class2);
    }

    public static InputStream stdin() {
        return System.in;
    }

    public static String toJSON(Object o) {
        return ToJSON.toJSON(o);
    }

    public static <T> T invokeStaticMethod(Class<?> cls, String methodName, Object[] params) {
        try {
            for (Method method : cls.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    return (T) method.invoke(null, params);
                }
            }
            throw new NoSuchMethodException(cls + "." + methodName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static long unsignedInt(int signed) {
        return (long) signed & 0xffffffffL;
    }
}
