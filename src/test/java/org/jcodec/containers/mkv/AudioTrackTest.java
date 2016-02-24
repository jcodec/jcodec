package org.jcodec.containers.mkv;

import static java.lang.System.arraycopy;
import static org.jcodec.common.io.IOUtils.closeQuietly;
import static org.jcodec.common.io.IOUtils.readFileToByteArray;
import static org.jcodec.containers.mkv.MKVMuxerTest.bufferToArray;
import static org.jcodec.containers.mkv.MKVMuxerTest.tildeExpand;
import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.SimpleBlock;
import static org.jcodec.containers.mkv.MKVType.findAllTree;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer.AudioTrack;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class AudioTrackTest {

    private MKVParser par;
    private FileInputStream demInputStream;
    private MKVDemuxer dem;
    private boolean showInterlacedBlocks = false;

    @Ignore @Test
    public void testSoundSamples() throws Exception {
        AudioTrack audio = (AudioTrack) dem.getAudioTracks().get(0);
        Assert.assertNotNull(audio);
        audio.gotoFrame(9);
        
        Packet p = audio.nextFrame();
        ByteBuffer audioSample = NIOUtils.fetchFromFile(tildeExpand("./src/test/resources/mkv/test1.audiosample09.mp3"));
        
        Assert.assertArrayEquals(audioSample.array(), bufferToArray(p.getData()));
    }

    @Ignore @Test
    public void testTwoSoundSamples() throws Exception {
        AudioTrack audio = (AudioTrack) dem.getAudioTracks().get(0);
        Assert.assertNotNull(audio);
        audio.gotoFrame(8);
        
        Packet p = audio.getFrames(2);
        byte[] sample08 = readFileToByteArray(tildeExpand("./src/test/resources/mkv/test1.audiosample08.mp3"));
        byte[] sample09 = readFileToByteArray(tildeExpand("./src/test/resources/mkv/test1.audiosample09.mp3"));
        byte[] twoSamples = new byte[sample08.length+sample09.length];
        arraycopy(sample08, 0, twoSamples, 0, sample08.length);
        arraycopy(sample09, 0, twoSamples, sample08.length, sample09.length);
        Assert.assertArrayEquals(twoSamples, p.getData().array());
    }

    @Before
    public void setUp() throws FileNotFoundException, IOException {
        MKVTestSuite suite = MKVTestSuite.read();
        if (!suite.isSuitePresent())
            Assert.fail("MKV test suite is missing, please download from http://www.matroska.org/downloads/test_w1.html, and save to the path recorded in src/test/resources/mkv/suite.properties");
        FileInputStream inputStream = new FileInputStream(suite.test1);
        par = new MKVParser(new FileChannelWrapper(inputStream .getChannel()));
        List<EbmlMaster> mkv = null;
        try {
            mkv = par.parse();
        } finally {
            closeQuietly(inputStream);
        }
        if (showInterlacedBlocks) {
            MKVType[] path = { Segment, Cluster, SimpleBlock };
            MkvBlock[] blocks = findAllTree(mkv, MkvBlock.class, path);
            for (MkvBlock be : blocks) {
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
        dem = new MKVDemuxer(mkv, new FileChannelWrapper(demInputStream.getChannel()));
    }
    
    @After
    public void tearDown(){
        closeQuietly(demInputStream);
    }

}
