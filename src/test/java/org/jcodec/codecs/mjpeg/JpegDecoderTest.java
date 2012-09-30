package org.jcodec.codecs.mjpeg;

import static junit.framework.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.jcodec.codecs.mjpeg.tools.Asserts;
import org.jcodec.common.dct.IntDCT;
import org.jcodec.common.dct.SlowDCT;
import org.junit.Test;

public class JpegDecoderTest {
    @Test
    public void testQuantTables() throws Exception {
        int[] expectedQLum = new int[] {

        4, 3, 3, 4, 5, 7, 8, 10,

        3, 3, 3, 4, 5, 6, 8, 8,

        3, 3, 3, 4, 6, 8, 10, 13,

        4, 4, 4, 7, 8, 10, 13, 18,

        5, 5, 6, 8, 11, 14, 18, 20,

        7, 6, 8, 10, 14, 18, 20, 20,

        8, 8, 10, 13, 18, 20, 20, 20,

        10, 8, 13, 18, 20, 20, 20, 20

        };
        int[] expectedQChrom = new int[] {

        5, 5, 8, 15, 18, 22, 20, 20,

        5, 7, 10, 15, 21, 20, 20, 20,

        8, 10, 12, 21, 20, 20, 20, 20,

        15, 15, 21, 20, 20, 20, 20, 20,

        18, 21, 20, 20, 20, 20, 20, 20,

        22, 20, 20, 20, 20, 20, 20, 20,

        22, 20, 20, 20, 20, 20, 20, 20,

        20, 20, 20, 20, 20, 20, 20, 20

        };
        CodedImage coded = parse(new File("src/test/resources/test1.jpg"));
        assertTrue(Arrays.equals(expectedQLum, coded.getQuantLum().getValues()));
        assertTrue(Arrays.equals(expectedQChrom, coded.getQuantChrom()
                .getValues()));
    }

    @Test
    public void testDecompress444() throws IOException {
        String expected = "src/test/resources/test1.bmp";
        String testjpg = "src/test/resources/test1.jpg";
        testDecode(expected, testjpg);
    }

    private void testDecode(String expected, String testjpg) throws IOException {
        byte[] expectedData = getData(read(expected));
        JpegDecoder.dct = SlowDCT.INSTANCE;
        BufferedImage decode = decode(new File(testjpg));
        byte[] actualData = getData(decode);
        assertTrue(Arrays.equals(expectedData, actualData));

        JpegDecoder.dct = IntDCT.INSTANCE;
        actualData = getData(decode);
        Asserts.assertEpsilonEquals(expectedData, actualData, 3);
    }

    public void testNewExpectedOut() throws Exception {
        JpegDecoder.dct = SlowDCT.INSTANCE;
        BufferedImage bi = decode(new File("src/test/resources/fr.jpg"));
        ImageIO.write(bi, "bmp", new File("test1.bmp"));
    }

    @Test
    public void testDecompress420() throws Exception {
        String expected = "src/test/resources/fr.bmp";
        String testjpg = "src/test/resources/fr.jpg";
        testDecode(expected, testjpg);
    }

    @Test
    public void testHuffTable() throws Exception {
        CodedImage parsed = parse(new File("src/test/resources/fr.jpg"));
        parsed.getYdc().printTable(System.out);
        parsed.getYac().printTable(System.out);
        parsed.getCac().printTable(System.out);
        parsed.getCdc().printTable(System.out);
    }

    private CodedImage parse(File fname) throws FileNotFoundException,
            IOException {
        CodedImage coded;
        InputStream in = new BufferedInputStream(new FileInputStream(fname));
        coded = new JpegParser().parse(in);
        in.close();
        return coded;
    }

    @Test
    public void testDecompress422() throws Exception {
        String expected = "src/test/resources/olezha.bmp";
        String testjpg = "src/test/resources/olezha422.jpg";
        testDecode(expected, testjpg);
    }

    private BufferedImage decode(File filename) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(filename));
        try {
            DecodedImage decoded = JpegDecoder.doIt(is);
            int w = decoded.getWidth();
            int h = decoded.getHeight();
            BufferedImage bi = new BufferedImage(w, h,
                    BufferedImage.TYPE_3BYTE_BGR);
            bi.setRGB(0, 0, w, h, decoded.getPixels(), 0, w);
            return bi;
        } finally {
            is.close();
        }
    }

    private BufferedImage read(String filename) throws IOException {
        return ImageIO.read(new File(filename));
    }

    private byte[] getData(BufferedImage bi) {
        return ((DataBufferByte) bi.getData().getDataBuffer()).getData();
    }

}
