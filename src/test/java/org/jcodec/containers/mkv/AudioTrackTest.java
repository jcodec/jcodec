package org.jcodec.containers.mkv;
import static java.lang.System.arraycopy;
import static org.jcodec.common.io.IOUtils.closeQuietly;
import static org.jcodec.common.io.IOUtils.readFileToByteArray;
import static org.jcodec.containers.mkv.MKVMuxerTest.bufferToArray;
import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.SimpleBlock;
import static org.jcodec.containers.mkv.MKVType.findAllTree;

import org.jcodec.Utils;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer.AudioTrack;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.System;
import java.nio.ByteBuffer;
import java.util.List;

public class AudioTrackTest {

    private MKVDemuxer demuxer;
    private boolean showInterlacedBlocks = false;

    @Ignore @Test
    public void _testSoundSamples() throws Exception {
        AudioTrack audio = (AudioTrack) demuxer.getAudioTracks().get(0);
        Assert.assertNotNull(audio);
        audio.gotoFrame(9);
        
        Packet p = audio.nextFrame();
        ByteBuffer audioSample = NIOUtils.fetchFromFile(Utils.tildeExpand("./src/test/resources/mkv/test1.audiosample09.mp3"));
        
        Assert.assertArrayEquals(audioSample.array(), bufferToArray(p.getData()));
    }

    @Ignore @Test
    public void _testTwoSoundSamples() throws Exception {
        AudioTrack audio = (AudioTrack) demuxer.getAudioTracks().get(0);
        Assert.assertNotNull(audio);
        audio.gotoFrame(8);
        
        Packet p = audio.getFrames(2);
        byte[] sample08 = readFileToByteArray(Utils.tildeExpand("./src/test/resources/mkv/test1.audiosample08.mp3"));
        byte[] sample09 = readFileToByteArray(Utils.tildeExpand("./src/test/resources/mkv/test1.audiosample09.mp3"));
        byte[] twoSamples = new byte[sample08.length+sample09.length];
        arraycopy(sample08, 0, twoSamples, 0, sample08.length);
        arraycopy(sample09, 0, twoSamples, sample08.length, sample09.length);
        Assert.assertArrayEquals(twoSamples, p.getData().array());
    }

    @Before
    public void setUp() throws FileNotFoundException, IOException {
        MKVTestSuite suite = MKVTestSuite.read();
        if (!suite.isSuitePresent())
            Assert.fail("MKV test suite is missing, please download from https://www.matroska.org/downloads/test_suite.html, and save to the path recorded in src/test/resources/mkv/suite.properties");
        
        demuxer = new MKVDemuxer(NIOUtils.readableChannel(suite.test1));
        List<? extends EbmlBase> tree = demuxer.getTree();
        if (showInterlacedBlocks) {
            MKVType[] path = { Segment, Cluster, SimpleBlock };
            MkvBlock[] blocks = findAllTree(tree, MkvBlock.class, path);
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
    }
    
    @After
    public void tearDown(){
        closeQuietly(demuxer);
    }

}
