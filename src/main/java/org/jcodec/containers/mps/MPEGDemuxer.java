package org.jcodec.containers.mps;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public interface MPEGDemuxer extends Demuxer {
    List<? extends MPEGDemuxerTrack> getTracks();
    List<? extends MPEGDemuxerTrack> getVideoTracks();
    List<? extends MPEGDemuxerTrack> getAudioTracks();
    
    void seekByte(long offset) throws IOException;

    public static interface MPEGDemuxerTrack extends DemuxerTrack {
        Packet nextFrameWithBuffer(ByteBuffer buf) throws IOException;
        DemuxerTrackMeta getMeta();
        void ignore();
    }
}