package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;

/**
 * A profile to convert mkv with AVC to PNG images.
 * 
 * @author Stanislav Vitvitskiy
 *
 */
class Mkv2png extends ToImgTranscoder {
    private H264Decoder decoder;

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MKV);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.H264);
    }

    @Override
    protected VideoDecoder getDecoder(Cmd cmd, DemuxerTrack inTrack, ByteBuffer firstFrame) {
        return decoder;
    }

    @Override
    protected DemuxerTrack getDemuxer(Cmd cmd, SeekableByteChannel source) throws IOException {
        MKVDemuxer demux = new MKVDemuxer(source);
        DemuxerTrack inTrack = demux.getVideoTracks().get(0);
        decoder = H264Decoder.createH264DecoderFromCodecPrivate(inTrack.getMeta().getCodecPrivate());
        return inTrack;
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