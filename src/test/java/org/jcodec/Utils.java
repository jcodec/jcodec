package org.jcodec;

import java.io.File;

import org.jcodec.common.ArrayUtil;
import org.jcodec.common.tools.MathUtil;
import org.junit.Assert;

public class Utils {

    public static File tildeExpand(String path) {
        if (path.startsWith("~")) {
            path = path.replaceFirst("~", System.getProperty("user.home"));
        }
        return new File(path);
    }

    public static void printArray(int[] array, String... title) {
        if (title.length > 0)
            System.out.println(title[0]);

        int max = ArrayUtil.max(array);
        // Approx digits needed, 2^10 ~ 10^3
        int digits = Math.max(1, (3 * MathUtil.log2(max)) / 10);

        for (int i = 0; i < array.length; i++) {
            System.out.print(String.format("%0" + digits + "d", array[i]));
            if (i < array.length - 1)
                System.out.print(",");
        }
        System.out.println();
    }

    public static void printArrayHex(int[] array, String... title) {
        if (title.length > 0)
            System.out.println(title[0]);

        int max = ArrayUtil.max(array);
        // One hex digit = 2^4
        int digits = Math.max(2, MathUtil.log2(max) >> 2);

        for (int i = 0; i < array.length; i++) {
            System.out.print(String.format("%0" + digits + "x", array[i]));
            if (i < array.length - 1)
                System.out.print(",");
        }
        System.out.println();
    }

    public static void printArray(int[][] array, String... title) {
        if (title.length > 0)
            System.out.println(title[0]);
        for (int i = 0; i < array.length; i++) {
            printArray(array[i]);
        }
        System.out.println();
    }

    public static void assertArrayEquals(int[][] is, int[][] v) {
        Assert.assertEquals(is.length, v.length);
        for (int i = 0; i < is.length; i++) {
            Assert.assertArrayEquals(is[i], v[i]);
        }
    }

}
