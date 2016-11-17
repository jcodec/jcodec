package org.jcodec.movtool.streaming.tracks;

import org.jcodec.codecs.mjpeg.JpegDecoder;
import org.jcodec.codecs.mjpeg.JpegToThumb2x2;
import org.jcodec.codecs.mjpeg.JpegToThumb4x4;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.model.Size;
import org.jcodec.movtool.streaming.CodecMeta;
import org.jcodec.movtool.streaming.VideoCodecMeta;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Virtual movie track that transcodes Jpeg to AVC on the fly.
 * 
 * @author The JCodec project
 * 
 */
public class Jpeg2AVCTrack extends Transcode2AVCTrack {

    public Jpeg2AVCTrack(VirtualTrack proresTrack, Size frameDim) {
        super(proresTrack, frameDim);
    }

    @Override
    protected void checkFourCC(VirtualTrack jpegTrack) {
        String fourcc = jpegTrack.getCodecMeta().getFourcc();
        if ("jpeg".equals(fourcc) || "mjpa".equals(fourcc))
            return;

        throw new IllegalArgumentException("Input track is not Jpeg");
    }

    @Override
    protected int selectScaleFactor(Size frameDim) {
        return frameDim.getWidth() >= 960 ? 2 : (frameDim.getWidth() > 480 ? 1 : 0);
    }

    @Override
    protected VideoDecoder getDecoder(int scaleFactor) {
        VideoCodecMeta meta = (VideoCodecMeta)src.getCodecMeta();
        
        JpegDecoder decoder;
        switch (scaleFactor) {
        case 2:
            decoder = new JpegToThumb2x2();
            break;
        case 1:
            decoder = new JpegToThumb4x4();
            break;
        case 0:
            decoder = new JpegDecoder();
            break;
        default:
            throw new IllegalArgumentException("Unsupported scale factor: " + scaleFactor);
        }
        decoder.setInterlace(meta.isInterlaced(), meta.isTopFieldFirst());
        return decoder;
    }
}
