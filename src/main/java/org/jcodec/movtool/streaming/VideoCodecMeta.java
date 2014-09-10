package org.jcodec.movtool.streaming;

import java.nio.ByteBuffer;

import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VideoCodecMeta extends CodecMeta {

    private Size size;
    private Rational pasp;
    private boolean interlaced;
    private boolean topFieldFirst;

    public VideoCodecMeta(String fourcc, ByteBuffer codecPrivate, Size size, Rational pasp) {
        super(fourcc, codecPrivate);
        this.size = size;
        this.pasp = pasp;
    }

    public VideoCodecMeta(String fourcc, ByteBuffer codecPrivate, Size size, Rational pasp, boolean interlaced,
            boolean topFieldFirst) {
        super(fourcc, codecPrivate);
        this.size = size;
        this.pasp = pasp;
        this.interlaced = interlaced;
        this.topFieldFirst = topFieldFirst;
    }

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