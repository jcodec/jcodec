package org.jcodec.codecs.h264;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.platform.Platform;
import org.junit.Assert;
import org.junit.Test;

public class MacroblockIntraMixedDecodingTest {

    @Test
    public void testMBlockCABAC1() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File(
                "src/test/resources/h264/cabac/mixed_1/64x64_1.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture buf = Picture.create(64, 64, ColorSpace.YUV420);
        Picture out = new H264Decoder().decodeFrame(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFrom(new File("src/test/resources/h264/cabac/mixed_1/64x64_1.yuv"));
        Assert.assertArrayEquals(getAsIntArray(yuv, 4096), out.getPlaneData(0));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(1));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCABAC2() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File(
                "src/test/resources/h264/cabac/mixed_2/64x64_2.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture buf = Picture.create(64, 64, ColorSpace.YUV420);
        Picture out = new H264Decoder().decodeFrame(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFrom(new File("src/test/resources/h264/cabac/mixed_2/64x64_2.yuv"));
        Assert.assertArrayEquals(getAsIntArray(yuv, 4096), out.getPlaneData(0));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(1));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCABAC3() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File(
                "src/test/resources/h264/cabac/mixed_3/64x64_3.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture buf = Picture.create(64, 64, ColorSpace.YUV420);
        Picture out = new H264Decoder().decodeFrame(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFrom(new File("src/test/resources/h264/cabac/mixed_3/64x64_3.yuv"));
        Assert.assertArrayEquals(getAsIntArray(yuv, 4096), out.getPlaneData(0));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(1));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCABAC4() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File(
                "src/test/resources/h264/cabac/mixed_4/64x64_4.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture buf = Picture.create(64, 64, ColorSpace.YUV420);
        Picture out = new H264Decoder().decodeFrame(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFrom(new File("src/test/resources/h264/cabac/mixed_4/64x64_4.yuv"));
        Assert.assertArrayEquals(getAsIntArray(yuv, 4096), out.getPlaneData(0));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(1));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCAVLC1() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File(
                "src/test/resources/h264/cavlc/mixed_1/64x64_1.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture buf = Picture.create(64, 64, ColorSpace.YUV420);
        Picture out = new H264Decoder().decodeFrame(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFrom(new File("src/test/resources/h264/cavlc/mixed_1/64x64_1.yuv"));
        Assert.assertArrayEquals(getAsIntArray(yuv, 4096), out.getPlaneData(0));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(1));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(2));
    }

    @Test
    public void testMBlockCAVLC2() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File(
                "src/test/resources/h264/cavlc/mixed_2/64x64_2.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture buf = Picture.create(64, 64, ColorSpace.YUV420);
        Picture out = new H264Decoder().decodeFrame(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFrom(new File("src/test/resources/h264/cavlc/mixed_2/64x64_2.yuv"));
        Assert.assertArrayEquals(getAsIntArray(yuv, 4096), out.getPlaneData(0));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(1));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(2));
    }
    
    @Test
    public void testMBlockCAVLC3() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File(
                "src/test/resources/h264/cavlc/mixed_3/64x64_3.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture buf = Picture.create(64, 64, ColorSpace.YUV420);
        Picture out = new H264Decoder().decodeFrame(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFrom(new File("src/test/resources/h264/cavlc/mixed_3/64x64_3.yuv"));
        Assert.assertArrayEquals(getAsIntArray(yuv, 4096), out.getPlaneData(0));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(1));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(2));
    }
    
    @Test
    public void testMBlockCAVLC4() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File(
                "src/test/resources/h264/cavlc/mixed_4/64x64_4.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture buf = Picture.create(64, 64, ColorSpace.YUV420);
        Picture out = new H264Decoder().decodeFrame(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFrom(new File("src/test/resources/h264/cavlc/mixed_4/64x64_4.yuv"));
        Assert.assertArrayEquals(getAsIntArray(yuv, 4096), out.getPlaneData(0));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(1));
        Assert.assertArrayEquals(getAsIntArray(yuv, 1024), out.getPlaneData(2));
    }
    
    @Test
    public void testMBlockShit() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File(
                "src/test/resources/h264/cabac/random_1/random_1.264")));
        ByteBuffer data = es.nextFrame().getData();
        Picture buf = Picture.create(480, 272, ColorSpace.YUV420);
        Picture out = new H264Decoder().decodeFrame(data, buf.getData());
        
        ByteBuffer yuv = NIOUtils.fetchFrom(new File("src/test/resources/h264/cabac/random_1/random_1.yuv"));
        Assert.assertArrayEquals(getAsIntArray(yuv, 129600), Platform.copyOfRangeI(out.getPlaneData(0), 0, 129600));
        Assert.assertArrayEquals(getAsIntArray(yuv, 32400), Platform.copyOfRangeI(out.getPlaneData(1), 0, 32400));
        Assert.assertArrayEquals(getAsIntArray(yuv, 32400), Platform.copyOfRangeI(out.getPlaneData(2), 0, 32400));
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