package org.jcodec.movtool.streaming.tracks;
import java.lang.IllegalStateException;
import java.lang.System;
import java.lang.IllegalArgumentException;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.encode.H264FixedRateControl;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.mpeg12.Mpeg2Thumb2x2;
import org.jcodec.codecs.mpeg12.Mpeg2Thumb4x4;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;
import org.jcodec.common.model.Size;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * An MPEG thumbnail to AVC transcoder implemented fully in java ( using jcodec
 * codecs ).
 * 
 * @author The JCodec project
 * 
 */
public class MPEGToAVCTranscoder {
    public static final int TARGET_RATE = 1024;
    private VideoDecoder decoder;
    private H264Encoder encoder;
    private Picture pic0;
    private Picture pic1;
    private Transform transform;
    private H264FixedRateControl rc;
    private int scaleFactor;
    private int thumbWidth;
    private int thumbHeight;

    public MPEGToAVCTranscoder(int scaleFactor) {
        this.scaleFactor = scaleFactor;
        rc = new H264FixedRateControl(MPEGToAVCTranscoder.TARGET_RATE);
        this.decoder = getDecoder(scaleFactor);
        this.encoder = new H264Encoder(rc);
    }

    protected VideoDecoder getDecoder(int scaleFactor) {
        switch (scaleFactor) {
        case 2:
            return new Mpeg2Thumb2x2();
        case 1:
            return new Mpeg2Thumb4x4();
        case 0:
            return new MPEGDecoder();
        default:
            throw new IllegalArgumentException("Unsupported scale factor: " + scaleFactor);
        }
    }

    public ByteBuffer transcodeFrame(ByteBuffer src, ByteBuffer dst, boolean iframe, int poc) throws IOException {
        if (src == null)
            return null;

        if (pic0 == null) {
            Size size = new MPEGDecoder().getCodecMeta(src.duplicate()).getSize();
            thumbWidth = size.getWidth() >> this.scaleFactor;
            thumbHeight = size.getHeight() >> this.scaleFactor;
            int mbW = (thumbWidth + 8) >> 4;
            int mbH = (thumbHeight + 8) >> 4;

            pic0 = Picture.create(mbW << 4, (mbH + 1) << 4, ColorSpace.YUV444);
        }
        Picture decoded = decoder.decodeFrame(src, pic0.getData());
        if (pic1 == null) {
            pic1 = Picture.create(decoded.getWidth(), decoded.getHeight(), encoder.getSupportedColorSpaces()[0]);
            transform = ColorUtil.getTransform(decoded.getColor(), encoder.getSupportedColorSpaces()[0]);
        }
        Picture toEnc;
        if (transform != null) {
            transform.transform(decoded, pic1);
            toEnc = pic1;
        } else {
            toEnc = decoded;
        }
        pic1.setCrop(new Rect(0, 0, thumbWidth, thumbHeight));
        int rate = MPEGToAVCTranscoder.TARGET_RATE;
        do {
            try {
                encoder.doEncodeFrame(toEnc, dst, iframe, poc, SliceType.I);
                break;
            } catch (BufferOverflowException ex) {
                Logger.warn("Abandon frame, buffer too small: " + dst.capacity());
                rate -= 10;
                rc.setRate(rate);
            }
        } while (rate > 10);
        rc.setRate(MPEGToAVCTranscoder.TARGET_RATE);

        H264Utils.encodeMOVPacketInplace(dst);

        return dst;
    }

    protected static MPEGToAVCTranscoder createTranscoder(int scaleFactor) {
        return new MPEGToAVCTranscoder(scaleFactor);
    }
}