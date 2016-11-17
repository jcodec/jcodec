package org.jcodec.common;

import java.io.Closeable;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface Demuxer extends Closeable {

    List<? extends DemuxerTrack> getTracks();
    
    List<? extends DemuxerTrack> getVideoTracks();

    List<? extends DemuxerTrack> getAudioTracks();

}
