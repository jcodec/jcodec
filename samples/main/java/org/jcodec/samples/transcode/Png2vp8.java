package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import org.jcodec.codecs.vpx.IVFMuxer;
import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;

/**
 * Image sequence to VP8 muxed into IVF encoding profile.
 * 
 * @author Stanislav Vitvitskiy
 *
 */
class Png2vp8 extends FromImgTranscoder {
    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.IVF);
    }

    @Override
    public Set<Codec> outputVideoCodec() {
        return TranscodeMain.codecs(Codec.VP8);
    }

    @Override
    protected VideoEncoder getEncoder() {
        return VP8Encoder.createVP8Encoder(10); // qp
    }

    @Override
    protected MuxerTrack getMuxerTrack(SeekableByteChannel sink, DemuxerTrackMeta inTrackMeta, Picture8Bit yuv,
            Packet firstPacket) throws IOException {
        return new IVFMuxer(sink, yuv.getWidth(), yuv.getHeight(), (int) firstPacket.getTimescale());
    }

    @Override
    protected void finalizeMuxer() throws IOException {
    }

    @Override
    protected Packet encodeFrame(VideoEncoder encoder, Picture8Bit yuv, Packet inPacket, ByteBuffer buf) {
        ByteBuffer frame = encoder.encodeFrame8Bit(yuv, buf);
        return Packet.createPacketWithData(inPacket, frame);
    }
}