package org.jcodec.movtool.streaming;
import js.lang.IllegalStateException;
import js.lang.System;


import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class CodecMeta {
    private String fourcc;
    private ByteBuffer codecPrivate;

    public CodecMeta(String fourcc, ByteBuffer codecPrivate) {
        this.fourcc = fourcc;
        this.codecPrivate = codecPrivate;
    }
    
    public String getFourcc() {
        return fourcc;
    }
    
    public ByteBuffer getCodecPrivate() {
        return codecPrivate;
    }
}