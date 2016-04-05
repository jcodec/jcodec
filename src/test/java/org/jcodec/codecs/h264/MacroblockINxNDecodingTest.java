package org.jcodec.codecs.h264;

import static org.jcodec.common.ArrayUtil.toByteArrayShifted;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.junit.Assert;
import org.junit.Test;

import js.io.File;
import js.io.IOException;
import js.nio.ByteBuffer;

public class MacroblockINxNDecodingTest {
    @Test
    public void testMBlockCABAC1() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cabac/iNxN_1/16x16.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(16, 16, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cabac/iNxN_1/16x16.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 64)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 64)), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCABAC2() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cabac/iNxN_2/32x32_1.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(32, 32, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cabac/iNxN_2/32x32_1.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCABAC3() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cabac/iNxN_3/32x32_2.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(32, 32, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cabac/iNxN_3/32x32_2.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCABAC4() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cabac/iNxN_4/32x32_3.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(32, 32, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cabac/iNxN_4/32x32_3.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCABAC5() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cabac/iNxN_5/32x32_4.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(32, 32, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cabac/iNxN_5/32x32_4.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCAVLC() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cavlc/iNxN_1/16x16.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(16, 16, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cavlc/iNxN_1/16x16.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 64)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 64)), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCAVLC1() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cavlc/iNxN_2/32x32_1.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(32, 32, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cavlc/iNxN_2/32x32_1.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCAVLC2() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cavlc/iNxN_3/32x32_2.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(32, 32, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cavlc/iNxN_3/32x32_2.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCAVLC3() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cavlc/iNxN_4/32x32_3.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(32, 32, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cavlc/iNxN_4/32x32_3.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCAVLC4() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cavlc/iNxN_5/32x32_4.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(32, 32, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cavlc/iNxN_5/32x32_4.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 256)), out.getPlaneData(2));
    }

    private int[] getAsIntArray(ByteBuffer yuv, int size) {
        byte[] b = new byte[size];
        int[] result = new int[size];
        yuv.getBuf(b);
        for (int i = 0; i < b.length; i++) {
            result[i] = b[i] & 0xff;
        }
        return result;
    }
}