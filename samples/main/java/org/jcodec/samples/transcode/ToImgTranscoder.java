package org.jcodec.samples.transcode;

import static java.lang.String.format;
import static org.jcodec.common.tools.MainUtils.tildeExpand;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.RgbToBgr8Bit;

public abstract class ToImgTranscoder extends V2VTranscoder {

    @Override
    protected GenericTranscoder getTranscoder(Cmd cmd, Profile profile) {
        return new ToImgTranscoder2(cmd, profile);
    }

    protected void populateAdditionalFlags(Map<String, String> flags) {
    }

    protected boolean validateArguments(Cmd cmd) {
        return true;
    }

    protected abstract VideoDecoder getDecoder(Cmd cmd, DemuxerTrack inTrack, ByteBuffer firstFrame);

    protected abstract DemuxerTrack getDemuxer(Cmd cmd, SeekableByteChannel source) throws IOException;

    protected abstract Picture8Bit decodeFrame(VideoDecoder decoder, Picture8Bit target1, ByteBuffer data);

    protected abstract Packet nextPacket(DemuxerTrack inTrack) throws IOException;

    protected DemuxerTrackMeta getTrackMeta(DemuxerTrack inTrack, ByteBuffer firstFrame) {
        return inTrack.getMeta();
    }

    protected class ToImgTranscoder2 extends GenericTranscoder {

        private BufferedImage bi;
        private RgbToBgr8Bit rgbToBgr;
        private DemuxerTrack demuxer;
        private VideoDecoder decoder;


        @Override
        protected ByteBuffer encodeVideo(Picture8Bit dec, ByteBuffer _out) {
            if (bi == null) {
                bi = new BufferedImage(dec.getWidth(), dec.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            }
            rgbToBgr.transform(dec, dec);

            AWTUtil.toBufferedImage8Bit(dec, bi);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ImageIO.write(bi, profile.getOutputVideoCodec() == Codec.PNG ? "png" : "jpeg", baos);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ByteBuffer.wrap(baos.toByteArray());
        }
        
        @Override
        protected ColorSpace getEncoderColorspace() {
            return ColorSpace.RGB;
        }
        
        public ToImgTranscoder2(Cmd cmd, Profile profile) {
            super(cmd, profile);
        }

        @Override
        protected void initDecode(SeekableByteChannel source) throws IOException {
            demuxer = getDemuxer(cmd, source);
        }
        
        @Override
        protected Picture8Bit createPixelBuffer(ColorSpace yuv444, ByteBuffer firstFrame) {
            DemuxerTrackMeta trackMeta = getTrackMeta(demuxer, firstFrame);
            Size dim = trackMeta.getDimensions();
            return Picture8Bit.create(dim.getWidth(), dim.getHeight(), yuv444);
        }

        @Override
        protected Picture8Bit decodeVideo(ByteBuffer data, Picture8Bit target1) {
            if (decoder == null)
                decoder = getDecoder(cmd, demuxer, data);
            return decodeFrame(decoder, target1, data);
        }

        @Override
        protected void initEncode(SeekableByteChannel sink) throws IOException {
            rgbToBgr = new RgbToBgr8Bit();
        }

        @Override
        protected void finishEncode() throws IOException {
        }

        @Override
        protected Packet inputVideoPacket() throws IOException {
            return nextPacket(demuxer);
        }

        @Override
        protected void outputVideoPacket(Packet packet) throws IOException {
            NIOUtils.writeTo(packet.getData(), tildeExpand(format(cmd.getArg(1), packet.getFrameNo())));
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
    protected void additionalFlags(Map<String, String> flags) {
        populateAdditionalFlags(flags);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.IMG);
    }

    @Override
    public Set<Codec> outputVideoCodec() {
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