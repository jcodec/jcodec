package org.jcodec.containers.mps;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.model.Packet;

import js.io.IOException;
import js.nio.ByteBuffer;
import js.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
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