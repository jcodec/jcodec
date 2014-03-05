package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.encode.ConstantRateControl;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.mpeg12.Mpeg2Thumb2x2;
import org.jcodec.codecs.mpeg12.Mpeg2Thumb4x4;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;
import org.jcodec.common.model.Size;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

/**
 * An MPEG thumbnail to AVC transcoder implemented fully in java ( using jcodec
 * codecs ).
 * 
 * @author Jay Codec
 * 
 */
public class MPEGToAVCTranscoder {
    private VideoDecoder decoder;
    private H264Encoder encoder;
    private Picture pic0;
    private Picture pic1;
    private Transform transform;
    private ConstantRateControl rc;
    private int scaleFactor;
    private int thumbWidth;
    private int thumbHeight;

    public MPEGToAVCTranscoder(int scaleFactor) {
        this.scaleFactor = scaleFactor;
        rc = new ConstantRateControl(Mpeg2AVCTrack.TARGET_RATE);
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
            Size size = MPEGDecoder.getSize(src.duplicate());
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
        int rate = Mpeg2AVCTrack.TARGET_RATE;
        do {
            try {
                encoder.encodeFrame(toEnc, dst, iframe, poc);
                break;
            } catch (BufferOverflowException ex) {
                System.out.println("Abandon frame!!!");
                rate -= 10;
                rc.setRate(rate);
            }
        } while (rate > 10);
        rc.setRate(Mpeg2AVCTrack.TARGET_RATE);

        H264Utils.encodeMOVPacket(dst);

        return dst;
    }
}