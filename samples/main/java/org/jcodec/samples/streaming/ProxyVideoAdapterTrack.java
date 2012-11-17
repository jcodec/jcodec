package org.jcodec.samples.streaming;

import java.io.IOException;

import org.jcodec.common.model.Packet;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.samples.streaming.Adapter.VideoAdapterTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Base class for adaptor video adapter tracks
 * 
 * @author The JCodec project
 * 
 */
public class ProxyVideoAdapterTrack implements VideoAdapterTrack {

    protected VideoAdapterTrack src;

    public ProxyVideoAdapterTrack(VideoAdapterTrack src) {
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
    public Packet[] getGOP(int gopId) throws IOException {
        return src.getGOP(gopId);
    }

    @Override
    public int gopId(int frameNo) {
        return src.gopId(frameNo);
    }
}
