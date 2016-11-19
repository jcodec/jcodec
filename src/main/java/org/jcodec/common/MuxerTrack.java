package org.jcodec.common;

import java.io.IOException;

import org.jcodec.common.model.Packet;

/**
 * Interface for muxer track that many muxers implement.
 * 
 * @author Stanislav Vitvitskiy
 */
public interface MuxerTrack {

    void addFrame(Packet outPacket) throws IOException;

}
