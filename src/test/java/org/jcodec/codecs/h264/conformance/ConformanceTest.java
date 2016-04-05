package org.jcodec.codecs.h264.conformance;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.platform.Platform;
import org.junit.Test;

import js.io.BufferedInputStream;
import js.io.File;
import js.io.FileInputStream;
import js.io.IOException;
import js.io.InputStream;
import js.lang.System;
import js.nio.ByteBuffer;
import js.util.ArrayList;
import js.util.List;

import static java.lang.Integer.parseInt;
import static js.util.Collections.singletonList;
import static org.jcodec.common.Assert.assertTrue;

public class ConformanceTest {

    @Test
    public void testNoContainer() throws IOException {
        String dir = "src/test/resources/video/seq_h264_4";
        String yuv = "src/test/resources/video/seq_h264_4.yuv";

        String info = Platform.stringFromBytes(readFile(dir + "/info.txt").array());
        System.out.println("info "+info);
        int width = parseInt(info.split(" ")[0]);
        int height = parseInt(info.split(" ")[1]);
        int frameCount = parseInt(info.split(" ")[2]);

        byte[][] picData = Picture8Bit.create(width, height, ColorSpace.YUV420J).getData();

        H264Decoder decoder = new H264Decoder();
        decoder.addSps(singletonList(readFile(dir + "/sps")));
        decoder.addPps(singletonList(readFile(dir + "/pps")));

        RawReader8Bit rawReader = new RawReader8Bit(new File(yuv), width, height);

//        OutputStream out = new FileOutputStream(dir + "/yuv");

        for (int fn = 0; fn < frameCount; fn++) {
            System.out.println("fn "+fn);
            ByteBuffer buf = readFile(dir + "/" + zeroPad3(fn));
            Picture8Bit pic = decoder.decodeFrame8BitFromNals(extractNALUnits(buf), picData);

//            out.write(picData[0]);
//            out.write(picData[1]);
//            out.write(picData[2]);

//            Utils.saveImage(pic, "png", dir + "/" + zeroPad3(fn) + "_.png");

            Picture8Bit ref = rawReader.readNextFrame();
            assertTrue("frame=" + fn + " FAILED", compare(ref, pic));
        }

//        out.close();
    }

    private ByteBuffer readFile(String path) throws IOException {
        File file = new File(path);
        InputStream _in = new BufferedInputStream(new FileInputStream(file));
        byte[] buf = new byte[(int) file.length()];
        _in.read(buf);
        _in.close();
        return ByteBuffer.wrap(buf);
    }

    private String zeroPad3(int n) {
        String s = n + "";
        while (s.length() < 3)
            s = "0" + s;
        return s;
    }

    private List<ByteBuffer> extractNALUnits(ByteBuffer buf) {
        buf = buf.duplicate();
        List<ByteBuffer> nalUnits = new ArrayList<ByteBuffer>();

        while (buf.remaining() > 4) {
            int length = buf.getInt();
            ByteBuffer nalUnit = ByteBuffer.allocate(length);
            for (int i = 0; i < length; i++) {
                nalUnit.put(buf.get());
            }
            nalUnit.flip();
            nalUnits.add(nalUnit);
        }

        return nalUnits;
    }

    private static boolean compare(Picture8Bit expected, Picture8Bit actual) {
        int size = expected.getWidth() * expected.getHeight();

        byte[] expY = expected.getPlaneData(0);
        byte[] actY = actual.getPlaneData(0);
        for (int i = 0; i < size; i++) {
            if (expY[i] != actY[i])
                return false;
        }

        byte[] expCb = expected.getPlaneData(1);
        byte[] actCb = actual.getPlaneData(1);
        for (int i = 0; i < (size >> 2); i++) {
            if (expCb[i] != actCb[i])
                return false;
        }

        byte[] expCr = expected.getPlaneData(2);
        byte[] actCr = actual.getPlaneData(2);
        for (int i = 0; i < (size >> 2); i++) {
            if (expCr[i] != actCr[i])
                return false;
        }

        return true;
    }
}
