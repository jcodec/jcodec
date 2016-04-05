package org.jcodec.platform;

import static org.stjs.javascript.JSCollections.$castArray;
import static org.stjs.javascript.JSObjectAdapter.$prototype;
import static org.stjs.javascript.JSStringAdapterBase.charCodeAt;
import static org.stjs.javascript.JSStringAdapterBase.fromCharCode;

import org.stjs.javascript.Array;
import org.stjs.javascript.Global;
import org.stjs.javascript.JSFunctionAdapter;
import org.stjs.javascript.JSGlobal;

import js.io.File;
import js.io.InputStream;
import js.lang.NoSuchMethodException;
import js.lang.StJs;
import js.lang.VGObjectAdapter;
import js.lang.reflect.Field;
import js.lang.reflect.Method;
import js.net.URL;
import js.nio.charset.Charset;
import js.util.JSArrays;

public class Platform {
    static Object neu(Class cls, Array params) {
        // http://www.ecma-international.org/ecma-262/5.1/#sec-13.2.2
        Object instance = VGObjectAdapter.create(Object.class, $prototype(cls));
        Object result = JSFunctionAdapter.apply(cls, instance, params);
        Object object = (result != null && JSGlobal.typeof(result) == "object") ? result : instance;
        return object;
    }

    public static <T> T newInstance(Class<T> clazz, Object[] params) throws NoSuchMethodException {
        Global.console.log("newInstance", clazz);
        Object created = neu(clazz, $castArray(params));
        return (T) created;

        //        Class[] classes = new Class[params.length];
        //        for (int i = 0; i < params.length; i++) {
        //            classes[i] = params[i].getClass();
        //        }
        //        try {
        //            return clazz.getConstructor(classes).newInstance(params);
        //        } catch (Exception e) {
        //            throw new RuntimeException(e);
        //        }
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
        throw new RuntimeException("TODO");
        //        return claz.getDeclaredMethods();
    }

    public static Method[] getMethods(Class<?> class1) {
        throw new RuntimeException("TODO");
        //        return class1.getMethods();
    }

    public static Field[] getDeclaredFields(Class<?> class1) {
        throw new RuntimeException("TODO");
        //        return class1.getDeclaredFields();
    }

    public static Field[] getFields(Class<?> class1) {
        throw new RuntimeException("TODO");
        //        return class1.getFields();
    }

    public static String stringFromCharset(byte[] data, Charset charset) {
        throw new RuntimeException("TODO");
        //        return new String(data, charset);
    }

    public static byte[] getBytesForCharset(String url, Charset utf8) {
        throw new RuntimeException("TODO");
        //        return url.getBytes(utf8);
    }

    public static InputStream getResourceAsStream(Class<?> class1, String string) {
        throw new RuntimeException("TODO");
        //        return class1.getClassLoader().getResourceAsStream(string);
    }

    public static String stringFromCharset4(byte[] data, int offset, int len, Charset charset) {
        throw new RuntimeException("TODO");
        //        return new String(data, offset, len, charset);
    }

    public static URL getResource(Class<?> class1, String string) {
//        return class1.getResource(string);
        throw new RuntimeException("TODO");
    }

    public static boolean arrayEqualsInt(int[] a, int[] a2) {
        throw new RuntimeException("TODO");
        //        return Arrays.equals(a, a2);
    }

    public static boolean arrayEqualsByte(byte[] a, byte[] a2) {
        throw new RuntimeException("TODO");
        //        return Arrays.equals(a, a2);
    }

    public static boolean arrayEqualsObj(Object[] a, Object[] a2) {
        return JSArrays.equalsObjs(a, a2);
        //        return Arrays.equals(a, a2);
    }

    public static <T> T[] copyOfRangeO(T[] original, int from, int to) {
        throw new RuntimeException("TODO");
        //        return Arrays.copyOfRange(original, from, to);
    }

    public static long[] copyOfRangeL(long[] original, int from, int to) {
        throw new RuntimeException("TODO");
        //        return Arrays.copyOfRange(original, from, to);
    }

    public static int[] copyOfRangeI(int[] original, int from, int to) {
        throw new RuntimeException("TODO");
        //        return Arrays.copyOfRange(original, from, to);
    }

    public static byte[] copyOfRangeB(byte[] original, int from, int to) {
        throw new RuntimeException("TODO");
        //        return Arrays.copyOfRange(original, from, to);
    }

    public static <T> T[] copyOfObj(T[] original, int newLength) {
        Object[] copy = new Object[newLength];
        js.lang.System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return (T[]) copy;
    }

    public static long[] copyOfLong(long[] original, int newLength) {
        throw new RuntimeException("TODO");
        //        return Arrays.copyOf(original, newLength);
    }

    public static int[] copyOfInt(int[] original, int newLength) {
        int[] copy = new int[newLength];
        js.lang.System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }

    public static boolean[] copyOfBool(boolean[] original, int newLength) {
        throw new RuntimeException("TODO");
        //        return Arrays.copyOf(original, newLength);
    }

    public static byte[] copyOfByte(byte[] original, int newLength) {
        throw new RuntimeException("TODO");
        //        return Arrays.copyOf(original, newLength);
    }

    public static String arrayToString(Object[] a) {
        throw new RuntimeException("TODO");
        //        return Arrays.toString(a);
    }

    public static void deleteFile(File file) {
        throw new RuntimeException("TODO");
        //        file.delete();
    }

    public static byte[] getBytes(String string) {
        //        return string.getBytes();
        byte[] buf = new byte[string.length()];
        for (int i = 0; i < string.length(); i++) {
            int charCodeAt = charCodeAt(string, i);
            buf[i] = (byte) charCodeAt;
        }
        return buf;
    }

    public static String stringFromBytes(byte[] bytes) {
        String str = "";
        for (int i = 0; i < bytes.length; i++) {
            str += fromCharCode(String.class, bytes[i]);
        }
        return str;
        // return new String(bytes);
    }

    public static boolean isAssignableFrom(Class parent, Class child) {
        return StJs.stjs.isInstanceOf(child, parent);
//        return parent.isAssignableFrom(child);
    }

}
