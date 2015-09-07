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

    public static final void swap(int[] arr, int ind1, int ind2) {
        int tmp = arr[ind1];
        arr[ind1] = arr[ind2];
        arr[ind2] = tmp;
    }

    public static final int sum(int[] array) {
        int result = 0;
        for (int i = 0; i < array.length; i++) {
            result += array[i];
        }
        return result;
    }

    public static final int sum(byte[] array) {
        int result = 0;
        for (int i = 0; i < array.length; i++) {
            result += array[i];
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

    public static int sum(byte[] array, int from, int count) {
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
        return (int[]) array.clone();
    }

    public static long[] clone(long[] array) {
        if (array == null) {
            return null;
        }
        return (long[]) array.clone();
    }

    public static Object[] clone(Object[] array) {
        if (array == null) {
            return null;
        }
        return (Object[]) array.clone();
    }

    public static byte[] toByteArrayShifted(int... val) {
        byte[] result = new byte[val.length];
        for (int i = 0; i < val.length; i++)
            result[i] = (byte) (val[i] - 128);
        return result;
    }

    public static int[] toIntArrayUnshifted(byte... array) {
        int[] result = new int[array.length];
        for (int i = 0; i < result.length; i++)
            result[i] = (byte) (array[i] + 128);

        return result;
    }

    public static byte[] toByteArray(int... val) {
        byte[] result = new byte[val.length];
        for (int i = 0; i < val.length; i++)
            result[i] = (byte) val[i];
        return result;
    }

    public static int[] toIntArray(byte... array) {
        int[] result = new int[array.length];
        for (int i = 0; i < result.length; i++)
            result[i] = array[i] & 0xff;

        return result;
    }

    public static int[] toUnsignedIntArray(byte[] val) {
        int[] result = new int[val.length];
        for (int i = 0; i < val.length; i++)
            result[i] = val[i] & 0xff;
        return result;
    }

    public static <T> void reverse(T[] frames) {
        for (int i = 0, j = frames.length - 1; i < frames.length >> 1; ++i, --j) {
            T tmp = frames[i];
            frames[i] = frames[j];
            frames[j] = tmp;
        }
    }

    public static int max(int[] array) {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }

        return max;
    }

    public static int[][] rotate(int[][] src) {
        int[][] dst = new int[src[0].length][src.length];
        for (int i = 0; i < src.length; i++) {
            for (int j = 0; j < src[0].length; j++) {
                dst[j][i] = src[i][j];
            }
        }
        return dst;
    }

    public static byte[][] create2D(int width, int height) {
        byte[][] result = new byte[height][];
        for (int i = 0; i < height; i++)
            result[i] = new byte[width];
        return result;
    }

    public static void printMatrix(int[] array, String format, int width) {
        String[] strings = new String[array.length];
        int maxLen = 0;
        for (int i = 0; i < array.length; i++) {
            strings[i] = String.format(format, array[i]);
            maxLen = Math.max(maxLen, strings[i].length());
        }
        for (int ind = 0; ind < strings.length;) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < width && ind < strings.length; i++, ind++) {
                for (int j = 0; j < maxLen - strings[ind].length() + 1; j++)
                    builder.append(' ');
                builder.append(strings[ind]);
            }
            System.out.println(builder);
        }
    }

    public static byte[] padLeft(byte[] array, int padLength) {
        byte[] result = new byte[array.length + padLength];
        for(int i = padLength; i < result.length; i++)
            result[i] = array[i - padLength];
        return result;
    }
}
