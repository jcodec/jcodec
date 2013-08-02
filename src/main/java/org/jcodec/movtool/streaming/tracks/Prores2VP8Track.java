package org.jcodec.movtool.streaming.tracks;

import java.nio.ByteBuffer;

import org.jcodec.codecs.prores.ProresToThumb2x2;
import org.jcodec.codecs.prores.ProresToThumb4x4;
import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.codecs.vpx.VP8FixedRateControl;
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
public class Prores2VP8Track extends TranscodeTrack {

    public Prores2VP8Track(VirtualTrack proresTrack, Size frameDim) {
        super(proresTrack, frameDim);
    }

    @Override
    protected VideoDecoder getDecoder(int scaleFactor) {
        return scaleFactor == 2 ? new ProresToThumb2x2() : new ProresToThumb4x4();
    }

    @Override
    protected VideoEncoder getEncoder(int rate) {
        return new VP8Encoder(new VP8FixedRateControl(rate));
    }

    @Override
    protected int getFrameSize(int mbCount, int rate) {
        return 0;
    }

    @Override
    protected void getCodecPrivate(ByteBuffer buf, Size dim) {
        
    }
}
