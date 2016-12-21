package org.jcodec.samples.transcode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.encode.DumbRateControl;
import org.jcodec.codecs.mjpeg.JpegDecoder;
import org.jcodec.codecs.mjpeg.JpegToThumb2x2;
import org.jcodec.codecs.mjpeg.JpegToThumb4x4;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

class MP4Jpeg2avc extends V2VTranscoder {
    public static final String FLAG_DOWNSCALE = "downscale";

    public static class MP4Jpeg2avcTranscoder extends GenericTranscoder {
        private DemuxerTrack inputVideoTrack;
        private MP4Muxer muxer;
        private MuxerTrack outputVideoTrack;
        private H264Encoder videoEncoder;
        private JpegDecoder videoDecoder;

        public MP4Jpeg2avcTranscoder(Cmd cmd, Profile profile) {
            super(cmd, profile);
        }

        @Override
        protected void initDecode(SeekableByteChannel source) throws IOException {
            MP4Demuxer demux = new MP4Demuxer(source);

            inputVideoTrack = demux.getVideoTrack();

            Integer downscale = cmd.getIntegerFlag(FLAG_DOWNSCALE);
            if (downscale == null || downscale == 1) {
                videoDecoder = new JpegDecoder();
            } else if (downscale == 2) {
                videoDecoder = new JpegToThumb4x4();
            } else if (downscale == 4) {
                videoDecoder = new JpegToThumb2x2();
            } else {
                throw new IllegalArgumentException("Downscale factor of " + downscale + " is not supported ([2,4]).");
            }
        }

        @Override
        protected void initEncode(SeekableByteChannel sink) throws IOException {
            muxer = MP4Muxer.createMP4Muxer(sink, Brand.MOV);

            videoEncoder = new H264Encoder(new DumbRateControl());

            outputVideoTrack = muxer.addVideoTrack(Codec.H264, inputVideoTrack.getMeta().getVideoCodecMeta());
        }

        @Override
        protected void finishEncode() throws IOException {
            muxer.finish();
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
            return videoEncoder.estimateBufferSize(frame);
        }
    }

    @Override
    public void printHelp(PrintStream err) {
        MainUtils.printHelpVarArgs(new HashMap<String, String>() {
            {
                put(FLAG_DOWNSCALE, "Downscale factor: [2, 4].");
            }
        }, "in file", "out file");
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MOV);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.MOV);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.JPEG);
    }

    @Override
    public Set<Codec> outputVideoCodec() {
        return TranscodeMain.codecs(Codec.H264);
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
        return new MP4Jpeg2avcTranscoder(cmd, profile);
    }
}