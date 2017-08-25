package org.jcodec.common;
import java.lang.IllegalStateException;
import java.lang.System;


import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class CodecMeta {
    private Codec codec;
    private ByteBuffer codecPrivate;

    public CodecMeta(Codec codec, ByteBuffer codecPrivate) {
        this.codec = codec;
        this.codecPrivate = codecPrivate;
    }
    
    public Codec getCodec() {
        return codec;
    }
    
    public ByteBuffer getCodecPrivate() {
        return codecPrivate;
    }

    public VideoCodecMeta video() {
        return this instanceof VideoCodecMeta ? (VideoCodecMeta)this : null;
    }

    public boolean isVideo() {
        return this instanceof VideoCodecMeta;
    }
    
    public AudioCodecMeta audio() {
        return this instanceof AudioCodecMeta ? (AudioCodecMeta)this : null;
    }

    public boolean isAudio() {
        return this instanceof AudioCodecMeta;
    }
}