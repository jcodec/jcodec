package org.jcodec.movtool.streaming.tracks;
import java.lang.IllegalStateException;
import java.lang.System;
import java.lang.IllegalArgumentException;

import org.jcodec.common.CodecMeta;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Returns clips track to a certain frame range
 * 
 * @author The JCodec project
 * 
 */
public class ClipTrack implements VirtualTrack {

    private VirtualTrack src;
    private int from;
    private int to;
    private int startFrame;
    private double startPts;
    private List<VirtualPacket> gop;
    private boolean eof;

    public ClipTrack(VirtualTrack src, int frameFrom, int frameTo) {
        if (frameTo <= frameFrom)
            throw new IllegalArgumentException("Clipping negative or zero frames.");
        this.src = src;
        this.from = frameFrom;
        this.to = frameTo;
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        if (eof)
            return null;

        VirtualPacket nextPacket;

        if (gop == null) {
            gop = getGop(src, from);
            startPts = gop.get(0).getPts();
            startFrame = gop.get(0).getFrameNo();
        }

        nextPacket = gop.size() > 0 ? gop.remove(0) : src.nextPacket();

        if (nextPacket == null || nextPacket.getFrameNo() >= to) {
            eof = true;
            return null;
        }

        return new ClipPacket(this, nextPacket);
    }

    protected List<VirtualPacket> getGop(VirtualTrack src, int from) throws IOException {
        List<VirtualPacket> result = new ArrayList<VirtualPacket>();
        VirtualPacket nextPacket;
        do {
            nextPacket = src.nextPacket();
            if (nextPacket != null) {
                if (nextPacket.isKeyframe())
                    result.clear();
                result.add(nextPacket);
            }
        } while (nextPacket != null && nextPacket.getFrameNo() < from);
        return result;
    }

    @Override
    public CodecMeta getCodecMeta() {
        return src.getCodecMeta();
    }

    @Override
    public VirtualEdit[] getEdits() {
        return null;
    }

    @Override
    public int getPreferredTimescale() {
        return src.getPreferredTimescale();
    }

    @Override
    public void close() throws IOException {
        src.close();
    }

    public static class ClipPacket extends VirtualPacketWrapper {

        private ClipTrack track;

		public ClipPacket(ClipTrack track, VirtualPacket src) {
            super(src);
			this.track = track;
        }

        @Override
        public double getPts() {
            return super.getPts() - track.startPts;
        }

        @Override
        public int getFrameNo() {
            return super.getFrameNo() - track.startFrame;
        }
    }
}
