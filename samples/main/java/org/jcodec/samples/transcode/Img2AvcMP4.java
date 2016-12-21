package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.Muxer;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * Converts image sequence to AVC in MP4 container.
 * 
 * @author Stanislav Vitvitskiy
 *
 */
class Img2AvcMP4 extends FromImgTranscoder {

    private MP4Muxer muxer;

    @Override
    protected H264Encoder getEncoder() {
        return H264Encoder.createH264Encoder();
    }

    @Override
    protected Muxer createMuxer(SeekableByteChannel sink) throws IOException {
        muxer = MP4Muxer.createMP4MuxerToChannel(sink);
        return muxer;
    }

    @Override
    protected MuxerTrack getMuxerTrack(Muxer muxer, Picture8Bit yuv) {
        return muxer.addVideoTrack(Codec.H264, new VideoCodecMeta(new Size(yuv.getWidth(), yuv.getHeight())));
    }

    @Override
    protected void finalizeMuxer() throws IOException {
        muxer.finish();
    }

    @Override
    protected EncodedFrame encodeFrame(VideoEncoder encoder, Picture8Bit yuv, ByteBuffer buf) {
        return encoder.encodeFrame8Bit(yuv, buf);
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