package org.jcodec.movtool.streaming.tracks;
import js.lang.IllegalStateException;
import js.lang.System;
import js.lang.ThreadLocal;
import js.lang.IllegalArgumentException;

import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.codecs.prores.ProresEncoder.Profile;
import org.jcodec.codecs.prores.ProresToThumb2x2;
import org.jcodec.codecs.prores.ProresToThumb4x4;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.model.Size;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Prores2AVCTrack extends Transcode2AVCTrack {

    public Prores2AVCTrack(VirtualTrack proresTrack, Size frameDim) {
        super(proresTrack, frameDim);
    }

    @Override
    protected void checkFourCC(VirtualTrack proresTrack) {
        String fourcc = proresTrack.getCodecMeta().getFourcc();
        if ("ap4h".equals(fourcc))
            return;
        Profile[] values = ProresEncoder.Profile.values();
        for (int i = 0; i < values.length; i++) {
            Profile profile = values[i];
            if (profile.fourcc.equals(fourcc))
                return;
        }
        throw new IllegalArgumentException("Input track is not ProRes");
    }

    @Override
    protected int selectScaleFactor(Size frameDim) {
        return frameDim.getWidth() >= 960 ? 2 : (frameDim.getWidth() > 480 ? 1 : 0);
    }

    @Override
    protected VideoDecoder getDecoder(int scaleFactor) {
        switch (scaleFactor) {
        case 2:
            return new ProresToThumb2x2();
        case 1:
            return new ProresToThumb4x4();
        case 0:
            return new ProresDecoder();
        default:
            throw new IllegalArgumentException("Unsupported scale factor: " + scaleFactor);
        }
    }
}