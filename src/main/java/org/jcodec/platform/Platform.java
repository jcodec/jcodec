package org.jcodec.platform;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

}
