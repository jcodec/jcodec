package org.jcodec;

import org.jcodec.common.ArrayUtil;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MathUtil;
//import org.jcodec.scale.AWTUtil;
import org.junit.Assert;

import javax.imageio.ImageIO;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;

import static org.jcodec.common.tools.MathUtil.abs;
import static org.jcodec.common.tools.MathUtil.clipMax;

public class Utils {

    public static File tildeExpand(String path) {
        if (path.startsWith("~")) {
            path = path.replaceFirst("~", System.getProperty("user.home"));
        }
        return new File(path);
    }

    public static void printArray(int[] array, String[] title) {
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

    public static void printArrayHex(int[] array, String[] title) {
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

    public static void printArray2d(int[][] array, String[] title) {
        if (title.length > 0)
            System.out.println(title[0]);
        for (int i = 0; i < array.length; i++) {
            printArray(array[i], new String[0]);
        }
        System.out.println();
    }

    public static void assertArrayEquals(int[][] is, int[][] v) {
        Assert.assertEquals(is.length, v.length);
        for (int i = 0; i < is.length; i++) {
            Assert.assertArrayEquals(is[i], v[i]);
        }
    }

    public static int maxDiff(Picture8Bit fr1, Picture8Bit fr2) {
        if (fr2.getWidth() != fr1.getWidth() || fr2.getHeight() != fr1.getHeight())
            throw new IllegalArgumentException("Diffing pictures of different sizes.");

        int maxDiff = 0;
        for (int i = 0; i < fr2.getData().length; i++) {
            int diff = findMaxDiff(fr2.getPlaneData(i), fr1.getPlaneData(i));
            if (diff > maxDiff) {
                maxDiff = diff;
            }
        }
        return maxDiff;
    }

    public static boolean picturesRoughlyEqual(Picture8Bit fr1, Picture8Bit fr2, int threshold) {
        if (fr2.getWidth() != fr1.getWidth() || fr2.getHeight() != fr1.getHeight())
            return false;

        for (int i = 0; i < fr2.getData().length; i++) {
            int maxDiff = findMaxDiff(fr2.getPlaneData(i), fr1.getPlaneData(i));
            if (maxDiff > 0) {
                Logger.warn("Max diff: " + maxDiff);
            }
            if (maxDiff > threshold) {
                return false;
            }
        }
        return true;
    }

    public static int findMaxDiff(byte[] rand, byte[] newRand) {
        int maxDiff = 0;
        for (int i = 0; i < rand.length; i++) {
            int diff = Math.abs(rand[i] - newRand[i]);
            if (diff > maxDiff)
                maxDiff = diff;
        }
        return maxDiff;
    }

    public static Picture8Bit readYuvFrame(ReadableByteChannel ch, int width, int height) throws IOException {
        Picture8Bit result = Picture8Bit.create(width, height, ColorSpace.YUV420J);

        for (int i = 0; i < result.getData().length; i++) {
            byte[] planeData = result.getPlaneData(i);
            if (ch.read(ByteBuffer.wrap(planeData)) != result.getPlaneData(i).length) {
                throw new EOFException();
            }
            for (int j = 0; j < planeData.length; j++) {
                planeData[j] = (byte) ((planeData[j] & 0xff) - 128);
            }
        }

        return result;
    }

    public static void saveImage(Picture8Bit fr2, String formatName, String name) throws IOException {
//        ImageIO.write(AWTUtil.toBufferedImage8Bit(fr2), formatName, new File(name));
    }

    public static Picture8Bit diff(Picture8Bit one, Picture8Bit two, int mul) {
        Picture8Bit result = Picture8Bit.create(one.getWidth(), one.getHeight(), one.getColor());
        byte[][] dataO = one.getData();
        byte[][] dataT = two.getData();
        byte[][] dataR = result.getData();
        Arrays.fill(dataR[1], (byte)64);
        for (int i = 0; i < dataO[0].length; i++) {
            dataR[0][i] = (byte) (clipMax(abs(dataO[0][i] - dataT[0][i]) * mul, 255) - 128);
        }

        return result;
    }
}
