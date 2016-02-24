package org.jcodec;

import java.io.File;
import java.io.IOException;

import org.jcodec.api.FrameGrab8Bit;
import org.jcodec.api.JCodecException;
import org.jcodec.common.ArrayUtil;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture8Bit;
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
    
    public static void compareMP4H264Files(File file, File refFile) throws IOException, JCodecException {
        FileChannelWrapper ch1 = null, ch2 = null;
        try {
            ch1 = NIOUtils.readableFileChannel(file);
            ch2 = NIOUtils.readableFileChannel(refFile);
            FrameGrab8Bit frameGrab1 = new FrameGrab8Bit(ch1);
            FrameGrab8Bit frameGrab2 = new FrameGrab8Bit(ch2);

            Picture8Bit fr1, fr2;
            do {
                fr1 = frameGrab1.getNativeFrame();
                fr2 = frameGrab2.getNativeFrame();
                if (fr1 == null || fr2 == null)
                    break;

                fr1 = fr1.cropped();
                fr2 = fr2.cropped();

                Assert.assertEquals(fr2.getWidth(), fr1.getWidth());
                Assert.assertEquals(fr2.getHeight(), fr1.getHeight());

                for (int i = 0; i < fr2.getData().length; i++)
                    assertArrayApproximatelyEquals(fr2.getPlaneData(i), fr1.getPlaneData(i), 20);
            } while (fr1 != null && fr2 != null);

            Assert.assertNull(fr1);
            Assert.assertNull(fr2);
        } finally {
            IOUtils.closeQuietly(ch1);
            IOUtils.closeQuietly(ch2);
        }
    }

    public static void assertArrayApproximatelyEquals(byte[] rand, byte[] newRand, int threash) {
        int maxDiff = 0;
        for (int i = 0; i < rand.length; i++) {
            int diff = Math.abs(rand[i] - newRand[i]);
            if (diff > maxDiff)
                maxDiff = diff;
        }
        Assert.assertTrue("Maxdiff: " + maxDiff, maxDiff < threash);
    }

}
