package org.jcodec;

import static org.jcodec.common.tools.MathUtil.abs;
import static org.jcodec.common.tools.MathUtil.clipMax;
import static org.junit.Assert.assertTrue;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.ArrayUtil;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;
import org.jcodec.scale.AWTUtil;
import org.junit.Assert;

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

    public static void compareMP4H264Files(File file, File refFile) throws IOException, JCodecException {
        FileChannelWrapper ch1 = null, ch2 = null;
        try {
            ch1 = NIOUtils.readableChannel(file);
            ch2 = NIOUtils.readableChannel(refFile);
            FrameGrab frameGrab1 = FrameGrab.createFrameGrab(ch1);
            FrameGrab frameGrab2 = FrameGrab.createFrameGrab(ch2);

            Picture fr1, fr2;
            do {
                fr1 = frameGrab1.getNativeFrame();
                fr2 = frameGrab2.getNativeFrame();
                if (fr1 == null || fr2 == null)
                    break;

                fr1 = fr1.cropped();
                fr2 = fr2.cropped();

                assertTrue(picturesRoughlyEqual(fr1, fr2, 20));
            } while (fr1 != null && fr2 != null);

            Assert.assertNull(fr1);
            Assert.assertNull(fr2);
        } finally {
            IOUtils.closeQuietly(ch1);
            IOUtils.closeQuietly(ch2);
        }
    }

    public static int maxDiff(Picture fr1, Picture fr2) {
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

    public static boolean picturesRoughlyEqual(Picture fr1, Picture fr2, int threshold) {
        if (fr2.getWidth() != fr1.getWidth() || fr2.getHeight() != fr1.getHeight())
            return false;

        for (int i = 0; i < fr2.getData().length; i++) {
            int avgDiff = findAvgDiff(fr2.getPlaneData(i), fr1.getPlaneData(i));
            if (avgDiff > 0) {
                Logger.warn("Avg diff: " + avgDiff);
            }
            if (avgDiff > threshold) {
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
    
    public static int findAvgDiff(byte[] one, byte[] two) {
        int totalDiff = 0;
        for (int i = 0; i < one.length; i++) {
            totalDiff += Math.abs(one[i] - two[i]);
        }
        return totalDiff / one.length;
    }

    public static Picture readYuvFrame(ReadableByteChannel ch, int width, int height) throws IOException {
        Picture result = Picture.create(width, height, ColorSpace.YUV420J);

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

    public static void saveImage(Picture fr2, String formatName, String name) throws IOException {
        ImageIO.write(AWTUtil.toBufferedImage(fr2), formatName, new File(name));
    }

    public static Picture diff(Picture one, Picture two, int mul) {
        Picture result = Picture.create(one.getWidth(), one.getHeight(), one.getColor());
        byte[][] dataO = one.getData();
        byte[][] dataT = two.getData();
        byte[][] dataR = result.getData();
        Arrays.fill(dataR[1], (byte) 64);
        for (int i = 0; i < dataO[0].length; i++) {
            dataR[0][i] = (byte) (clipMax(abs(dataO[0][i] - dataT[0][i]) * mul, 255) - 128);
        }

        return result;
    }

    public static BufferedImage scale(BufferedImage source, int width, int height) {
        AffineTransform at = AffineTransform.getScaleInstance(((double) width) / source.getWidth(), ((double) height)
                / source.getHeight());
        AffineTransformOp bilinearScaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);

        return bilinearScaleOp.filter(
            source,
            new BufferedImage(width, height, source.getType()));
    }
    
    private static double clampVector(double d) {
        return d < 0 ? 0 : (d > 1 ? 1 : d);
    }
    
    public static Picture buildSmoothRandomPic(int width, int height, int smooth, double vectorSmooth) {
        Picture pic = Picture.create(width, height, ColorSpace.YUV420);
        int[] prevRow = new int[width];
        double vector = Math.random();
        for (int p = 0; p < pic.getColor().nComp; p++) {
            int val = (int) (Math.random() * 255);
            pic.getPlaneData(p)[0] = (byte) (val - 128);
            prevRow[0] = val;

            for (int i = 1; i < pic.getPlaneData(p).length; i++) {
                int predInd = i % pic.getPlaneWidth(p);
                int pred;
                if (i < pic.getPlaneWidth(p)) {
                    pred = prevRow[predInd - 1];
                } else if (predInd == 0) {
                    pred = prevRow[predInd];
                } else {
                    vector = clampVector(vector + Math.random() * vectorSmooth - vectorSmooth / 2);
                    pred = (int) (prevRow[predInd] * vector + prevRow[predInd - 1] * (1 - vector));
                }
                int delta = (int) (Math.random() * smooth) - (pred * smooth) / 256;
                val = MathUtil.clip(pred + delta, 0, 255);
                pic.getPlaneData(p)[i] = (byte) (val - 128);
                prevRow[predInd] = val;
            }
        }
        return pic;
    }
}
