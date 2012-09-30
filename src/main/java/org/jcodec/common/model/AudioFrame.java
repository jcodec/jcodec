package org.jcodec.common.model;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.io.Buffer;

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

    public AudioFrame(AudioBuffer other, long pts, long duration, long timescale) {
        super(other);
        this.pts = pts;
        this.duration = duration;
        this.timescale = timescale;
    }

    public AudioFrame(Buffer buffer, AudioFormat format, int nFrames, long pts, long duration, long timescale) {
        super(buffer, format, nFrames);
        this.pts = pts;
        this.duration = duration;
        this.timescale = timescale;
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
}
