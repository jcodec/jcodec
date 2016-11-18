package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import org.jcodec.codecs.vp8.VP8Decoder;
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
 * A profile to convert WEBM VP8 to images
 * 
 * @author Stanislav Vitvitskiy
 */
class Webm2png extends ToImgProfile {
    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MKV);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.VP8);
    }

    @Override
    protected VideoDecoder getDecoder(Cmd cmd, DemuxerTrack inTrack, ByteBuffer firstFrame) throws IOException {
        return new VP8Decoder();
    }

    @Override
    protected DemuxerTrack getDemuxer(Cmd cmd, SeekableByteChannel source) throws IOException {
        MKVDemuxer demux = new MKVDemuxer(source);
        return demux.getVideoTracks().get(0);
    }

    @Override
    protected Picture8Bit decodeFrame(VideoDecoder decoder, Picture8Bit target1, Packet pkt) {
        if (!pkt.isKeyFrame())
            return null;

        try {
            return decoder.decodeFrame8Bit(pkt.getData(), target1.getData());
        } catch (AssertionError ae) {
            ae.printStackTrace(System.err);
            return null;
        }
    }

    @Override
    protected Packet nextPacket(DemuxerTrack inTrack) throws IOException {
        return inTrack.nextFrame();
    }
}