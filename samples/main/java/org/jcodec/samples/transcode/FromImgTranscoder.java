package org.jcodec.samples.transcode;

import static org.jcodec.common.tools.MainUtils.tildeExpand;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.Muxer;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.imgseq.ImageSequenceDemuxer;
import org.jcodec.scale.AWTUtil;

/**
 * Converts image sequence to any format (impemented in derived classes).
 * 
 * @author Stanislav Vitvitskiy
 *
 */
abstract class FromImgTranscoder extends V2VTranscoder {
    private static final String FLAG_MAX_FRAMES = "maxFrames";

    // Protected interface
    protected abstract VideoEncoder getEncoder();

    protected abstract Muxer createMuxer(SeekableByteChannel sink) throws IOException;

    protected abstract MuxerTrack getMuxerTrack(Muxer muxer, Picture8Bit yuv);

    protected abstract void finalizeMuxer() throws IOException;

    protected abstract EncodedFrame encodeFrame(VideoEncoder encoder, Picture8Bit yuv, ByteBuffer buf);

    protected void populateAdditionalFlags(Map<String, String> flags) {
    }

    private class FromImgTranscoder2 extends GenericTranscoder {
        private ImageSequenceDemuxer demuxer;
        private MuxerTrack track;
        private VideoEncoder encoder;
        private Muxer muxer;

        public FromImgTranscoder2(Cmd cmd, Profile profile) {
            super(cmd, profile);
        }

        @Override
        protected void initDecode(SeekableByteChannel source) throws IOException {
            demuxer = new ImageSequenceDemuxer(tildeExpand(cmd.getArg(0)).getAbsolutePath(),
                    cmd.getIntegerFlagD(FLAG_MAX_FRAMES, Integer.MAX_VALUE));
        }

        @Override
        protected void initEncode(SeekableByteChannel sink) throws IOException {
            muxer = createMuxer(sink);
            encoder = getEncoder();
        }

        @Override
        protected void finishEncode() throws IOException {
            finalizeMuxer();
        }

        @Override
        protected Packet inputVideoPacket() throws IOException {
            return demuxer.nextFrame();
        }

        @Override
        protected Picture8Bit decodeVideo(ByteBuffer data, Picture8Bit target1) {
            try {
                BufferedImage rgb = ImageIO.read(new ByteArrayInputStream(NIOUtils.toArray(data)));
                return AWTUtil.fromBufferedImageRGB8Bit(rgb);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected EncodedFrame encodeVideo(Picture8Bit frame, ByteBuffer _out) {
            if (track == null) {
                track = getMuxerTrack(muxer, frame);
            }
            return encodeFrame(encoder, frame, _out);
        }

        @Override
        protected void outputVideoPacket(Packet packet) throws IOException {
            track.addFrame(packet);
        }

        @Override
        protected Picture8Bit createPixelBuffer(ColorSpace yuv444, ByteBuffer firstFrame) {
            try {
                BufferedImage rgb = ImageIO.read(new ByteArrayInputStream(NIOUtils.toArray(firstFrame)));
                return Picture8Bit.create(rgb.getWidth(), rgb.getHeight(), yuv444);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected ColorSpace getEncoderColorspace() {
            return encoder.getSupportedColorSpaces()[0];
        }

        @Override
        protected int getBufferSize(Picture8Bit frame) {
            return encoder.estimateBufferSize(frame);
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
    }

    @Override
    protected GenericTranscoder getTranscoder(Cmd cmd, Profile profile) {
        return new FromImgTranscoder2(cmd, profile);
    }

    @Override
    protected void additionalFlags(Map<String, String> flags) {
        flags.put(FLAG_MAX_FRAMES, "Number of frames to transcode");
        populateAdditionalFlags(flags);
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.IMG);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.PNG);
    }

    @Override
    public Set<Codec> inputAudioCodec() {
        return null;
    }

    @Override
    public Set<Codec> outputAudioCodec() {
        return null;
    }
}