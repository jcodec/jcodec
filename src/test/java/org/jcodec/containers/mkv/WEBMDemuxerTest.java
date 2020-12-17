package org.jcodec.containers.mkv;
import static org.jcodec.common.io.IOUtils.closeQuietly;
import static org.jcodec.common.io.IOUtils.readFileToByteArray;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer.VideoTrack;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class WEBMDemuxerTest {
    MKVDemuxer dem = null;
    private FileInputStream demInputStream;
    
    @Before
    public void setUp() throws IOException{
        demInputStream = new FileInputStream("./src/test/resources/mkv/10frames.webm");
        dem = new MKVDemuxer(new FileChannelWrapper(demInputStream.getChannel()));
    }
    
    @After
    public void tearDown(){
        closeQuietly(demInputStream);
    }

    @Test
    public void testGetFrame() throws IOException {
        
        Assert.assertNotNull(dem);
        Assert.assertNotNull(dem.getVideoTracks().get(0));
        
        VideoTrack video = (VideoTrack) dem.getVideoTracks().get(0);
        Packet frame = video.nextFrame();
        
        Assert.assertNotNull(video);
        byte[] vp8Frame = readFileToByteArray(new File("./src/test/resources/mkv/10frames01.vp8"));
        Assert.assertArrayEquals(vp8Frame, MKVMuxerTest.bufferToArray(frame.getData()));
    }

    @Test
    public void testPosition() throws IOException {
        
        Assert.assertNotNull(dem);
        Assert.assertNotNull(dem.getVideoTracks().get(0));
        
        VideoTrack video = (VideoTrack) dem.getVideoTracks().get(0);
        video.gotoFrame(1);
        Packet frame = video.nextFrame();
        
        Assert.assertNotNull(video);
        
        byte[] vp8Frame = readFileToByteArray(new File("./src/test/resources/mkv/10frames02.vp8"));
        Assert.assertArrayEquals(vp8Frame, MKVMuxerTest.bufferToArray(frame.getData()));
    }

}
