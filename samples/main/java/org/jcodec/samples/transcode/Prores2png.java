package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.prores.ProresToThumb;
import org.jcodec.codecs.prores.ProresToThumb2x2;
import org.jcodec.codecs.prores.ProresToThumb4x4;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MathUtil;
import org.jcodec.containers.mp4.demuxer.FramesMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

class Prores2png extends ToImgTranscoder {
    private static final String FLAG_DOWNSCALE = "downscale";

    @Override
    protected void populateAdditionalFlags(Map<String, String> flags) {
        flags.put(FLAG_DOWNSCALE, "Downscale factor, i.e. [2, 4, 8].");
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MOV);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.IMG);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.PRORES);
    }

    @Override
    public Set<Codec> outputVideoCodec() {
        return TranscodeMain.codecs(Codec.PNG);
    }

    @Override
    public Set<Codec> inputAudioCodec() {
        return null;
    }

    @Override
    public Set<Codec> outputAudioCodec() {
        return null;
    }

    @Override
    protected VideoDecoder getDecoder(Cmd cmd, DemuxerTrack inTrack, ByteBuffer firstFrame) {
        Integer downscale = cmd.getIntegerFlag(FLAG_DOWNSCALE);
        if (downscale == null) {
            return new ProresDecoder();
        } else if (2 == downscale) {
            return new ProresToThumb4x4();
        } else if (4 == downscale) {
            return new ProresToThumb2x2();
        } else if (8 == downscale) {
            return new ProresToThumb();
        } else {
            throw new IllegalArgumentException("Unsupported downscale factor: " + downscale + ".");
        }
    }

    @Override
    protected boolean validateArguments(Cmd cmd) {
        Integer downscale = cmd.getIntegerFlag(FLAG_DOWNSCALE);
        if (downscale != null && (1 << MathUtil.log2(downscale)) != downscale) {
            Logger.error("Only values [2, 4, 8] are supported for " + FLAG_DOWNSCALE);
            return false;
        }
        return super.validateArguments(cmd);
    }

    @Override
    protected DemuxerTrack getDemuxer(Cmd cmd, SeekableByteChannel source) throws IOException {
        MP4Demuxer rawDemuxer = new MP4Demuxer(source);
        return (FramesMP4DemuxerTrack) rawDemuxer.getVideoTrack();
    }

    @Override
    protected Picture8Bit decodeFrame(VideoDecoder decoder, Picture8Bit target1, ByteBuffer pkt) {
        return decoder.decodeFrame8Bit(pkt, target1.getData());
    }

    @Override
    protected Packet nextPacket(DemuxerTrack inTrack) throws IOException {
        return inTrack.nextFrame();
    }
}