package org.jcodec.player.filters;

import java.io.IOException;

import org.jcodec.common.model.Frame;
import org.jcodec.common.model.RationalLarge;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface VideoSource {

    /**
     * Decodes a video frame into supplied buffer
     * 
     * The frames are guaranteed to be returned in display order any reordering
     * should happen inside of the video source
     * 
     * @param buffer
     * @return
     * @throws IOException
     */
    Frame decode(byte[][] buffer) throws IOException;

    /**
     * Determines if seek is possible
     * 
     * @param second
     * @return
     * @throws IOException
     */
    boolean drySeek(RationalLarge second) throws IOException;

    /**
     * Seeks the video source to a different second position in the stream
     * 
     * @param second
     * @throws IOException
     */
    void seek(RationalLarge second) throws IOException;

    /**
     * Seeks the video source to a different frame position in the stream
     * 
     * Note, all frame positions are specified in decoding order, i.e. in the
     * order frame appear in the stream
     * 
     * @param frame
     */
    void gotoFrame(int frame) throws IOException;

    /**
     * Returns meta information about this stream
     * 
     * Note, may return different data on every invocation as meta information
     * may change over time. I.e. in the case of a live stream or stream that is
     * in the process of indexing.
     * 
     * @return
     * @throws IOException
     */
    MediaInfo.VideoInfo getMediaInfo() throws IOException;

    /**
     * Closes the video source
     * 
     * This must close the underlying packet source ( if any ) and free all
     * resources associated with decoding of this stream
     * 
     * @throws IOException
     */
    void close() throws IOException;
}