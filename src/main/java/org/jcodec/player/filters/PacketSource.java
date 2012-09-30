package org.jcodec.player.filters;

import java.io.IOException;

import org.jcodec.common.model.Packet;

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
    Packet getPacket(byte[] buffer) throws IOException;

    /**
     * Get media info for this stream
     * 
     * @return Media info object
     */
    MediaInfo getMediaInfo();

    /**
     * Seek to a random point in a stream.Tries to seek to a frame having the
     * closest PTS to the one requested.
     * 
     * @param pts
     *            Presentation timestamp represented in stream's timescale
     * @return Weather seek was successfull
     */
    boolean seek(long pts);

    /**
     * Closes this stream
     * 
     * @throws IOException
     */
    void close() throws IOException;
}