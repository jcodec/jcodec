package org.jcodec.containers.mp3;

import java.io.File;
import java.io.IOException;

import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.junit.Assert;
import org.junit.Test;

public class MPEGAudioDemuxerTest {

    @Test
    public void testParseCBR() throws IOException {
	File mp3 = new File("src/test/resources/ascii.mp3");
	SeekableByteChannel ch = null;
	try {
	    ch = NIOUtils.readableChannel(mp3);
	    MPEGAudioDemuxer mp3Parser = new MPEGAudioDemuxer(ch);
	    Assert.assertNull(mp3Parser.getVideoTracks());
	    DemuxerTrack demuxerTrack = mp3Parser.getAudioTracks().get(0);
	    Packet nextFrame = demuxerTrack.nextFrame();
	    for (int i = 0; i < 607; i++) {
		nextFrame = demuxerTrack.nextFrame();
		Assert.assertNotNull(nextFrame);
		Assert.assertEquals(522, nextFrame.getData().remaining());
	    }
	    Assert.assertNull(demuxerTrack.nextFrame());

	    DemuxerTrackMeta meta = demuxerTrack.getMeta();
	    Assert.assertEquals(44100, meta.getAudioCodecMeta().getSampleRate());
	    Assert.assertEquals(2, meta.getAudioCodecMeta().getChannelCount());
	} finally {
	    NIOUtils.closeQuietly(ch);
	}
    }

    @Test
    public void testMP3ParseVBR() throws IOException {
	int[] sizes = { 208, 104, 417, 417, 417, 417, 365, 417, 417, 417, 365, 417, 313, 365, 365, 313, 313, 313, 313,
		313, 313, 261, 261, 417, 261, 208, 208, 104, 417, 104, 104, 104

	};
	File mp3 = new File("src/test/resources/drip.mp3");
	SeekableByteChannel ch = null;
	try {
	    ch = NIOUtils.readableChannel(mp3);
	    MPEGAudioDemuxer mp3Parser = new MPEGAudioDemuxer(ch);
	    Assert.assertNull(mp3Parser.getVideoTracks());
	    DemuxerTrack demuxerTrack = mp3Parser.getAudioTracks().get(0);
	    for (int i = 0; i < 32; i++) {
		Packet nextFrame = demuxerTrack.nextFrame();
		Assert.assertNotNull(nextFrame);
		Assert.assertEquals(sizes[i], nextFrame.getData().remaining());
	    }
	    Assert.assertNull(demuxerTrack.nextFrame());

	    DemuxerTrackMeta meta = demuxerTrack.getMeta();
	    Assert.assertEquals(44100, meta.getAudioCodecMeta().getSampleRate());
	    Assert.assertEquals(2, meta.getAudioCodecMeta().getChannelCount());
	} finally {
	    NIOUtils.closeQuietly(ch);
	}
    }
    
    @Test
    public void testProbe1() throws IOException {
        File mp3 = new File("src/test/resources/drip.mp3");
        int probe = MPEGAudioDemuxer.PROBE.probe(NIOUtils.fetchFromFileL(mp3, 200 * 1024));
        Assert.assertEquals(100, probe);
    }
    
    @Test
    public void testProbe2() throws IOException {
        File mp3 = new File("src/test/resources/ascii.mp3");
        int probe = MPEGAudioDemuxer.PROBE.probe(NIOUtils.fetchFromFileL(mp3, 200 * 1024));
        Assert.assertEquals(99, probe);
    }
}
