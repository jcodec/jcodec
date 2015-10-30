package org.jcodec.codecs.h264;

import static org.jcodec.common.JCodecUtil.getAsArray;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.junit.Assert;
import org.junit.Test;

public class MacroblockInterDecodingTest {

    @Test
    public void testMBlockCABAC1() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File("src/test/resources/h264/cabac/p_1/seq.264")));
        H264Decoder dec = new H264Decoder();

        Picture8Bit[] out = {
                dec.decodeFrame8Bit(es.nextFrame().getData(), Picture8Bit.create(32, 32, ColorSpace.YUV420).getData()),
                dec.decodeFrame8Bit(es.nextFrame().getData(), Picture8Bit.create(32, 32, ColorSpace.YUV420).getData()),
                dec.decodeFrame8Bit(es.nextFrame().getData(), Picture8Bit.create(32, 32, ColorSpace.YUV420).getData()) };

        ByteBuffer yuv = NIOUtils.fetchFrom(new File("src/test/resources/h264/cabac/p_1/seq_dec.yuv"));
        Assert.assertArrayEquals(getAsArray(yuv, 1024), out[0].getPlaneData(0));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[0].getPlaneData(1));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[0].getPlaneData(2));
        Assert.assertArrayEquals(getAsArray(yuv, 1024), out[1].getPlaneData(0));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[1].getPlaneData(1));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[1].getPlaneData(2));
        Assert.assertArrayEquals(getAsArray(yuv, 1024), out[2].getPlaneData(0));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[2].getPlaneData(1));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[2].getPlaneData(2));
    }

    @Test
    public void testMBlockCABAC2() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File("src/test/resources/h264/cabac/p_2/seq.264")));
        H264Decoder dec = new H264Decoder();

        Picture8Bit[] out = {
                dec.decodeFrame8Bit(es.nextFrame().getData(), Picture8Bit.create(32, 32, ColorSpace.YUV420).getData()),
                dec.decodeFrame8Bit(es.nextFrame().getData(), Picture8Bit.create(32, 32, ColorSpace.YUV420).getData()),
                dec.decodeFrame8Bit(es.nextFrame().getData(), Picture8Bit.create(32, 32, ColorSpace.YUV420).getData()) };

        ByteBuffer yuv = NIOUtils.fetchFrom(new File("src/test/resources/h264/cabac/p_2/seq_dec.yuv"));
        Assert.assertArrayEquals(getAsArray(yuv, 1024), out[0].getPlaneData(0));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[0].getPlaneData(1));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[0].getPlaneData(2));
        Assert.assertArrayEquals(getAsArray(yuv, 1024), out[1].getPlaneData(0));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[1].getPlaneData(1));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[1].getPlaneData(2));
        Assert.assertArrayEquals(getAsArray(yuv, 1024), out[2].getPlaneData(0));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[2].getPlaneData(1));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[2].getPlaneData(2));
    }

    @Test
    public void testMBlockCAVLC1() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File("src/test/resources/h264/cavlc/p_1/seq.264")));
        H264Decoder dec = new H264Decoder();

        Picture8Bit[] out = {
                dec.decodeFrame8Bit(es.nextFrame().getData(), Picture8Bit.create(32, 32, ColorSpace.YUV420).getData()),
                dec.decodeFrame8Bit(es.nextFrame().getData(), Picture8Bit.create(32, 32, ColorSpace.YUV420).getData()),
                dec.decodeFrame8Bit(es.nextFrame().getData(), Picture8Bit.create(32, 32, ColorSpace.YUV420).getData()) };

        ByteBuffer yuv = NIOUtils.fetchFrom(new File("src/test/resources/h264/cavlc/p_1/seq_dec.yuv"));
        Assert.assertArrayEquals(getAsArray(yuv, 1024), out[0].getPlaneData(0));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[0].getPlaneData(1));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[0].getPlaneData(2));
        Assert.assertArrayEquals(getAsArray(yuv, 1024), out[1].getPlaneData(0));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[1].getPlaneData(1));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[1].getPlaneData(2));
        Assert.assertArrayEquals(getAsArray(yuv, 1024), out[2].getPlaneData(0));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[2].getPlaneData(1));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[2].getPlaneData(2));
    }

    @Test
    public void testMBlockCAVLC2() throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File("src/test/resources/h264/cavlc/p_2/seq.264")));
        H264Decoder dec = new H264Decoder();

        Picture8Bit[] out = {
                dec.decodeFrame8Bit(es.nextFrame().getData(), Picture8Bit.create(32, 32, ColorSpace.YUV420).getData()),
                dec.decodeFrame8Bit(es.nextFrame().getData(), Picture8Bit.create(32, 32, ColorSpace.YUV420).getData()),
                dec.decodeFrame8Bit(es.nextFrame().getData(), Picture8Bit.create(32, 32, ColorSpace.YUV420).getData()) };

        ByteBuffer yuv = NIOUtils.fetchFrom(new File("src/test/resources/h264/cavlc/p_2/seq_dec.yuv"));
        Assert.assertArrayEquals(getAsArray(yuv, 1024), out[0].getPlaneData(0));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[0].getPlaneData(1));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[0].getPlaneData(2));
        Assert.assertArrayEquals(getAsArray(yuv, 1024), out[1].getPlaneData(0));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[1].getPlaneData(1));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[1].getPlaneData(2));
        Assert.assertArrayEquals(getAsArray(yuv, 1024), out[2].getPlaneData(0));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[2].getPlaneData(1));
        Assert.assertArrayEquals(getAsArray(yuv, 256), out[2].getPlaneData(2));
    }
}