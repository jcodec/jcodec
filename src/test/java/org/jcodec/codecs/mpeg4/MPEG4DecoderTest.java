package org.jcodec.codecs.mpeg4;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.junit.Assert;
import org.junit.Test;

public class MPEG4DecoderTest {
    
    @Test
    public void testForemanIFrame() throws IOException {
        ByteBuffer bb = NIOUtils.fetchFromFile(new File("src/test/resources/frame0.m4v"));
        Picture p = Picture.create(1920, 1080, ColorSpace.YUV420J);
        Picture out = new MPEG4Decoder().decodeFrame(bb, p.getData()).cropped();
        comparePic(out, "src/test/resources/frame0.yuv");
    }

    private void comparePic(Picture out, String name) throws IOException {
        Picture ref = Picture.create(out.getWidth(), out.getHeight(), ColorSpace.YUV420);
        readYuv(ref, name);
//        Assert.assertArrayEquals(ref.getPlaneData(0), out.getPlaneData(0));
//        Assert.assertArrayEquals(ref.getPlaneData(1), out.getPlaneData(1));
//        Assert.assertArrayEquals(ref.getPlaneData(2), out.getPlaneData(2));
        assertByteArrayApproximatelyEquals(ref.getPlaneData(0), out.getPlaneData(0), 0, out.getWidth(), 0);
        assertByteArrayApproximatelyEquals(ref.getPlaneData(1), out.getPlaneData(1), 0, out.getPlaneWidth(1), 1);
        assertByteArrayApproximatelyEquals(ref.getPlaneData(2), out.getPlaneData(2), 0, out.getPlaneWidth(2), 1);
    }
    
    private void assertByteArrayApproximatelyEquals(byte[] ref, byte[] dec, int threash, int stride, int shift) {
        for (int i = 0; i < ref.length; i++) {
            int diff = Math.abs(ref[i] - dec[i]);
            if (diff > threash) {
                int col = i % stride;
                int row = i / stride;
                int logMbS = 4 - shift;
                int mbX = col >> logMbS;
                int mbY = row >> logMbS;
                Assert.assertTrue(String.format("diff=: %d@%d, mb: %d, %d, (%d, %d)", diff, i, mbX,
                        mbY, col - (mbX << logMbS), row - (mbY << logMbS)), diff <= threash);
            }
        }
    }

    private void readYuv(Picture out, String name) throws IOException {
        ByteBuffer buf = NIOUtils.fetchFromFile(new File(name));
        int lumaLen = out.getHeight() * out.getWidth();
        for(int i = 0; i < lumaLen; i++) {
            out.getPlaneData(0)[i] = (byte)((buf.get() & 0xff) - 128);
        }
        for(int i = 0; i < lumaLen/4; i++) {
            out.getPlaneData(1)[i] = (byte)((buf.get() & 0xff) - 128);
        }
        for(int i = 0; i < lumaLen/4; i++) {
            out.getPlaneData(2)[i] = (byte)((buf.get() & 0xff) - 128);
        }
    }
    
    private void saveYuv(Picture cr, String name) throws IOException {
        int lumaLen = cr.getHeight() * cr.getWidth();
        ByteBuffer buf = ByteBuffer.allocate((3 * lumaLen) / 2);
        for(int i = 0; i < lumaLen; i++) {
            buf.put((byte)(cr.getPlaneData(0)[i] + 128));
        }
        for(int i = 0; i < lumaLen/4; i++) {
            buf.put((byte)(cr.getPlaneData(1)[i] + 128));
        }
        for(int i = 0; i < lumaLen/4; i++) {
            buf.put((byte)(cr.getPlaneData(2)[i] + 128));
        }
        buf.flip();
        
        NIOUtils.writeTo(buf, new File(name));
    }
    
    @Test
    public void testForemanPFrame() throws IOException {
        ByteBuffer bb = NIOUtils.fetchFromFile(new File("src/test/resources/frame0123.m4v"));
        Picture p0 = Picture.create(1920, 1080, ColorSpace.YUV420J);
        Picture p1 = Picture.create(1920, 1080, ColorSpace.YUV420J);
        Picture p2 = Picture.create(1920, 1080, ColorSpace.YUV420J);
        Picture p3 = Picture.create(1920, 1080, ColorSpace.YUV420J);
        MPEG4Decoder decoder = new MPEG4Decoder();
        Picture out0 = decoder.decodeFrame(bb, p0.getData()).cropped();
        Picture out1 = decoder.decodeFrame(bb, p1.getData()).cropped();
        Picture out2 = decoder.decodeFrame(bb, p2.getData()).cropped();
        Picture out3 = decoder.decodeFrame(bb, p3.getData()).cropped();
        comparePic(out1, "src/test/resources/frame1.yuv");
        comparePic(out2, "src/test/resources/frame2.yuv");
        comparePic(out3, "src/test/resources/frame3.yuv");
//        saveYuv(out0, "/tmp/out0.yuv");
//        saveYuv(out1, "/tmp/out1.yuv");
//        saveYuv(out2, "/tmp/out2.yuv");
//        saveYuv(out3, "/tmp/out3.yuv");
    }
}
