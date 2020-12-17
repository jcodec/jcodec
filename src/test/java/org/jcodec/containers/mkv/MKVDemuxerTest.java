package org.jcodec.containers.mkv;

import static org.jcodec.common.io.IOUtils.closeQuietly;
import static org.jcodec.common.io.IOUtils.readFileToByteArray;

import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.TrackType;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer.AudioTrack;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer.SubtitlesTrack;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer.VideoTrack;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

public class MKVDemuxerTest {
    MKVDemuxer demuxer = null;
    private FileInputStream demInputStream;
    MKVTestSuite suite;
    
    @Before
    public void setUp() throws IOException{
        suite = MKVTestSuite.read();
        if (!suite.isSuitePresent()) {
            Assert.fail("MKV test suite is missing, please download from https://www.matroska.org/downloads/test_suite.html, and save to the path recorded in src/test/resources/mkv/suite.properties");
    	}
        //System.out.println("Scanning file: " + suite.test5.getAbsolutePath());
        demInputStream = new FileInputStream(suite.test5);
        demuxer = new MKVDemuxer(new FileChannelWrapper(demInputStream.getChannel()));
    }

    @After
    public void tearDown(){
        closeQuietly(demInputStream);
    }

    @Test
    public void showAll() throws IOException {
        System.out.println("showAll");
    	System.out.println("Audio tracks: " + demuxer.getAudioTracks().size());
        for (DemuxerTrack elem : demuxer.getAudioTracks()) {
        	AudioTrack elemAudio = (AudioTrack)elem;
        	if (elemAudio == null) {
        		System.out.println("    - null");
        	} else if (elemAudio.getMeta() == null) {
            	System.out.println("    - null (no metadata)");
            } else {
        		System.out.println("    - " + elemAudio.getLanguage() + "  " + elemAudio.getMeta().getCodec() + "  " + elemAudio.getMeta().getAudioCodecMeta().getChannelCount() + "  " + elemAudio.getMeta().getAudioCodecMeta().getSampleRate());
        	}
        }
    	System.out.println("Video tracks: " + demuxer.getVideoTracks().size());
        for (DemuxerTrack elem : demuxer.getVideoTracks()) {
        	VideoTrack elemVideo = (VideoTrack)elem;
        	System.out.println("    - " + elemVideo.getMeta().getType() + "  " + elemVideo.getMeta().getCodec() + "  " + elemVideo.getMeta().getVideoCodecMeta().getSize());
        }
    	System.out.println("Video subtitle tracks: " + demuxer.getSubtitleTracks().size());
        for (DemuxerTrack elem : demuxer.getSubtitleTracks()) {
        	SubtitlesTrack elemSubtitle = (SubtitlesTrack)elem;
        	if (elemSubtitle == null) {
        		System.out.println("    - null");
        	} else if (elemSubtitle.getMeta() == null) {
            	System.out.println("    - null (no metadata)");
            } else {
        	   	System.out.println("    - " + elemSubtitle.getLanguage() + "  " + elemSubtitle.getMeta().getType() + "  " + elemSubtitle.getMeta().getCodec());
            }
        }
    }

    @Test
    public void _testVideo() throws IOException {
        Assert.assertEquals(1, demuxer.getVideoTracks().size());
        DemuxerTrack track = demuxer.getVideoTracks().get(0);
        Assert.assertEquals(TrackType.VIDEO, track.getMeta().getType());
    	VideoTrack trackVideo = (VideoTrack)track;
        Assert.assertEquals(Codec.H264, trackVideo.getMeta().getCodec());
        Assert.assertEquals(576, trackVideo.getMeta().getVideoCodecMeta().getSize().getHeight());
        Assert.assertEquals(1024, trackVideo.getMeta().getVideoCodecMeta().getSize().getWidth());
    }

