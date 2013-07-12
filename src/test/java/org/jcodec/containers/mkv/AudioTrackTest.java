package org.jcodec.containers.mkv;

import static org.jcodec.common.IOUtils.closeQuietly;
import static org.jcodec.common.IOUtils.readFileToByteArray;
import static org.jcodec.containers.mkv.MKVMuxerTest.tildeExpand;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jcodec.common.model.Packet;
import org.jcodec.containers.mkv.MKVDemuxer.AudioTrack;
import org.jcodec.containers.mkv.elements.BlockElement;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AudioTrackTest {

    private SimpleEBMLParser par;
    private FileInputStream demInputStream;
    private MKVDemuxer dem;
    private boolean showInterlacedBlocks = false;

    public void testSoundSamples() throws Exception {
        AudioTrack audio = dem.getAudioTracks().get(0);
        Assert.assertNotNull(audio);
        audio.seekPointer(9);
        
        Packet p = audio.getFrames(1);
        byte[] audioSample = readFileToByteArray(tildeExpand("./src/test/resources/mkv/test1.audiosample09.mp3"));
        Assert.assertArrayEquals(audioSample, p.getData().array());
    }
    
    public void testTwoSoundSamples() throws Exception {
        AudioTrack audio = dem.getAudioTracks().get(0);
        Assert.assertNotNull(audio);
        audio.seekPointer(8);
        
        Packet p = audio.getFrames(2);
        byte[] sample08 = readFileToByteArray(tildeExpand("./src/test/resources/mkv/test1.audiosample08.mp3"));
        byte[] sample09 = readFileToByteArray(tildeExpand("./src/test/resources/mkv/test1.audiosample09.mp3"));
        byte[] twoSamples = new byte[sample08.length+sample09.length];
        System.arraycopy(sample08, 0, twoSamples, 0, sample08.length);
        System.arraycopy(sample09, 0, twoSamples, sample08.length, sample09.length);
        Assert.assertArrayEquals(twoSamples, p.getData().array());
    }

    @Before
    public void setUp() throws FileNotFoundException, IOException {
        MKVTestSuite suite = MKVTestSuite.read();
        if (!suite.isSuitePresent())
            Assert.fail("MKV test suite is missing, please download from http://www.matroska.org/downloads/test_w1.html, and save to the path recorded in src/test/resources/mkv/suite.properties");
        FileInputStream inputStream = new FileInputStream(suite.test1);
        par = new SimpleEBMLParser(inputStream .getChannel());
        try {
            par.parse();
        } finally {
            closeQuietly(inputStream);
        }
        if (showInterlacedBlocks) {
            BlockElement[] blocks = Type.findAll(par.getTree(), BlockElement.class, Type.Segment, Type.Cluster, Type.SimpleBlock);
            for (BlockElement be : blocks) {
                System.out.println("\nTRACK " + be.trackNumber);
                String pref = "";
                if (be.lacingPresent) {
                    pref = "lacing ";
                }
                for (long offset : be.frameOffsets)
                    System.out.println(pref+"sample offset " + Long.toHexString(offset));
            }
        }
        
        demInputStream = new FileInputStream(suite.test1);
        dem = new MKVDemuxer(par.getTree(), demInputStream.getChannel());
    }
    
    @After
    public void tearDown(){
        closeQuietly(demInputStream);
    }

}
