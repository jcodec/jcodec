package org.jcodec.containers.mkv;

import static org.jcodec.common.IOUtils.closeQuietly;
import static org.jcodec.common.IOUtils.readFileToByteArray;
import static org.jcodec.containers.mkv.MKVMuxerTest.tildeExpand;

import java.io.FileInputStream;
import java.io.IOException;

import org.jcodec.common.model.Packet;
import org.jcodec.containers.mkv.MKVDemuxer.VideoTrack;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MKVDemuxerTest {
    MKVDemuxer dem = null;
    private FileInputStream demInputStream;
    private SimpleEBMLParser par;
    
    @Before
    public void setUp() throws IOException{
        
        FileInputStream inputStream = new FileInputStream(MKVMuxerTest.tildeExpand("./src/test/resources/mkv/10frames.webm"));
        par = new SimpleEBMLParser(inputStream .getChannel());
        try {
            par.parse();
        } finally {
            closeQuietly(inputStream);
        }
        
        demInputStream = new FileInputStream(MKVMuxerTest.tildeExpand("./src/test/resources/mkv/10frames.webm"));
        dem = new MKVDemuxer(par.getTree(), demInputStream.getChannel());
    }
    
    @After
    public void tearDown(){
        closeQuietly(demInputStream);
    }

    @Test
    public void testGetFrame() throws IOException {
        
        Assert.assertNotNull(dem);
        Assert.assertNotNull(dem.getVideoTrack());
        Assert.assertNotNull(dem.getTracks());
        
        VideoTrack video = dem.getVideoTrack();
        Packet frame = video.getFrames(1);
        
        Assert.assertNotNull(video);
        System.out.println(video.getFrameCount());
        System.out.println(video.getNo());
        byte[] vp8Frame = readFileToByteArray(tildeExpand("./src/test/resources/mkv/10frames01.vp8"));
        Assert.assertArrayEquals(vp8Frame, frame.getData().array());
    }

    @Test
    public void testPosition() throws IOException {
        
        Assert.assertNotNull(dem);
        Assert.assertNotNull(dem.getVideoTrack());
        Assert.assertNotNull(dem.getTracks());
        
        VideoTrack video = dem.getVideoTrack();
        video.seekPointer(1);
        Packet frame = video.getFrames(1);
        
        Assert.assertNotNull(video);
        System.out.println(video.getFrameCount());
        System.out.println(video.getNo());
        
        byte[] vp8Frame = readFileToByteArray(tildeExpand("./src/test/resources/mkv/10frames02.vp8"));
        Assert.assertArrayEquals(vp8Frame, frame.getData().array());
    }

}
