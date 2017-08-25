package org.jcodec.containers.mp4.demuxer;

import org.jcodec.common.AutoFileChannelWrapper;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.platform.Platform;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;

import junit.framework.Assert;

public class MP4DemuxerTest {
    
    @Test
    public void testRawDemuxer() throws Exception {
        URL resource = Platform.getResource(this.getClass(), "37.mp4");
        System.out.println(resource);
        File source = new File(resource.getFile());
        SeekableByteChannel input = new AutoFileChannelWrapper(source);
        MP4Demuxer rawMP4Demuxer = MP4Demuxer.createRawMP4Demuxer(input);
        int dataSize = rawMP4Demuxer.getAudioTracks().get(0).nextFrame().getData().remaining();
        assertEquals(229, dataSize);
    }

    // broken file
    // iphone generated video has 171 samples at 84 samples per chunk but only 2 chunks
    // 2 chunk * 84 samples == 168 expected samples
    // but 171 actual samples
    @Test
    public void testAudioTrack() throws Exception {
        URL resource = Platform.getResource(this.getClass(), "37.mp4");
        System.out.println(resource);
        File source = new File(resource.getFile());
        SeekableByteChannel input = new AutoFileChannelWrapper(source);
        MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(input);
        DemuxerTrack track = demuxer.getAudioTracks().get(0);
        Packet packet;
        while (null != (packet = track.nextFrame())) {
            ByteBuffer data = packet.getData();
        }
    }

    // default sample size in audio track stsz
    // ffmpeg 3.2.2 loves to do it
    @Test
    public void testDefaultSampleSize() throws IOException {
        URL resource = Platform.getResource(this.getClass(), "a01_0023.mp4");
        File source = new File(resource.getFile());
        SeekableByteChannel input = new AutoFileChannelWrapper(source);
        MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(input);
        PCMMP4DemuxerTrack track = (PCMMP4DemuxerTrack) demuxer.getAudioTracks().get(0);
        assertEquals(6, track.getFrameSize());
    }
    
    @Test
    public void testVideoColor() throws Exception {
        File source = new File("src/test/resources/AVCClipCatTest/cat_avc_clip.mp4");
        SeekableByteChannel input = null;
        try {
            input = NIOUtils.readableChannel(source);
            MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(input);
            DemuxerTrack videoTrack = demuxer.getVideoTrack();
            DemuxerTrackMeta meta = videoTrack.getMeta();
            assertEquals(ColorSpace.YUV420J, meta.getCodecMeta().video().getColor());
        } finally {
            NIOUtils.closeQuietly(input);
        }
    }
}
