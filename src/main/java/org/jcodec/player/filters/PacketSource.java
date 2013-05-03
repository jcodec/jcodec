package org.jcodec.player.filters;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.model.Packet;
import org.jcodec.common.model.RationalLarge;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A coded video/audio frame stream
 * 
 * @author The JCodec project
 * 
 */
public interface PacketSource {

    /**
     * Get one encoded frame from a source
     * 
     * @param buffer
     *            Where to put frame data, must be big enough to hold the data
     * @return Encoded frame data with meta-information
     * @throws IOException
     */
    Packet getPacket(ByteBuffer buffer) throws IOException;

    /**
     * Get media info for this stream
     * 
     * @return Media info object
     */
    MediaInfo getMediaInfo() throws IOException;

    /**
     * Seek to a random point in a stream.Tries to seek to a frame having the
     * closest PTS to the one requested.
     * 
     * If the seek can not be performed my throw an exception
     * 
     * @param pts
     *            Presentation timestamp represented in stream's timescale
     */
    void seek(RationalLarge second) throws IOException;

    /**
     * Goes to a specific frame in the video
     * 
     * @param frameNo
     */
    void gotoFrame(int frameNo);

    /**
     * Verifies if seek will be successful if performed
     * 
     * @param second
     * @return
     */
    boolean drySeek(RationalLarge second) throws IOException;

    /**
     * Closes this stream
     * 
     * @throws IOException
     */
    void close() throws IOException;
}