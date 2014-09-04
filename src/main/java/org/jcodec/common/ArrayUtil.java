package org.jcodec.common;

import java.lang.reflect.Array;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class ArrayUtil {

    public static <T> void shiftRight(T[] array) {
        for (int i = 1; i < array.length; i++) {
            array[i] = array[i - 1];
        }
        array[0] = null;
    }

    public static <T> void shiftLeft(T[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            array[i] = array[i + 1];
        }
        array[array.length - 1] = null;
    }

    public static <T> void shiftRight(T[] array, int from, int to) {
        for (int i = to - 1; i > from; i--) {
            array[i] = array[i - 1];
        }
        array[from] = null;
    }

    public static <T> void shiftLeft(T[] array, int from, int to) {
        for (int i = from; i < to - 1; i++) {
            array[i] = array[i + 1];
        }
        array[to - 1] = null;
    }

    public static <T> void shiftLeft(T[] array, int from) {
        shiftLeft(array, from, array.length);
    }

    public static <T> void shiftRight(T[] array, int to) {
        shiftRight(array, 0, to);
    }

    public static void swap(int[] arr, int ind1, int ind2) {
        int tmp = arr[ind1];
        arr[ind1] = arr[ind2];
        arr[ind2] = tmp;
    }

    public static int sum(int[] array) {
        int result = 0;
        for (int i : array) {
            result += i;
        }
        return result;
    }

    public static int sum(int[] array, int from, int count) {
        int result = 0;
        for (int i = from; i < from + count; i++) {
            result += array[i];
        }
        return result;
    }

    public static void add(int[] array, int val) {
        for (int i = 0; i < array.length; i++)
            array[i] += val;
    }

    public static int[] addAll(int[] array1, int[] array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        }
        int[] joinedArray = new int[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    public static long[] addAll(long[] array1, long[] array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        }
        long[] joinedArray = new long[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    public static Object[] addAll(Object[] array1, Object[] array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        }
        Object[] joinedArray = (Object[]) Array.newInstance(array1.getClass().getComponentType(), array1.length
                + array2.length);
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    public static int[] clone(int[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    public static long[] clone(long[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    public static Object[] clone(Object[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    public static byte[] toByteArray(int[] val) {
        byte[] result = new byte[val.length];
        for (int i = 0; i < val.length; i++)
            result[i] = (byte) val[i];
        return result;
    }
    
    public static int[] toUnsignedIntArray(byte[] val) {
        int[] result = new int[val.length];
        for (int i = 0; i < val.length; i++)
            result[i] = val[i] & 0xff;
        return result;
    }
}
