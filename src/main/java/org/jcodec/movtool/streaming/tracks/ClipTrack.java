package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;

import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

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

    private ClipTrack src;
    private int from;
    private int to;
    private double startPts;

    public ClipTrack(ClipTrack src, int frameFrom, int frameTo) {
        this.src = src;
        this.from = frameFrom;
        this.to = frameTo;
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        VirtualPacket nextPacket;

        while ((nextPacket = src.nextPacket()) != null && nextPacket.getFrameNo() < from)
            startPts = nextPacket.getPts();

        if (nextPacket == null || nextPacket.getFrameNo() >= to)
            return null;

        return new ClipPacket(nextPacket);
    }

    @Override
    public SampleEntry getSampleEntry() {
        return src.getSampleEntry();
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

    public class ClipPacket extends VirtualPacketWrapper {

        public ClipPacket(VirtualPacket src) {
            super(src);
        }

        @Override
        public double getPts() {
            return super.getPts() - startPts;
        }

        @Override
        public int getFrameNo() {
            return super.getFrameNo() - from;
        }
    }
}
