package org.jcodec.platform;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;

public class Platform {

    public static <T> T newInstance(Class<T> clazz, Object... params) {
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

    public static void invokeMethod(Object target, String methodName, Object... params) throws NoSuchMethodException {
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

    public static String stringFromCharset(byte[] data, int offset, int len, Charset charset) {
        return new String(data, offset, len, charset);
    }
    
    public static URL getResource(Class<?> class1, String string) {
        return class1.getResource(string);
 }
    
    public static boolean arrayEquals(Object... arguments) {
        throw new RuntimeException("TODO");
    }
    
    public static <T> T[] copyOfRangeO(T[] original, int from, int to) {
        throw new RuntimeException("TODO");
    }

    public static long[] copyOfRangeL(long[] positions, int from, int to) {
        throw new RuntimeException("TODO");
    }

    public static int[] copyOfRangeI(int[] sizes, int sampleNo, int to) {
        throw new RuntimeException("TODO");
    }

    public static byte[] copyOfRangeB(byte[] arr, int off, int min) {
        // TODO Auto-generated method stub
        throw new RuntimeException("TODO");
    }

    public static <T> T[] copyOfObj(T[] original, int newLength) {
        throw new RuntimeException("TODO");
    }

    public static long[] copyOfLong(long[] values, int length) {
        throw new RuntimeException("TODO");
    }

    public static int[] copyOfInt(int[] values, int length) {
        throw new RuntimeException("TODO");
    }

    public static boolean[] copyOfBool(boolean[] shortUsed, int length) {
        throw new RuntimeException("TODO");
    }

    public static byte[] copyOfByte(byte[] asByteArray, int newLength) {
        // TODO Auto-generated method stub
        throw new RuntimeException("TODO");
    }
    
    public static String arrayToString(Object[] references) {
        throw new RuntimeException("TODO");
    }

    public static void deleteFile(File video) {
        throw new RuntimeException("TODO Platform.deleteFile");
    }

}
