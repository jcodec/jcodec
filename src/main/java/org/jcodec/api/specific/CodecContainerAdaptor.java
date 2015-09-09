package org.jcodec.api.specific;

import java.io.IOException;

import org.jcodec.api.FrameGrab.MediaInfo;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A FrameGrab adapter for dealing with a specific combination of
 * container/codec.
 * 
 * Dealing with different codecs inside different containers is many times way
 * too different so it makes sense to have separate code doing it
 * 
 * @author The JCodec project
 * 
 */
public abstract class CodecContainerAdaptor {
    
    private SeekableDemuxerTrack track;

    public CodecContainerAdaptor(SeekableDemuxerTrack track) {
        this.track = track;
    }
    
    protected SeekableDemuxerTrack track() {
        return track;
    }

    public abstract Picture nextFrame() throws IOException;

    public abstract MediaInfo getMediaInfo();

    public abstract void seek(double second) throws IOException;

    public abstract void gotoFrame(int frameNumber) throws IOException;

    public abstract void seekToKeyFrame(double second) throws IOException;

    public abstract void gotoToKeyFrame(int frameNumber) throws IOException;
}
