package org.jcodec.common;

import static java.lang.System.arraycopy;

import java.lang.StringBuilder;
import java.lang.System;
import java.lang.reflect.Array;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class ArrayUtil {

    public static <T> void shiftRight1(T[] array) {
        for (int i = 1; i < array.length; i++) {
            array[i] = array[i - 1];
        }
        array[0] = null;
    }

    public static <T> void shiftLeft1(T[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            array[i] = array[i + 1];
        }
        array[array.length - 1] = null;
    }

    public static <T> void shiftRight3(T[] array, int from, int to) {
        for (int i = to - 1; i > from; i--) {
            array[i] = array[i - 1];
        }
        array[from] = null;
    }

    public static <T> void shiftLeft3(T[] array, int from, int to) {
        for (int i = from; i < to - 1; i++) {
            array[i] = array[i + 1];
        }
        array[to - 1] = null;
    }

    public static <T> void shiftLeft2(T[] array, int from) {
        shiftLeft3(array, from, array.length);
    }

    public static <T> void shiftRight2(T[] array, int to) {
        shiftRight3(array, 0, to);
    }

    public static final void swap(int[] arr, int ind1, int ind2) {
        if (ind1 == ind2)
            return;
        int tmp = arr[ind1];
        arr[ind1] = arr[ind2];
        arr[ind2] = tmp;
    }

    public static final int sumInt(int[] array) {
        int result = 0;
        for (int i = 0; i < array.length; i++) {
            result += array[i];
        }
        return result;
    }

    public static final int sumByte(byte[] array) {
        int result = 0;
        for (int i = 0; i < array.length; i++) {
            result += array[i];
        }
        return result;
    }

    public static int sumInt3(int[] array, int from, int count) {
        int result = 0;
        for (int i = from; i < from + count; i++) {
            result += array[i];
        }
        return result;
    }

    public static int sumByte3(byte[] array, int from, int count) {
        int result = 0;
        for (int i = from; i < from + count; i++) {
            result += array[i];
        }
        return result;
    }

    public static void addInt(int[] array, int val) {
        for (int i = 0; i < array.length; i++)
            array[i] += val;
    }

    public static int[] addAllInt(int[] array1, int[] array2) {
        if (array1 == null) {
            return cloneInt(array2);
        } else if (array2 == null) {
            return cloneInt(array1);
        }
        int[] joinedArray = new int[array1.length + array2.length];
        arraycopy(array1, 0, joinedArray, 0, array1.length);
        arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    public static long[] addAllLong(long[] array1, long[] array2) {
        if (array1 == null) {
            return cloneLong(array2);
        } else if (array2 == null) {
            return cloneLong(array1);
        }
        long[] joinedArray = new long[array1.length + array2.length];
        arraycopy(array1, 0, joinedArray, 0, array1.length);
        arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    public static Object[] addAllObj(Object[] array1, Object[] array2) {
        if (array1 == null) {
            return cloneObj(array2);
        } else if (array2 == null) {
            return cloneObj(array1);
        }
        Object[] joinedArray = (Object[]) Array.newInstance(array1.getClass().getComponentType(), array1.length
                + array2.length);
        arraycopy(array1, 0, joinedArray, 0, array1.length);
        arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    public static int[] cloneInt(int[] array) {
        if (array == null) {
            return null;
        }
        return (int[]) array.clone();
    }

    public static long[] cloneLong(long[] array) {
        if (array == null) {
            return null;
        }
        return (long[]) array.clone();
    }

    public static Object[] cloneObj(Object[] array) {
        if (array == null) {
            return null;
        }
        return (Object[]) array.clone();
    }

    public static byte[] toByteArrayShifted(int[] array) {
        byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++)
            result[i] = (byte) (array[i] - 128);
        return result;
    }
    
    public static byte[][] toByteArrayShifted2(int[][] intArray) {
        byte[][] result = new byte[intArray.length][];
        for (int i = 0; i < intArray.length; i++) {
            result[i] = toByteArrayShifted(intArray[i]);
        }
        return result;
    }

    public static int[] toIntArrayUnshifted(byte[] bytes) {
        int[] result = new int[bytes.length];
        for (int i = 0; i < result.length; i++)
            result[i] = (byte) (bytes[i] + 128);

        return result;
    }

    public static byte[] toByteArray(int[] ints) {
        byte[] result = new byte[ints.length];
        for (int i = 0; i < ints.length; i++)
            result[i] = (byte) ints[i];
        return result;
    }

    public static int[] toIntArray(byte[] bytes) {
        int[] result = new int[bytes.length];
        for (int i = 0; i < result.length; i++)
            result[i] = bytes[i];

        return result;
    }

    public static int[] toUnsignedIntArray(byte[] bytes) {
        int[] result = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            result[i] = bytes[i] & 0xff;
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

    public static void printMatrixBytes(byte[] array, String format, int width) {
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
                    builder.append(" ");
                builder.append(strings[ind]);
            }
            System.out.println(builder);
        }
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
                    builder.append(" ");
                builder.append(strings[ind]);
            }
            System.out.println(builder);
        }
    }

    public static byte[] padLeft(byte[] array, int padLength) {
        byte[] result = new byte[array.length + padLength];
        for (int i = padLength; i < result.length; i++)
            result[i] = array[i - padLength];
        return result;
    }

    public static int[] randomIntArray(int size, int from, int to) {
        int width = to - from;
        int[] result = new int[size];
        for (int i = 0; i < size; i++)
            result[i] = (int) ((Math.random() * width) % width) + from;
        return result;
    }

    public static byte[] randomByteArray(int size, byte from, byte to) {
        byte width = (byte) (to - from);
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++)
            result[i] = (byte) (((Math.random() * width) % width) + from);
        return result;
    }

    /**
     * Implements a quicksort algorithm
     */
    public static void quickSort(int[] a, int start, int end, int[] p) {
        int len = end - start;
        if (len < 2) {
            return;
        } else {
            int startPlus1 = start + 1;
            if (len == 2) {
                if (a[start] > a[startPlus1]) {
                    swap(a, start, startPlus1);
                    if (p != null)
                        swap(p, start, startPlus1);
                }
                return;
            } else if (len == 3) {
                if (a[start] > a[startPlus1]) {
                    swap(a, start, startPlus1);
                    if (p != null)
                        swap(p, start, startPlus1);
                }
                int startPlus2 = start + 2;
                if (a[startPlus1] > a[startPlus2]) {
                    swap(a, startPlus1, startPlus2);
                    if (p != null)
                        swap(p, startPlus1, startPlus2);
                }
                if (a[start] > a[startPlus1]) {
                    swap(a, start, startPlus1);
                    if (p != null)
                        swap(p, start, startPlus1);
                }
            }
        }
        int pivot = a[0];

        // partially sort
        int p_large = end - 1;
        for (int i = end - 1; i >= start; i--) {
            if (a[i] > pivot) {
                swap(a, i, p_large);
                if (p != null)
                    swap(p, i, p_large);
                p_large--;
            }
        }
        swap(a, start, p_large);
        if (p != null)
            swap(p, start, p_large);

        quickSort(a, start, p_large, p);
        quickSort(a, p_large + 1, end, p);
    }

    public static int[] flatten5D(int[][][][][] is) {
        IntArrayList list = new IntArrayList(128);
        flatten5DL(is, list);
        return list.toArray();
    }

    public static int[] flatten4D(int[][][][] is) {
        IntArrayList list = new IntArrayList(128);
        flatten4DL(is, list);
        return list.toArray();
    }

    public static int[] flatten3D(int[][][] is) {
        IntArrayList list = new IntArrayList(128);
        flatten3DL(is, list);
        return list.toArray();
    }

    public static int[] flatten2D(int[][] is) {
        IntArrayList list = new IntArrayList(128);
        flatten2DL(is, list);
        return list.toArray();
    }

    private static void flatten5DL(int[][][][][] is, IntArrayList list) {
        for (int i = 0; i < is.length; i++) {
            flatten4DL(is[i], list);
        }
    }

    private static void flatten4DL(int[][][][] is, IntArrayList list) {
        for (int i = 0; i < is.length; i++) {
            flatten3DL(is[i], list);
        }
    }

    private static void flatten3DL(int[][][] is, IntArrayList list) {
        for (int i = 0; i < is.length; i++) {
            flatten2DL(is[i], list);
        }
    }

    private static void flatten2DL(int[][] is, IntArrayList list) {
        for (int i = 0; i < is.length; i++) {
            flatten1DL(is[i], list);
        }
    }

    private static void flatten1DL(int[] is, IntArrayList list) {
        for (int i = 0; i < is.length; i++) {
            list.add(is[i]);
        }
    }

    public static void copy6D(int[][][][][][] to, int[][][][][][] from) {
        for (int i = 0; i < Math.min(to.length, from.length); i++) {
            copy5D(to[i], from[i]);
        }
    }

    public static void copy5D(int[][][][][] to, int[][][][][] from) {
        for (int i = 0; i < Math.min(to.length, from.length); i++) {
            copy4D(to[i], from[i]);
        }
    }

    public static void copy4D(int[][][][] to, int[][][][] from) {
        for (int i = 0; i < Math.min(to.length, from.length); i++) {
            copy3D(to[i], from[i]);
        }
    }

    public static void copy3D(int[][][] to, int[][][] from) {
        for (int i = 0; i < Math.min(to.length, from.length); i++) {
            copy2D(to[i], from[i]);
        }
    }

    public static void copy2D(int[][] to, int[][] from) {
        for (int i = 0; i < Math.min(to.length, from.length); i++) {
            copy1D(to[i], from[i]);
        }
    }

    public static void copy1D(int[] to, int[] from) {
        for (int i = 0; i < Math.min(to.length, from.length); i++) {
            to[i] = from[i];
        }
    }

    public static int fill6D(int[][][][][][] to, int[] from, int index) {
        for (int i = 0; i < to.length; i++) {
            index = fill5D(to[i], from, index);
        }
        return index;
    }

    public static int fill5D(int[][][][][] to, int[] from, int index) {
        for (int i = 0; i < to.length; i++) {
            index = fill4D(to[i], from, index);
        }
        return index;
    }

    public static int fill4D(int[][][][] to, int[] from, int index) {
        for (int i = 0; i < to.length; i++) {
            index = fill3D(to[i], from, index);
        }
        return index;
    }

    public static int fill3D(int[][][] to, int[] from, int index) {
        for (int i = 0; i < to.length; i++) {
            index = fill2D(to[i], from, index);
        }
        return index;
    }

    public static int fill2D(int[][] to, int[] from, int index) {
        for (int i = 0; i < to.length; i++) {
            index = fill1D(to[i], from, index);
        }
        return index;
    }

    public static int fill1D(int[] to, int[] from, int index) {
        for (int i = 0; i < to.length; i++) {
            to[i] = from[index++];
        }
        return index;
    }

    public static int fill6D(short[][][][][][] to, short[] from, int index) {
        for (int i = 0; i < to.length; i++) {
            index = fill5D(to[i], from, index);
        }
        return index;
    }

    public static int fill5D(short[][][][][] to, short[] from, int index) {
        for (int i = 0; i < to.length; i++) {
            index = fill4D(to[i], from, index);
        }
        return index;
    }

    public static int fill4D(short[][][][] to, short[] from, int index) {
        for (int i = 0; i < to.length; i++) {
            index = fill3D(to[i], from, index);
        }
        return index;
    }

    public static int fill3D(short[][][] to, short[] from, int index) {
        for (int i = 0; i < to.length; i++) {
            index = fill2D(to[i], from, index);
        }
        return index;
    }

    public static int fill2D(short[][] to, short[] from, int index) {
        for (int i = 0; i < to.length; i++) {
            index = fill1D(to[i], from, index);
        }
        return index;
    }

    public static int fill1D(short[] to, short[] from, int index) {
        for (int i = 0; i < to.length; i++) {
            to[i] = from[index++];
        }
        return index;
    }

    
    public static int[] flatten2DL(List<int[]> sampleSizes) {
        int size = 0;
        for (int[] is : sampleSizes) {
            size += is.length;
        }
        int idx = 0;
        int[] ret = new int[size];
        for (int[] is : sampleSizes) {
            for (int i = 0; i < is.length; i++, idx ++) {
                ret[idx] = is[i];
            }
        }
        return ret;
    }
    // 116, 114, 97, 107
    public static int find(byte[] bytes, int off, byte[] needle) {
        for (int i = off; i < bytes.length; i++) {
            boolean eq = true;
            int j = 0;
            int min = Math.min(needle.length, bytes.length - i);
            for (; j < min; j++) {
                eq &= bytes[i + j] == needle[j];
            }
            if (eq && j == min)
                return i;
        }
        return -1;
    }

    public static int instances(byte[] bytes, byte[] bs) {
        int idx = 0;
        int count = 0;
        while ((idx = find(bytes, idx + bs.length, bs)) != -1) {
            count++;
        }
        return count;
    }
}
