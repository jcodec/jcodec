package org.jcodec.common;


/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface SeekableDemuxerTrack extends DemuxerTrack {

    boolean gotoFrame(long i);

    long getCurFrame();

    void seek(double second);
}
