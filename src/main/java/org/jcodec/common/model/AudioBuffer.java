package org.jcodec.common.model;

import javax.sound.sampled.AudioFormat;

import org.jcodec.common.io.Buffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Decoded audio samples
 * 
 * @author The JCodec project
 * 
 */
public class AudioBuffer {
    private Buffer data;
    private AudioFormat format;
    private int nFrames;

    public AudioBuffer(Buffer data, AudioFormat format, int nFrames) {
        this.data = data;
        this.format = format;
        this.nFrames = nFrames;
    }

    public AudioBuffer(AudioBuffer other) {
        this.data = other.data;
        this.format = other.format;
        this.nFrames = other.nFrames;
    }

    public Buffer getData() {
        return data;
    }

    public AudioFormat getFormat() {
        return format;
    }

    public int getNFrames() {
        return nFrames;
    }
}
