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
    protected ByteBuffer data;
    protected AudioFormat format;
    protected int nFrames;

    public AudioBuffer(ByteBuffer data, AudioFormat format, int nFrames) {
        this.data = data;
        this.format = format;
        this.nFrames = nFrames;
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
