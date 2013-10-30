package org.jcodec.containers.mps;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.common.model.Packet;

public interface MPEGDemuxer {
    List<? extends Track> getTracks();
    List<? extends Track> getVideoTracks();
    List<? extends Track> getAudioTracks();
    
    void seekByte(long offset) throws IOException;

    static interface Track {
        Packet nextFrame(ByteBuffer buf) throws IOException;
    }
}
