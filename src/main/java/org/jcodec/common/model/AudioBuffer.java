package org.jcodec.common.model;

import java.nio.ByteBuffer;

import org.jcodec.common.AudioFormat;

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
    private ByteBuffer data;
    private AudioFormat format;
    private int nFrames;

    public AudioBuffer(ByteBuffer data, AudioFormat format, int nFrames) {
        this.data = data;
        this.format = format;
        this.nFrames = nFrames;
    }

    public AudioBuffer(AudioBuffer other) {
        this.data = other.data;
        this.format = other.format;
        this.nFrames = other.nFrames;
    }

    public ByteBuffer getData() {
        return data;
    }

    public AudioFormat getFormat() {
        return format;
    }

    public int getNFrames() {
        return nFrames;
    }
}
