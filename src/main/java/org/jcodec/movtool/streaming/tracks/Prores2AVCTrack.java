package org.jcodec.movtool.streaming.tracks;

import java.nio.ByteBuffer;
import java.util.EnumSet;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.encode.H264FixedRateControl;
import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.codecs.prores.ProresToThumb2x2;
import org.jcodec.codecs.prores.ProresToThumb4x4;
import org.jcodec.codecs.prores.ProresEncoder.Profile;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.model.Size;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Prores2AVCTrack extends TranscodeTrack {

    public Prores2AVCTrack(VirtualTrack proresTrack, Size frameDim) {
        super(proresTrack, frameDim);
        checkFourCC(proresTrack);
    }

    @Override
    protected VideoDecoder getDecoder(int scaleFactor) {
        return scaleFactor == 2 ? new ProresToThumb2x2() : new ProresToThumb4x4();
    }

    @Override
    protected void getCodecPrivate(ByteBuffer buffer, Size size) {
        H264Encoder encoder = new H264Encoder();
        H264Utils.toNAL(buffer, encoder.initSPS(size), encoder.initPPS());
    }

    @Override
    protected VideoEncoder getEncoder(int rate) {
        return new H264Encoder(new H264FixedRateControl(rate));
    }

    @Override
    protected int getFrameSize(int mbCount, int rate) {
        return new H264FixedRateControl(rate).calcFrameSize(mbCount);
    }
    
    private void checkFourCC(VirtualTrack proresTrack) {
        String fourcc = proresTrack.getCodecMeta().getFourcc();
        if ("ap4h".equals(fourcc))
            return;
        for (Profile profile : EnumSet.allOf(ProresEncoder.Profile.class)) {
            if (profile.fourcc.equals(fourcc))
                return;
        }
        throw new IllegalArgumentException("Input track is not ProRes");
    }
}
