package org.jcodec.common;

import js.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface SeekableDemuxerTrack extends DemuxerTrack {

    /**
     * Goes to exactly frameNo.
     * 
     * @param frameNo
     *            Frame number, zero based, to go to.
     * @return Weather or not the operation was successful. Will fail if the
     *         frameNo is out of range.
     * @throws IOException
     */
    boolean gotoFrame(long frameNo) throws IOException;

    /**
     * Goes to a a frame that's a sync frame (key frame) and is prior or at
     * frame frameNo.
     * 
     * @param frameNo
     *            Frame number, zero based, related to which a sync frame will
     *            be selected.
     * @return If the operation was successful.
     * @throws IOException
     */
    boolean gotoSyncFrame(long frameNo) throws IOException;

    /**
     * Gets an index of the frame that the next call to 'nextFrame' will return,
     * zero based.
     * 
     * @return An index of the next frame, zero based.
     */
    long getCurFrame();

    /**
     * Seeks this container to the second provided so that the next call to
     * nextFrame will return a frame at that second.
     * 
     * @param second
     *            A second to seek to.
     * @throws IOException
     */
    void seek(double second) throws IOException;
}
