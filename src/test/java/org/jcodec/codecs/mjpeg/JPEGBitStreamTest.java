package org.jcodec.codecs.mjpeg;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

import junit.framework.Assert;

import org.jcodec.common.tools.MD5;
import org.junit.Test;

public class JPEGBitStreamTest {

    static class TestData {

        private int y0dc;
        private int y1dc;
        private int crdc;
        private int cbdc;
        private String y0md5;
        private String y1md5;
        private String crmd5;
        private String cbmd5;

        static TestData fromBlock(MCU block) {
            TestData data = new TestData();
            data.y0dc = block.lum.data[0][0];
            data.y1dc = block.lum.data[1][0];
            data.crdc = block.cr.data[0][0];
            data.cbdc = block.cr.data[0][0];
            data.y0md5 = md5(block.lum.data[0]);
            data.y1md5 = md5(block.lum.data[1]);
            data.crmd5 = md5(block.cr.data[0]);
            data.cbmd5 = md5(block.cb.data[0]);
            return data;
        }

        static TestData fromString(String s) {
            TestData data = new TestData();
            String[] split = s.split(" ");
            data.y0dc = Integer.parseInt(split[0]);
            data.y1dc = Integer.parseInt(split[1]);
            data.crdc = Integer.parseInt(split[2]);
            data.cbdc = Integer.parseInt(split[3]);
            data.y0md5 = split[4];
            data.y1md5 = split[5];
            data.crmd5 = split[6];
            data.cbmd5 = split[7];
            return data;
        }

        public String toString() {
            return String.format("%d %d %d %d %s %s %s %s", y0dc, y1dc, crdc,
                    cbdc, y0md5, y1md5, crmd5, cbmd5);
        }

        public boolean equals(Object obj) {
            TestData o = (TestData) obj;
            return toString().equals(o.toString());
        }

    }

    @Test
    public void testReadDCValue() throws Exception {
        CodedImage codedImage = new JpegParser().parse(new BufferedInputStream(
                new FileInputStream("src/test/resources/frame0.mjpg")));
        MCU block = MCU.create(codedImage.frame);
        JPEGBitStream jbs = new JPEGBitStream(codedImage);

        String fname = "src/test/resources/frame0_test.txt";
        BufferedReader reader = reader(fname);
        for (int i = 0; i < 2400; i++) {
            jbs.readBlock(block);
            TestData expected = TestData.fromString(reader.readLine());
            TestData actual = TestData.fromBlock(block);
            Assert.assertEquals(expected, actual);
        }
        reader.close();
    }

    private BufferedReader reader(String fname) throws FileNotFoundException {
        return new BufferedReader(new InputStreamReader(
                new BufferedInputStream(new FileInputStream(fname))));
    }

    static ByteBuffer buf = ByteBuffer.allocate(64 * 4);

    private static String md5(int[] array) {
        buf.clear();
        buf.asIntBuffer().put(array);
        return MD5.md5sum(buf);
    }
}
