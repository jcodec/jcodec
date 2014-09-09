package org.jcodec.containers.mkv;

import static org.jcodec.common.IOUtils.closeQuietly;
import static org.jcodec.common.IOUtils.readFileToByteArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer.VideoTrack;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MKVDemuxerTest {
    MKVDemuxer dem = null;
    private FileInputStream demInputStream;
    private MKVParser par;
    
    @Before
    public void setUp() throws IOException{
        
        FileInputStream inputStream = new FileInputStream("./src/test/resources/mkv/10frames.webm");
        par = new MKVParser(new FileChannelWrapper(inputStream .getChannel()));
        List<EbmlMaster> t = null;
        try {
            t = par.parse();
        } finally {
            closeQuietly(inputStream);
        }
        
        demInputStream = new FileInputStream("./src/test/resources/mkv/10frames.webm");
        dem = new MKVDemuxer(t, new FileChannelWrapper(demInputStream.getChannel()));
    }
    
    @After
    public void tearDown(){
        closeQuietly(demInputStream);
    }

    @Test
    public void testGetFrame() throws IOException {
        
        Assert.assertNotNull(dem);
        Assert.assertNotNull(dem.getVideoTrack());
        
        VideoTrack video = (VideoTrack) dem.getVideoTrack();
        Packet frame = video.nextFrame();
        
        Assert.assertNotNull(video);
        byte[] vp8Frame = readFileToByteArray(new File("./src/test/resources/mkv/10frames01.vp8"));
        Assert.assertArrayEquals(vp8Frame, MKVMuxerTest.bufferToArray(frame.getData()));
    }

    @Test
    public void testPosition() throws IOException {
        
        Assert.assertNotNull(dem);
        Assert.assertNotNull(dem.getVideoTrack());
        
        VideoTrack video = (VideoTrack) dem.getVideoTrack();
        video.gotoFrame(1);
        Packet frame = video.nextFrame();
        
        Assert.assertNotNull(video);
        
        byte[] vp8Frame = readFileToByteArray(new File("./src/test/resources/mkv/10frames02.vp8"));
        Assert.assertArrayEquals(vp8Frame, MKVMuxerTest.bufferToArray(frame.getData()));
    }

}
