package org.jcodec.movtool.streaming;
import java.lang.IllegalStateException;
import java.lang.System;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Virtual movie. A movie constructed on-the-fly from virtual track data.
 * 
 * Generic muxing
 * 
 * @author The JCodec project
 * 
 */
public abstract class VirtualMovie {
    public MovieSegment[] chunks;
    public MovieSegment _headerChunk;
    protected long _size;
    protected VirtualTrack[] tracks;

    public VirtualMovie(VirtualTrack[] virtualTracks) throws IOException {
        this.tracks = virtualTracks;
    }

    protected void muxTracks() throws IOException {
        List<MovieSegment> chch = new ArrayList<MovieSegment>();
        VirtualPacket[] heads = new VirtualPacket[tracks.length], tails = new VirtualPacket[tracks.length];

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

            chch.add(packetChunk(tracks[min], heads[min], curChunk, min, _size));
            if (heads[min].getDataLen() >= 0)
                _size += heads[min].getDataLen();
            else
                System.err.println("WARN: Negative frame data len!!!");
            tails[min] = heads[min];
            heads[min] = tracks[min].nextPacket();
        }

        _headerChunk = headerChunk(chch, tracks, _size);
        _size += _headerChunk.getDataLen();

        chunks = chch.toArray(new MovieSegment[0]);
    }

    protected abstract MovieSegment packetChunk(VirtualTrack track, VirtualPacket pkt, int chunkNo, int trackNo,
            long pos);

    protected abstract MovieSegment headerChunk(List<MovieSegment> chunks, VirtualTrack[] tracks, long dataSize)
            throws IOException;

    public void close() throws IOException {
        for (VirtualTrack virtualTrack : tracks) {
            virtualTrack.close();
        }
    }

    public MovieSegment getPacketAt(long position) throws IOException {
        if (position >= 0 && position < _headerChunk.getDataLen())
            return _headerChunk;
        for (int i = 0; i < chunks.length - 1; i++) {
            if (chunks[i + 1].getPos() > position)
                return chunks[i];
        }
        if (position < _size)
            return chunks[chunks.length - 1];
        return null;
    }

    public MovieSegment getPacketByNo(int no) {
        if (no > chunks.length)
            return null;
        if (no == 0)
            return _headerChunk;
        return chunks[no - 1];
    }

    public long size() {
        return _size;
    }
}