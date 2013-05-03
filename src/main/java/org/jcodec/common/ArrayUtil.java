package org.jcodec.common;

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
}
