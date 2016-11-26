package org.jcodec.codecs.h264;

import static org.jcodec.common.ArrayUtil.toByteArrayShifted;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MBlock8x8Test {
    @Test
    public void testMBlockCABACStrict1() throws IOException {
        BufferH264ES es = new BufferH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cabac/i8x8/64x64_1.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(64, 64, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cabac/i8x8/64x64_1_dec.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 4096)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(2));
    }
    
    @Test
    public void testMBlockCABACStrict2() throws IOException {
        BufferH264ES es = new BufferH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cabac/i8x8/64x64_2.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(64, 64, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cabac/i8x8/64x64_2_dec.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 4096)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(2));
    }
    
    @Test
    public void testMBlockCABACStrict3() throws IOException {
        BufferH264ES es = new BufferH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cabac/i8x8/64x64_3.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(64, 64, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cabac/i8x8/64x64_3_dec.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 4096)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(2));
    }
    
    @Test
    public void testMBlockCABACStrict4() throws IOException {
        BufferH264ES es = new BufferH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cabac/i8x8/64x64_4.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(64, 64, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cabac/i8x8/64x64_4_dec.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 4096)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(2));
    }
    
    @Test
    public void testMBlockCAVLCStrict1() throws IOException {
        BufferH264ES es = new BufferH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cavlc/i8x8/64x64_1.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(64, 64, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cavlc/i8x8/64x64_1_dec.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 4096)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(2));
    }
    
    @Test
    public void testMBlockCAVLCStrict2() throws IOException {
        BufferH264ES es = new BufferH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cavlc/i8x8/64x64_2.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(64, 64, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cavlc/i8x8/64x64_2_dec.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 4096)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(2));
    }
    
    @Test
    public void testMBlockCAVLCStrict3() throws IOException {
        BufferH264ES es = new BufferH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cavlc/i8x8/64x64_3.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(64, 64, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cavlc/i8x8/64x64_3_dec.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 4096)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(2));
    }
    
    @Test
    public void testMBlockCAVLCStrict4() throws IOException {
        BufferH264ES es = new BufferH264ES(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/cavlc/i8x8/64x64_4.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture8Bit buf = Picture8Bit.create(64, 64, ColorSpace.YUV420);
        Picture8Bit out = new H264Decoder().decodeFrame8Bit(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFromFile(new File("src/test/resources/h264/cavlc/i8x8/64x64_4_dec.yuv"));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 4096)), out.getPlaneData(0));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(1));
        Assert.assertArrayEquals(toByteArrayShifted(getAsIntArray(yuv, 1024)), out.getPlaneData(2));
    }
    
    private int[] getAsIntArray(ByteBuffer yuv, int size) {
        byte[] b = new byte[size];
        int[] result = new int[size];
        yuv.get(b);
        for (int i = 0; i < b.length; i++) {
            result[i] = b[i] & 0xff;
        }
        return result;
    }
}
