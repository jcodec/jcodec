package org.jcodec.player.filters;

import java.io.IOException;

import org.jcodec.common.model.AudioFrame;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface AudioSource {

    MediaInfo.AudioInfo getAudioInfo() throws IOException;

    AudioFrame getFrame(byte[] buf) throws IOException;

    boolean drySeek(long clock, long timescale) throws IOException;
    
    void seek(long clock, long timescale) throws IOException;

    void close() throws IOException;
}