    @Test
    public void _testAudio() throws IOException {
        Assert.assertEquals(2, demuxer.getAudioTracks().size());
        // test track 0
        DemuxerTrack track = demuxer.getAudioTracks().get(0);
        Assert.assertEquals(TrackType.AUDIO, track.getMeta().getType());
    	AudioTrack trackAudio = (AudioTrack)track;
        Assert.assertEquals(Codec.AAC, trackAudio.getMeta().getCodec());
        Assert.assertEquals(48000, trackAudio.getMeta().getAudioCodecMeta().getSampleRate());
        Assert.assertEquals(2, trackAudio.getMeta().getAudioCodecMeta().getChannelCount());
        Assert.assertEquals("und", trackAudio.getLanguage());
        // test track 1
        track = demuxer.getAudioTracks().get(1);
        Assert.assertEquals(TrackType.AUDIO, track.getMeta().getType());
    	trackAudio = (AudioTrack)track;
        Assert.assertEquals(Codec.AAC, trackAudio.getMeta().getCodec());
        Assert.assertEquals(22050, trackAudio.getMeta().getAudioCodecMeta().getSampleRate());
        Assert.assertEquals(1, trackAudio.getMeta().getAudioCodecMeta().getChannelCount());
        Assert.assertEquals("eng", trackAudio.getLanguage());
    }

    @Test
    public void _testSubtitle() throws IOException {
        Assert.assertEquals(8, demuxer.getSubtitleTracks().size());
        // test track 0
        DemuxerTrack track = demuxer.getSubtitleTracks().get(0);
        Assert.assertEquals(TrackType.TEXT, track.getMeta().getType());
        SubtitlesTrack trackSubtitle = (SubtitlesTrack)track;
        Assert.assertEquals(Codec.UTF8, trackSubtitle.getMeta().getCodec());
        Assert.assertEquals("eng", trackSubtitle.getLanguage());
        // test track 1
        track = demuxer.getSubtitleTracks().get(1);
        Assert.assertEquals(TrackType.TEXT, track.getMeta().getType());
        trackSubtitle = (SubtitlesTrack)track;
        Assert.assertEquals(Codec.UTF8, trackSubtitle.getMeta().getCodec());
        Assert.assertEquals("hun", trackSubtitle.getLanguage());
        // test track 2
        track = demuxer.getSubtitleTracks().get(2);
        Assert.assertEquals(TrackType.TEXT, track.getMeta().getType());
        trackSubtitle = (SubtitlesTrack)track;
        Assert.assertEquals(Codec.UTF8, trackSubtitle.getMeta().getCodec());
        Assert.assertEquals("ger", trackSubtitle.getLanguage());
        // test track 3
        track = demuxer.getSubtitleTracks().get(3);
        Assert.assertEquals(TrackType.TEXT, track.getMeta().getType());
        trackSubtitle = (SubtitlesTrack)track;
        Assert.assertEquals(Codec.UTF8, trackSubtitle.getMeta().getCodec());
        Assert.assertEquals("fre", trackSubtitle.getLanguage());
        // test track 4
        track = demuxer.getSubtitleTracks().get(4);
        Assert.assertEquals(TrackType.TEXT, track.getMeta().getType());
        trackSubtitle = (SubtitlesTrack)track;
        Assert.assertEquals(Codec.UTF8, trackSubtitle.getMeta().getCodec());
        Assert.assertEquals("spa", trackSubtitle.getLanguage());
        // test track 5
        track = demuxer.getSubtitleTracks().get(5);
        Assert.assertEquals(TrackType.TEXT, track.getMeta().getType());
        trackSubtitle = (SubtitlesTrack)track;
        Assert.assertEquals(Codec.UTF8, trackSubtitle.getMeta().getCodec());
        Assert.assertEquals("ita", trackSubtitle.getLanguage());
        // test track 6
        track = demuxer.getSubtitleTracks().get(6);
        Assert.assertEquals(TrackType.TEXT, track.getMeta().getType());
        trackSubtitle = (SubtitlesTrack)track;
        Assert.assertEquals(Codec.UTF8, trackSubtitle.getMeta().getCodec());
        Assert.assertEquals("jpn", trackSubtitle.getLanguage());
        // test track 7
        track = demuxer.getSubtitleTracks().get(7);
        Assert.assertEquals(TrackType.TEXT, track.getMeta().getType());
        trackSubtitle = (SubtitlesTrack)track;
        Assert.assertEquals(Codec.UTF8, trackSubtitle.getMeta().getCodec());
        Assert.assertEquals("und", trackSubtitle.getLanguage());
    }


}
