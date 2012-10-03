package org.jcodec.common.model;

import org.jcodec.common.io.Buffer;

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
    private Buffer data;
    private long pts;
    private long timescale;
    private long duration;
    private long frameNo;
    private boolean keyFrame;
    private TapeTimecode tapeTimecode;
    private int displayOrder;

    public Packet(Buffer data, long pts, long timescale, long duration, long frameNo, boolean keyFrame,
            TapeTimecode tapeTimecode) {
        this.data = data;
        this.pts = pts;
        this.timescale = timescale;
        this.duration = duration;
        this.frameNo = frameNo;
        this.keyFrame = keyFrame;
        this.tapeTimecode = tapeTimecode;
    }

    public Packet(Packet other) {
        this(other.data, other.pts, other.timescale, other.duration, other.frameNo, other.keyFrame, other.tapeTimecode);
        this.displayOrder = other.displayOrder;
    }

    public Packet(Packet other, Buffer data) {
        this(data, other.pts, other.timescale, other.duration, other.frameNo, other.keyFrame, other.tapeTimecode);
        this.displayOrder = other.displayOrder;
    }

    public Packet(Packet other, TapeTimecode timecode) {
        this(other.data, other.pts, other.timescale, other.duration, other.frameNo, other.keyFrame, timecode);
        this.displayOrder = other.displayOrder;
    }

    public Buffer getData() {
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
}
