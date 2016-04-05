package org.jcodec.movtool.streaming;
import js.lang.IllegalStateException;
import js.lang.System;


import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;

import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VideoCodecMeta extends CodecMeta {

    public static VideoCodecMeta createVideoCodecMeta(String fourcc, ByteBuffer codecPrivate, Size size,
            Rational pasp) {
        VideoCodecMeta self = new VideoCodecMeta(fourcc, codecPrivate);
        self.size = size;
        self.pasp = pasp;
        return self;
    }

    public static VideoCodecMeta createVideoCodecMeta2(String fourcc, ByteBuffer codecPrivate, Size size, Rational pasp,
            boolean interlaced, boolean topFieldFirst) {
        VideoCodecMeta self = new VideoCodecMeta(fourcc, codecPrivate);
        self.size = size;
        self.pasp = pasp;
        self.interlaced = interlaced;
        self.topFieldFirst = topFieldFirst;
        return self;
    }

    public VideoCodecMeta(String fourcc, ByteBuffer codecPrivate) {
        super(fourcc, codecPrivate);
    }

    private Size size;
    private Rational pasp;
    private boolean interlaced;
    private boolean topFieldFirst;

    public Size getSize() {
        return size;
    }

    public Rational getPasp() {
        return pasp;
    }

    public boolean isInterlaced() {
        return interlaced;
    }

    public boolean isTopFieldFirst() {
        return topFieldFirst;
    }
}