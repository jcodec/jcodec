package org.jcodec.common.model;

import java.nio.ByteBuffer;
import java.util.Comparator;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Encoded stream packet
 * 
 * @author The JCodec project
 * 
 */
public class Packet {
    private ByteBuffer data;
    private long pts;
    private long timescale;
    private long duration;
    private long frameNo;
    private boolean keyFrame;
    private TapeTimecode tapeTimecode;
    private int displayOrder;

    public Packet(ByteBuffer data, long pts, long timescale, long duration, long frameNo, boolean keyFrame,
            TapeTimecode tapeTimecode) {
        this(data, pts, timescale, duration, frameNo, keyFrame, tapeTimecode, 0);
    }

    public Packet(Packet other) {
        this(other.data, other.pts, other.timescale, other.duration, other.frameNo, other.keyFrame, other.tapeTimecode);
        this.displayOrder = other.displayOrder;
    }

    public Packet(Packet other, ByteBuffer data) {
        this(data, other.pts, other.timescale, other.duration, other.frameNo, other.keyFrame, other.tapeTimecode);
        this.displayOrder = other.displayOrder;
    }

    public Packet(Packet other, TapeTimecode timecode) {
        this(other.data, other.pts, other.timescale, other.duration, other.frameNo, other.keyFrame, timecode);
        this.displayOrder = other.displayOrder;
    }

    public Packet(ByteBuffer data, long pts, long timescale, long duration, long frameNo, boolean keyFrame,
            TapeTimecode tapeTimecode, int displayOrder) {
        this.data = data;
        this.pts = pts;
        this.timescale = timescale;
        this.duration = duration;
        this.frameNo = frameNo;
        this.keyFrame = keyFrame;
        this.tapeTimecode = tapeTimecode;
        this.displayOrder = displayOrder;
    }

    public ByteBuffer getData() {
        return data;
    }

    public long getPts() {
        return pts;
    }

    public long getTimescale() {
        return timescale;
    }

    public long getDuration() {
        return duration;
    }

    public long getFrameNo() {
        return frameNo;
    }

    public void setTimescale(int timescale) {
        this.timescale = timescale;
    }

    public TapeTimecode getTapeTimecode() {
        return tapeTimecode;
    }

    public void setTapeTimecode(TapeTimecode tapeTimecode) {
        this.tapeTimecode = tapeTimecode;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isKeyFrame() {
        return keyFrame;
    }

    public RationalLarge getPtsR() {
        return RationalLarge.R(pts, timescale);
    }
    
    public double getPtsD() {
        return ((double) pts) / timescale;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }

    public static final Comparator<Packet> FRAME_ASC = new Comparator<Packet>() {
        public int compare(Packet o1, Packet o2) {
            if (o1 == null && o2 == null)
                return 0;
            if (o1 == null)
                return -1;
            if (o2 == null)
                return 1;
            return o1.frameNo < o2.frameNo ? -1 : (o1.frameNo == o2.frameNo ? 0 : 1);
        }
    };
}
