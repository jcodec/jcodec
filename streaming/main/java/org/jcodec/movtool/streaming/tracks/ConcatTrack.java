package org.jcodec.movtool.streaming.tracks;
import java.lang.IllegalStateException;
import java.lang.System;

import org.jcodec.common.CodecMeta;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ConcatTrack implements VirtualTrack {
    private VirtualTrack[] tracks;
    private int idx = 0;
    private VirtualPacket lastPacket;
    private double offsetPts = 0;
    private int offsetFn = 0;

    public ConcatTrack(VirtualTrack[] tracks) {
        this.tracks = tracks;
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        while (idx < tracks.length) {
            VirtualTrack track = tracks[idx];
            VirtualPacket nextPacket = track.nextPacket();
            if (nextPacket == null) {
                idx++;
                offsetPts += lastPacket.getPts() + lastPacket.getDuration();
                offsetFn += lastPacket.getFrameNo() + 1;
            } else {
                lastPacket = nextPacket;
                return new ConcatPacket(nextPacket, offsetPts, offsetFn);
            }
        }
        return null;
    }

    @Override
    public CodecMeta getCodecMeta() {
        return tracks[0].getCodecMeta();
    }

    @Override
    public VirtualEdit[] getEdits() {
        return null;
    }

    @Override
    public int getPreferredTimescale() {
        return tracks[0].getPreferredTimescale();
    }

    @Override
    public void close() throws IOException {
        for (int i = 0; i < tracks.length; i++) {
            tracks[i].close();
        }
    }
}