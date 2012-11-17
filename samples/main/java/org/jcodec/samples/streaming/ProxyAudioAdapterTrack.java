package org.jcodec.samples.streaming;

import java.io.IOException;

import org.jcodec.common.model.Packet;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.samples.streaming.Adapter.AudioAdapterTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Base class for adaptor audio adapter tracks
 * 
 * @author The JCodec project
 * 
 */
public class ProxyAudioAdapterTrack implements AudioAdapterTrack {
    private AudioAdapterTrack src;

    public ProxyAudioAdapterTrack(AudioAdapterTrack src) {
        this.src = src;
    }

    @Override
    public MediaInfo getMediaInfo() throws IOException {
        return src.getMediaInfo();
    }

    @Override
    public int search(long pts) throws IOException {
        return src.search(pts);
    }

    @Override
    public Packet getFrame(int frameId) throws IOException {
        return src.getFrame(frameId);
    }
}
