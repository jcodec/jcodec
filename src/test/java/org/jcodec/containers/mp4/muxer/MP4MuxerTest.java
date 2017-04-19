package org.jcodec.containers.mp4.muxer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.ByteBuffer;

import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.junit.Test;

public class MP4MuxerTest {
    @Test
    public void testAddTrackWithId() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        SeekableByteChannel output = new ByteBufferSeekableByteChannel(buf);
        MP4Muxer muxer = MP4Muxer.createMP4MuxerToChannel(output);
        muxer.addTrackWithId(TrackType.SOUND, 48000, 2);
        muxer.addTrackWithId(TrackType.VIDEO, 30000, 1);
        muxer.writeHeader();
        
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
        muxer.addTrackWithId(TrackType.SOUND, 48000, 1);
        muxer.addTrackWithId(TrackType.VIDEO, 30000, 1);
    }

}
