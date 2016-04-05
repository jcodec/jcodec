package org.jcodec.movtool.streaming;
import js.lang.IllegalStateException;
import js.lang.System;


import org.jcodec.containers.mkv.MKVStreamingMuxer;

import js.io.IOException;
import js.util.ArrayList;
import js.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 * 
 * WebM specific muxing
 * 
 * @author The JCodec project
 * 
 */
public class VirtualWebmMovie extends VirtualMovie {
    
    private MKVStreamingMuxer muxer = null;

    public VirtualWebmMovie(VirtualTrack... arguments) throws IOException {
        super(arguments);
        muxer = new MKVStreamingMuxer();
        muxTracks();
    }
    
    @Override
    protected void muxTracks() throws IOException {
        List<MovieSegment> chch = new ArrayList<MovieSegment>();
        VirtualPacket[] heads = new VirtualPacket[tracks.length], tails = new VirtualPacket[tracks.length];
        long currentlyAddedContentSize = 0;
        for (int curChunk = 1;; curChunk++) {
            int min = -1;

            for (int i = 0; i < heads.length; i++) {
                if (heads[i] == null) {
                    heads[i] = tracks[i].nextPacket();
                    if (heads[i] == null)
                        continue;
                }

                min = min == -1 || heads[i].getPts() < heads[min].getPts() ? i : min;
            }
            if (min == -1)
                break;
            MovieSegment packetChunk = packetChunk(tracks[min], heads[min], curChunk, min, currentlyAddedContentSize);
            chch.add(packetChunk);
            currentlyAddedContentSize += packetChunk.getDataLen();
            tails[min] = heads[min];
            heads[min] = tracks[min].nextPacket();
        }
        
        _headerChunk = headerChunk(chch, tracks, _size);
        _size += _headerChunk.getDataLen()+currentlyAddedContentSize;
        
        chunks = chch.toArray(new MovieSegment[0]);
    }

    @Override
    protected MovieSegment packetChunk(VirtualTrack track, VirtualPacket pkt, int chunkNo, int trackNo, long previousClustersSize) {
        return muxer.preparePacket(track, pkt, chunkNo, trackNo, previousClustersSize);
    }

    @Override
    protected MovieSegment headerChunk(List<MovieSegment> chunks, VirtualTrack[] tracks, long dataSize) throws IOException {
        return muxer.prepareHeader(chunks, tracks);
    }
}