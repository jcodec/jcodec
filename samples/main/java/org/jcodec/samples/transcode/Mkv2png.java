package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer.VideoTrack;

/**
 * A profile to convert mkv with AVC to PNG images.
 * 
 * @author Stanislav Vitvitskiy
 *
 */
class Mkv2png extends ToImgProfile {
    private AvcCBox avcC;

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MKV);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.H264);
    }

    @Override
    protected VideoDecoder getDecoder(Cmd cmd, DemuxerTrack inTrack, ByteBuffer firstFrame) throws IOException {
        H264Decoder decoder = new H264Decoder();
        decoder.addSps(avcC.getSpsList());
        decoder.addPps(avcC.getPpsList());
        return decoder;
    }

    @Override
    protected DemuxerTrack getDemuxer(Cmd cmd, SeekableByteChannel source) throws IOException {
        MKVDemuxer demux = new MKVDemuxer(source);
        DemuxerTrack inTrack = demux.getVideoTracks().get(0);
        avcC = AvcCBox.createEmpty();
        avcC.parse(((VideoTrack) inTrack).getCodecState());
        return inTrack;
    }

    @Override
    protected Picture8Bit decodeFrame(VideoDecoder decoder, Picture8Bit target1, Packet pkt) {
        return ((H264Decoder) decoder).decodeFrame8BitFromNals(H264Utils.splitMOVPacket(pkt.getData(), avcC),
                target1.getData());
    }

    @Override
    protected Packet nextPacket(DemuxerTrack inTrack) throws IOException {
        return inTrack.nextFrame();
    }
}