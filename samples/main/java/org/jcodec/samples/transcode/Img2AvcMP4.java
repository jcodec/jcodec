package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * Converts image sequence to AVC in MP4 container.
 * 
 * @author Stanislav Vitvitskiy
 *
 */
class Img2AvcMP4 extends FromImgProfile {

    private MP4Muxer muxer;
    private FramesMP4MuxerTrack track;
    private List<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
    private List<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();

    @Override
    protected H264Encoder getEncoder() {
        return H264Encoder.createH264Encoder();
    }

    @Override
    protected FramesMP4MuxerTrack getMuxerTrack(SeekableByteChannel sink, DemuxerTrackMeta inTrackMeta, Picture8Bit yuv,  Packet firstPacket)
            throws IOException {
        muxer = MP4Muxer.createMP4MuxerToChannel(sink);
        track = muxer.addTrack(MP4TrackType.VIDEO, (int)firstPacket.getTimescale());
        return track;
    }

    @Override
    protected void finalizeMuxer() throws IOException {
        track.addSampleEntry(
                H264Utils.createMOVSampleEntryFromSpsPpsList(spsList.subList(0, 1), ppsList.subList(0, 1), 4));

        muxer.writeHeader();
    }

    @Override
    protected MP4Packet encodeFrame(VideoEncoder encoder, Picture8Bit yuv, Packet inPacket, ByteBuffer buf) {
        ByteBuffer ff = encoder.encodeFrame8Bit(yuv, buf);
        H264Utils.wipePSinplace(ff, spsList, ppsList);
        NALUnit nu = NALUnit.read(NIOUtils.from(ff.duplicate(), 4));
        H264Utils.encodeMOVPacket(ff);
        MP4Packet createMP4Packet = MP4Packet.createMP4Packet(ff, inPacket.getPts(), inPacket.getTimescale(),
                inPacket.getDuration(), inPacket.getFrameNo(), nu.type == NALUnitType.IDR_SLICE, null,
                inPacket.getDisplayOrder(), inPacket.getPts(), 0);
        return createMP4Packet;
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.MOV);
    }

    @Override
    public Set<Codec> outputVideoCodec() {
        return TranscodeMain.codecs(Codec.H264);
    }
}