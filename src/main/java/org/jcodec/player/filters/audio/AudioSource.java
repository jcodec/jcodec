package org.jcodec.player.filters.audio;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.model.AudioFrame;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.player.filters.MediaInfo;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface AudioSource {

    MediaInfo.AudioInfo getAudioInfo() throws IOException;

    AudioFrame getFrame(ByteBuffer buf) throws IOException;

    boolean drySeek(RationalLarge second) throws IOException;
    
    void seek(RationalLarge second) throws IOException;

    void close() throws IOException;
}
