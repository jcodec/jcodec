package org.jcodec.common;

import org.jcodec.common.model.Packet;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface DemuxerTrack {
    Packet nextFrame() throws IOException;
    
    DemuxerTrackMeta getMeta();
}
