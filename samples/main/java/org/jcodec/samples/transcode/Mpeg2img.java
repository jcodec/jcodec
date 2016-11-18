package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MathUtil;

class Mpeg2img extends MPSToImg {
    private static final String FLAG_DOWNSCALE = "downscale";

    @Override
    protected VideoDecoder getDecoder(Cmd cmd, DemuxerTrack inTrack, ByteBuffer firstFrame) throws IOException {
        VideoDecoder decoder = super.getDecoder(cmd, inTrack, firstFrame);
        Integer downscale = cmd.getIntegerFlag(FLAG_DOWNSCALE);
        if (downscale != null) {
            decoder = decoder.downscaled(downscale);
            if (decoder == null) {
                System.out.println("Could not create decoder for downscale ratio: " + downscale);
            }
        }
        return decoder;
    }

    @Override
    protected boolean validateArguments(Cmd cmd) {
        Integer downscale = cmd.getIntegerFlag(FLAG_DOWNSCALE);
        if (downscale != null && (1 << MathUtil.log2(downscale)) != downscale) {
            Logger.error("Only values [2, 4] are supported for " + FLAG_DOWNSCALE);
            return false;
        }
        return super.validateArguments(cmd);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.MPEG2);
    }

    protected DemuxerTrackMeta getTrackMeta(DemuxerTrack inTrack, ByteBuffer firstFrame) {
        return MPEGDecoder.getMeta(firstFrame);
    }
}