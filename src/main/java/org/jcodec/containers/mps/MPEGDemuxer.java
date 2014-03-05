package org.jcodec.containers.mps;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.model.Packet;

public interface MPEGDemuxer {
    List<? extends MPEGDemuxerTrack> getTracks();
    List<? extends MPEGDemuxerTrack> getVideoTracks();
    List<? extends MPEGDemuxerTrack> getAudioTracks();
    
    void seekByte(long offset) throws IOException;

    public static interface MPEGDemuxerTrack {
        Packet nextFrame(ByteBuffer buf) throws IOException;
        DemuxerTrackMeta getMeta();
        void ignore();
    }
}