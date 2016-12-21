package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import org.jcodec.codecs.vpx.VP8Encoder;
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
import org.jcodec.containers.mkv.muxer.MKVMuxer;

/**
 * Profile to convert image sequence to VP8 muxed in Webm.
 * 
 * @author Stanislav Vitvitskiy
 *
 */
class Png2webm extends FromImgTranscoder {

    private MKVMuxer muxer;

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.MKV);
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
    protected void finalizeMuxer() throws IOException {
        muxer.finish();
    }

    @Override
    protected Muxer createMuxer(SeekableByteChannel sink) throws IOException {
        muxer = new MKVMuxer(sink);
        return muxer;
    }

    @Override
    protected MuxerTrack getMuxerTrack(Muxer muxer, Picture8Bit yuv) {
        return muxer.addVideoTrack(Codec.VP8, new VideoCodecMeta(new Size(yuv.getWidth(), yuv.getHeight())));
    }

    @Override
    protected EncodedFrame encodeFrame(VideoEncoder encoder, Picture8Bit yuv, ByteBuffer buf) {
        return encoder.encodeFrame8Bit(yuv, buf);
    }
}