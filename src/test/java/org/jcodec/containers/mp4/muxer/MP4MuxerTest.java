package org.jcodec.containers.mp4.muxer;

import static org.jcodec.common.Codec.H264;
import static org.jcodec.common.VideoCodecMeta.createSimpleVideoCodecMeta;
import static org.jcodec.common.model.ColorSpace.YUV420;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.io.ByteBufferChannel;
import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.junit.Test;

public class MP4MuxerTest {
    
    @Test
    public void testAddEmptyTracks() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        ByteBufferChannel output = new ByteBufferChannel(buf);
        MP4Muxer muxer = MP4Muxer.createMP4MuxerToChannel(output);
        muxer.addVideoTrack(H264, createSimpleVideoCodecMeta(new Size(42, 43), YUV420));
        muxer.finish();

        ByteBuffer contents = output.getContents();
        MP4Demuxer demuxer = MP4Demuxer.createRawMP4Demuxer(new ByteBufferSeekableByteChannel(contents));
        DemuxerTrack videoTrack = demuxer.getVideoTrack();
        assertNotNull(videoTrack);
        assertEquals(1, demuxer.getVideoTracks().size());
        assertEquals(0, demuxer.getAudioTracks().size());
    }

    @Test
    public void testAddTrackWithId() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        SeekableByteChannel output = new ByteBufferSeekableByteChannel(buf);
        MP4Muxer muxer = MP4Muxer.createMP4MuxerToChannel(output);
        muxer.addTrackWithId(MP4TrackType.SOUND, Codec.AAC, 2);
        muxer.addTrackWithId(MP4TrackType.VIDEO, Codec.H264, 1);
        muxer.finish();

        output.setPosition(0);
        MovieBox movie = MP4Util.parseMovieChannel(output);
        assertEquals(2, movie.getTracks().length);
        assertNotNull(movie.getVideoTrack());
        assertNotNull(movie.getAudioTracks().get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddTrackWithSameId() throws Exception {
        SeekableByteChannel output = new ByteBufferSeekableByteChannel(ByteBuffer.allocate(64 * 1024));
        MP4Muxer muxer = MP4Muxer.createMP4MuxerToChannel(output);
        muxer.addTrackWithId(MP4TrackType.SOUND, Codec.AAC, 1);
        muxer.addTrackWithId(MP4TrackType.VIDEO, Codec.H264, 1);
    }

}
