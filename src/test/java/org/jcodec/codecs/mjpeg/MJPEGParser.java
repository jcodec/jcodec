package org.jcodec.codecs.mjpeg;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PushbackInputStream;

import org.apache.commons.io.input.CountingInputStream;
import org.jcodec.codecs.h264.JAVCTestCase;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Jay Codec
 *
 */
public class MJPEGParser extends JAVCTestCase {
    public static void main(String[] args) throws Exception {
        new MJPEGParser().testPerformance();
    }

    @Test
    public void testParse() throws Exception {
        JpegDecoder dec = new JpegDecoder();
        JpegParser parser = new JpegParser();
        InputStream is = new BufferedInputStream(new FileInputStream(
                new File("src/test/resources/jpeg/frames0-6.mjpg")));
        CodedImage codedImage = parser.parse(is);
        DecodedImage decode = new DecodedImage(codedImage.getWidth(),
                codedImage.getHeight());
        dec.decode(codedImage, decode);
        for (int i = 1; i < 5; i++) {
            System.out.println(i);
            codedImage = parser.parse(is);
            dec.decode(codedImage, decode);
        }
    }

    @Test
    @Ignore
    public void testPerformance() throws Exception {
        JpegDecoder dec = new JpegDecoder();
        JpegParser parser = new JpegParser();
        InputStream is = new BufferedInputStream(new FileInputStream(
                "/Users/zhukov/Movies/MVI_0636.mjpg"));
        CountingInputStream counter = new CountingInputStream(is);
        PushbackInputStream push = new PushbackInputStream(counter, 2);
        CodedImage codedImage = parser.parse(is);
        DecodedImage decode = new DecodedImage(codedImage.getWidth(),
                codedImage.getHeight());
        dec.decode(codedImage, decode);
        long start = System.currentTimeMillis();
        for (int i = 1; i < 2; i++) {
            if (i % 10 == 0) {
                long time = System.currentTimeMillis() - start;
                long fps = i * 1000 / time;
                System.out.println(i + " " + fps + "fps");
            }
            codedImage = parser.parse(push, counter);
            dec.decode(codedImage, decode);
        }

    }

    BufferedImage buffered(DecodedImage decoded) {
        int w = decoded.getWidth();
        int h = decoded.getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        bi.setRGB(0, 0, w, h, decoded.getPixels(), 0, w);
        return bi;

    }
}
