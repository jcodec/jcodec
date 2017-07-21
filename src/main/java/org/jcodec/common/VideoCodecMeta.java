package org.jcodec.common;
import java.lang.IllegalStateException;
import java.lang.System;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;

import java.nio.ByteBuffer;

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
    private ColorSpace color;

    public Size getSize() {
        return size;
    }

    public Rational getPasp() {
        return pasp;
    }

    public Rational getPixelAspectRatio() {
        return pasp;
    }

    public boolean isInterlaced() {
        return interlaced;
    }

    public boolean isTopFieldFirst() {
        return topFieldFirst;
    }

    public ColorSpace getColor() {
        return color;
    }

    public static VideoCodecMeta createSimpleVideoCodecMeta(Size size, ColorSpace color) {
        VideoCodecMeta self = new VideoCodecMeta(null, null);
        self.size = size;
        self.color = color;
        return self;
    }

    public void setPixelAspectRatio(Rational pasp) {
        this.pasp = pasp;
    }
}