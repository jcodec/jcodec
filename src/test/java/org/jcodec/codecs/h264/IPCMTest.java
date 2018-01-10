package org.jcodec.codecs.h264;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.Utils;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.raw.RAWVideoDecoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.junit.Assert;
import org.junit.Test;

public class IPCMTest {

    @Test
    public void testIPCMCAVLC() throws IOException {
        H264Decoder h264Decoder = new H264Decoder();
        ByteBuffer encoded = NIOUtils.fetchFromFile(new File("src/test/resources/h264/ipcm_cavlc.264"));
        ByteBuffer raw = NIOUtils.fetchFromFile(new File("src/test/resources/h264/ipcm_cavlc.yuv"));
        Picture tmp0 = Picture.create(176, 144, ColorSpace.YUV420);
        Picture tmp1 = Picture.create(176, 144, ColorSpace.YUV420);

        Frame frame = h264Decoder.decodeFrame(encoded, tmp0.getData());
        Picture ref = new RAWVideoDecoder(frame.getWidth(), frame.getHeight()).decodeFrame(raw, tmp1.getData());
        Assert.assertTrue(Utils.picturesRoughlyEqual(ref, frame, 1));
    }
    
    @Test
    public void testIPCMCABAC() throws IOException {
        H264Decoder h264Decoder = new H264Decoder();
        ByteBuffer encoded = NIOUtils.fetchFromFile(new File("src/test/resources/h264/ipcm.264"));
        ByteBuffer raw = NIOUtils.fetchFromFile(new File("src/test/resources/h264/ipcm.yuv"));
        Picture tmp0 = Picture.create(176, 144, ColorSpace.YUV420);
        Picture tmp1 = Picture.create(176, 144, ColorSpace.YUV420);

        Frame frame = h264Decoder.decodeFrame(encoded, tmp0.getData());
        Picture ref = new RAWVideoDecoder(frame.getWidth(), frame.getHeight()).decodeFrame(raw, tmp1.getData());
        Assert.assertTrue(Utils.picturesRoughlyEqual(ref, frame, 1));
    }
}