package org.jcodec.containers.mkv;
import static java.lang.String.format;
import static org.jcodec.codecs.h264.H264Utils.splitMOVPacket;
import static org.jcodec.common.model.ColorSpace.RGB;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer.VideoTrack;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.Transform;
import org.jcodec.scale.Yuv420pToRgb;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.lang.System;
import java.nio.ByteBuffer;

public class MKVFrameReadingTest {

    static MKVTestSuite suite;
    static String outPattern = "/tmp/frame%d.png";

    /*
    @BeforeClass
    public static void setUpTestSuite() throws Exception {
        suite = MKVTestSuite.read();
        if (!suite.isSuitePresent())
            Assert.fail("MKV test suite is missing, please download from https://www.matroska.org/downloads/test_suite.html, and save to the path recorded in src/test/resources/mkv/suite.properties");
    }
    */

    MKVParser par;
    MKVDemuxer dem;
    SeekableByteChannel demInputStream;

    /*
    @Before
    public void setUp() throws IOException {

        FileInputStream inputStream = new FileInputStream(suite.test5);
        par = new MKVParser(new FileChannelWrapper(inputStream.getChannel()));
        List<EbmlMaster> t = null;
        try {
            t = par.parse();
        } finally {
            closeQuietly(inputStream);
        }

        FileInputStream fis = new FileInputStream(suite.test5);
        demInputStream = new FileChannelWrapper(fis.getChannel());
        dem = new MKVDemuxer(t, demInputStream);
    }
    */

    @Ignore @Test
    public void _test() throws Exception {

        H264Decoder decoder = new H264Decoder();
        Transform transform = new Yuv420pToRgb();

        DemuxerTrack inTrack = dem.getVideoTracks().get(0);

        Picture rgb = Picture.create(dem.getPictureWidth(), dem.getPictureHeight(), ColorSpace.RGB);
        AvcCBox avcC = AvcCBox.parseAvcCBox(((VideoTrack) inTrack).getCodecState());

        decoder.addSps(avcC.getSpsList());
        decoder.addPps(avcC.getPpsList());

        Packet inFrame;
        for (int i = 1; (inFrame = inTrack.nextFrame()) != null && i <= 200; i++) {
            Picture buf = Picture.create(dem.getPictureWidth(), dem.getPictureHeight(), ColorSpace.YUV422);
            Picture pic = decoder.decodeFrameFromNals(splitMOVPacket(inFrame.getData(), avcC), buf.getData());
            if (rgb == null)
                rgb = Picture.create(pic.getWidth(), pic.getHeight(), RGB);
            transform.transform(pic, rgb);
            AWTUtil.writePNG(rgb, new File(format(outPattern, i++)));
        }

    }
    
    @Ignore @Test
    public void _testFirstFrame() throws Exception {
        
        Transform transform = new Yuv420pToRgb();

        H264Decoder decoder = new H264Decoder();
        DemuxerTrack inTrack = dem.getVideoTracks().get(0);

        AvcCBox avcC = AvcCBox.parseAvcCBox(((VideoTrack) inTrack).getCodecState());

        Assert.assertNotNull(avcC.getSpsList());
        Assert.assertFalse(avcC.getSpsList().isEmpty());
        Assert.assertNotNull(avcC.getPpsList());
        Assert.assertFalse(avcC.getPpsList().isEmpty());
        
        decoder.addSps(avcC.getSpsList());
        decoder.addPps(avcC.getPpsList());

        Packet inFrame = inTrack.nextFrame();
        Assert.assertNotNull(inFrame);
        
        ByteBuffer bb = inFrame.getData();
        Assert.assertNotNull(bb);
        
        
        File file = new File(suite.base, "test5_frame0.h264");
        byte[] rawFrame = IOUtils.readFileToByteArray(file);
        
        Assert.assertArrayEquals(rawFrame, MKVMuxerTest.bufferToArray(bb));
        
        Picture buf = Picture.create(dem.getPictureWidth(), dem.getPictureHeight(), ColorSpace.YUV422);
        Picture pic = decoder.decodeFrameFromNals(H264Utils.splitMOVPacket(inFrame.getData(), avcC), buf.getData());
        Picture rgb = Picture.create(dem.getPictureWidth(), dem.getPictureHeight(), ColorSpace.RGB);
        transform.transform(pic, rgb);
        File f = new File(format(outPattern, 0));
        System.out.println("Writing to file: " + f.getAbsolutePath());
        AWTUtil.writePNG(rgb, f);
    }

}
