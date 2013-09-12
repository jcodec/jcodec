package org.jcodec.common.model;

import java.nio.ByteBuffer;

import org.jcodec.common.AudioFormat;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class AudioFrame extends AudioBuffer {
    private long pts;
    private long duration;
    private long timescale;
    private int frameNo;

    public AudioFrame(AudioBuffer other, long pts, long duration, long timescale, int frameNo) {
        super(other);
        this.pts = pts;
        this.duration = duration;
        this.timescale = timescale;
        this.frameNo = frameNo;
    }

    public AudioFrame(ByteBuffer buffer, AudioFormat format, int nFrames, long pts, long duration, long timescale, int frameNo) {
        super(buffer, format, nFrames);
        this.pts = pts;
        this.duration = duration;
        this.timescale = timescale;
        this.frameNo = frameNo;
    }

    public long getPts() {
        return pts;
    }

    public long getDuration() {
        return duration;
    }

    public long getTimescale() {
        return timescale;
    }

    public int getFrameNo() {
        return frameNo;
    }
}
