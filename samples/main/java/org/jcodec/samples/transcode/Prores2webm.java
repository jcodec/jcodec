package org.jcodec.samples.transcode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.prores.ProresToThumb2x2;
import org.jcodec.codecs.vpx.IVFMuxer;
import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.Muxer;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mkv.muxer.MKVMuxer;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

class Prores2webm extends V2VTranscoder {
    public static final String FLAG_THUMBNAIL = "thumbnail";

    private static class Prores2webmTranscoder extends GenericTranscoder {

        private DemuxerTrack inputVideoTrack;
        private ProresDecoder videoDecoder;
        private VP8Encoder videoEncoder;
        private MuxerTrack outputVideoTrack;
        private Muxer muxer;
        private boolean useMkv;

        public Prores2webmTranscoder(Cmd cmd, Profile profile) {
            super(cmd, profile);
            useMkv = profile.getOutputFormat() == Format.MKV;
        }

        @Override
        protected void initDecode(SeekableByteChannel source) throws IOException {
            MP4Demuxer demux = new MP4Demuxer(source);
            inputVideoTrack = demux.getVideoTrack();
            if (cmd.getBooleanFlagD(FLAG_THUMBNAIL, false)) {
                videoDecoder = new ProresToThumb2x2();
            } else {
                videoDecoder = new ProresDecoder();
            }
        }

        @Override

        protected void initEncode(SeekableByteChannel sink) throws IOException {
            if (useMkv) {
                muxer = new MKVMuxer(sink);
            } else {
                muxer = new IVFMuxer(sink);
            }
            outputVideoTrack = muxer.addVideoTrack(Codec.VP8, inputVideoTrack.getMeta().getVideoCodecMeta());
            videoEncoder = VP8Encoder.createVP8Encoder(10);
        }

        @Override
        protected void finishEncode() throws IOException {
            if (useMkv) {
                muxer.finish();
            }
        }

        @Override
        protected Picture8Bit createPixelBuffer(ColorSpace yuv444, ByteBuffer firstFrame) {
            Size dim = inputVideoTrack.getMeta().getVideoCodecMeta().getSize();
            return Picture8Bit.create(dim.getWidth(), dim.getHeight(), yuv444);
        }

        @Override
        protected ColorSpace getEncoderColorspace() {
            return videoEncoder.getSupportedColorSpaces()[0];
        }

        @Override
        protected Packet inputVideoPacket() throws IOException {
            return inputVideoTrack.nextFrame();
        }

        @Override
        protected void outputVideoPacket(Packet packet) throws IOException {
            outputVideoTrack.addFrame(packet);
        }

        @Override
        protected Picture8Bit decodeVideo(ByteBuffer data, Picture8Bit target1) {
            return videoDecoder.decodeFrame8Bit(data, target1.getData());
        }

        @Override
        protected EncodedFrame encodeVideo(Picture8Bit frame, ByteBuffer _out) {
            return videoEncoder.encodeFrame8Bit(frame, _out);
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
            return frame.getWidth() * frame.getHeight() / 2;
        }

    }

    @Override
    public void printHelp(PrintStream err) {
        MainUtils.printHelpVarArgs(new HashMap<String, String>() {
            {
                put(FLAG_THUMBNAIL, "Use ProRes thumbnail decoder");
            }
        }, "in file", "out file");
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MOV);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.MKV, Format.IVF);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.PRORES);
    }

    @Override
    public Set<Codec> outputVideoCodec() {
        return TranscodeMain.codecs(Codec.VP8);
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
    protected GenericTranscoder getTranscoder(Cmd cmd, Profile profile) {
        return new Prores2webmTranscoder(cmd, profile);
    }
}