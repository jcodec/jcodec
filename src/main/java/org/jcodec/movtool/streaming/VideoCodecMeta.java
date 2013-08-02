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

    public VideoCodecMeta(String fourcc, Size size, Rational pasp, ByteBuffer codecPrivate) {
        super(fourcc, codecPrivate);
        this.size = size;
        this.pasp = pasp;
    }

    public Size getSize() {
        return size;
    }

    public Rational getPasp() {
        return pasp;
    }
}