package org.jcodec.movtool.streaming.tracks;

import org.jcodec.codecs.mjpeg.JpegDecoder;
import org.jcodec.codecs.mjpeg.JpegToThumb2x2;
import org.jcodec.codecs.mjpeg.JpegToThumb4x4;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.FielExtension;
import org.jcodec.containers.mp4.boxes.SampleEntry;
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
    protected void checkFourCC(VirtualTrack proresTrack) {
        String fourcc = proresTrack.getSampleEntry().getFourcc();
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
        SampleEntry srcSE = src.getSampleEntry();
        FielExtension fiel = Box.findFirst(srcSE, FielExtension.class, "fiel");
        boolean interlace = false, topField = false;
        if(fiel != null) {
            interlace = fiel.isInterlaced();
            topField = fiel.topFieldFirst();
        }
        
        
        switch (scaleFactor) {
        case 2:
            return new JpegToThumb2x2(interlace, topField);
        case 1:
            return new JpegToThumb4x4(interlace, topField);
        case 0:
            return new JpegDecoder(interlace, topField);
        default:
            throw new IllegalArgumentException("Unsupported scale factor: " + scaleFactor);
        }
    }
}
