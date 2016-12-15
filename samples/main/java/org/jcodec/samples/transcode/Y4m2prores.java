package org.jcodec.samples.transcode;

import static org.jcodec.common.model.Rational.HALF;
import static org.jcodec.common.model.Unit.SEC;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.codecs.raw.RAWVideoDecoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.y4m.Y4MDemuxer;

/**
 * Y4M decoder to prores.
 * 
 * @author Stanislav Vitvitskyy
 */
class Y4m2prores extends V2VTranscoder {
    static final String APPLE_PRO_RES_422 = "Apple ProRes 422";

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.Y4M);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.MOV);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.RAW);
    }

    @Override
    public Set<Codec> outputVideoCodec() {
        return TranscodeMain.codecs(Codec.PRORES);
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
    public GenericTranscoder getTranscoder(Cmd cmd, Profile profile) {
        return new Y4m2proresTranscoder(cmd, profile);
    }

    private static class Y4m2proresTranscoder extends GenericTranscoder {

        private MP4Muxer muxer;
        private Y4MDemuxer videoInputTrack;
        private FramesMP4MuxerTrack videoOutputTrack;
        private ProresEncoder encoder;
        private VideoDecoder videoDecoder;
        
        public Y4m2proresTranscoder(Cmd cmd, Profile profile) {
            super(cmd, profile);
        }

        @Override
        protected void initDecode(SeekableByteChannel source) throws IOException {
            videoInputTrack = new Y4MDemuxer(source);
            Size dim = videoInputTrack.getMeta().getDimensions();
            videoDecoder = new RAWVideoDecoder(dim.getWidth(), dim.getHeight());
        }

        @Override
        protected void initEncode(SeekableByteChannel sink) throws IOException {
            muxer = MP4Muxer.createMP4MuxerToChannel(sink);
            encoder = new ProresEncoder(ProresEncoder.Profile.HQ, false);

            videoOutputTrack = muxer.addVideoTrack("apch", videoInputTrack.getMeta().getDimensions(), APPLE_PRO_RES_422,
                    25000);
            videoOutputTrack.setTgtChunkDuration(HALF, SEC);
        }

        @Override
        protected void finishEncode() throws IOException {
            muxer.writeHeader();
        }

        @Override
        protected Picture8Bit createPixelBuffer(ColorSpace yuv444, ByteBuffer firstFrame) {
            Size dim = videoInputTrack.getMeta().getDimensions();
            return Picture8Bit.create(dim.getWidth(), dim.getHeight(), ColorSpace.YUV420);
        }

        @Override
        protected ColorSpace getEncoderColorspace() {
            return encoder.getSupportedColorSpaces()[0];
        }

        @Override
        protected Packet inputVideoPacket() throws IOException {
            return videoInputTrack.nextFrame();
        }

        @Override
        protected void outputVideoPacket(Packet packet) throws IOException {
            videoOutputTrack.setTimescale((int) packet.getTimescale());
            videoOutputTrack.addFrame(packet);
        }

        @Override
        protected Picture8Bit decodeVideo(ByteBuffer data, Picture8Bit target1) {
            return videoDecoder.decodeFrame8Bit(data, target1.getData());
        }

        @Override
        protected ByteBuffer encodeVideo(Picture8Bit frame, ByteBuffer _out) {
            return encoder.encodeFrame8Bit(frame, _out);
        }

        @Override
        protected boolean haveAudio() {
            return false;
        }

        @Override
        protected Packet inputAudioPacket() throws IOException {
            return null;
        }

        @Override
        protected void outputAudioPacket(Packet audioPkt) throws IOException {
        }

        @Override
        protected ByteBuffer decodeAudio(ByteBuffer audioPkt) throws IOException {
            return null;
        }

        @Override
        protected ByteBuffer encodeAudio(ByteBuffer wrap) {
            return null;
        }

        @Override
        protected boolean seek(int frame) throws IOException {
            return false;
        }

        @Override
        protected int getBufferSize(Picture8Bit frame) {
            return (3 * frame.getWidth() * frame.getHeight()) / 2;
        }
    }
}