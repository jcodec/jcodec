package org.jcodec.movtool.streaming;

import java.io.IOException;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * WebM specific muxing
 * 
 * @author The JCodec project
 * 
 */
public class VirtualWebmMovie extends VirtualMovie {

    public VirtualWebmMovie(VirtualTrack[] tracks) throws IOException {
        super(tracks);
    }

    @Override
    protected MovieSegment packetChunk(VirtualPacket pkt, int chunkNo, int track, long pos) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected MovieSegment headerChunk(List<MovieSegment> chunks, VirtualTrack[] tracks, long dataSize)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

}
