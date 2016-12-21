package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jcodec.codecs.aac.AACDecoder;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

class Avc2prores extends V2VTranscoder {
    private static final String FLAG_DUMPMV = "dumpMv";
    private static final String FLAG_DUMPMVJS = "dumpMvJs";

    private static class Avc2proresTranscoder extends GenericTranscoder {
        private MP4Demuxer demux;
        private MP4Muxer muxer;
        private MuxerTrack videoOutputTrack;
        private DemuxerTrack videoInputTrack;
        private DemuxerTrack audioInputTrack;
        private MuxerTrack audioOutputTrack;
        private AACDecoder audioDecoder;
        private H264Decoder videoDecoder;
        private ProresEncoder videoEncoder;

        public Avc2proresTranscoder(Cmd cmd, Profile profile) {
            super(cmd, profile);
        }

        private DemuxerTrack selectAudioTrack(List<? extends DemuxerTrack> tracks) {
            DemuxerTrack selectedAudioTrack = null;
            for (DemuxerTrack track : tracks) {
                if (track.getMeta().getCodec() == Codec.AAC) {
                    selectedAudioTrack = track;
                    break;
                }
            }
            return selectedAudioTrack;
        }

        @Override
        protected void initDecode(SeekableByteChannel source) throws IOException {
            demux = new MP4Demuxer(source);
            videoInputTrack = demux.getVideoTrack();
            DemuxerTrack videoTrack = demux.getVideoTrack();
            videoDecoder = H264Decoder.createH264DecoderFromCodecPrivate(videoTrack.getMeta().getCodecPrivate());
            DemuxerTrack selectedAudioTrack = selectAudioTrack(demux.getAudioTracks());
            if (selectedAudioTrack != null) {
                Logger.info("Using the audio track: " + selectedAudioTrack.getMeta().getIndex());
                this.audioInputTrack = selectedAudioTrack;
                DemuxerTrackMeta meta = audioInputTrack.getMeta();
                audioDecoder = new AACDecoder(meta.getCodecPrivate());
            }
        }

        static final String APPLE_PRO_RES_422 = "Apple ProRes 422";

        @Override
        protected void initEncode(SeekableByteChannel sink) throws IOException {
            muxer = MP4Muxer.createMP4Muxer(sink, Brand.MOV);
            videoOutputTrack = muxer.addVideoTrack(Codec.PRORES, videoInputTrack.getMeta().getVideoCodecMeta());
            videoEncoder = new ProresEncoder(ProresEncoder.Profile.HQ, false);
        }

        @Override
        protected void finishEncode() throws IOException {
            muxer.finish();
        }

        @Override
        protected Picture8Bit createPixelBuffer(ColorSpace yuv444, ByteBuffer firstFrame) {
            Size dim = videoInputTrack.getMeta().getVideoCodecMeta().getSize();
            return Picture8Bit.create((dim.getWidth() + 15) & ~0xf, (dim.getHeight() + 15) & ~0xf, ColorSpace.YUV420);
        }

        @Override
        protected ColorSpace getEncoderColorspace() {
            return videoEncoder.getSupportedColorSpaces()[0];
        }

        @Override
        protected Packet inputVideoPacket() throws IOException {
            return videoInputTrack.nextFrame();
        }

        @Override
        protected void outputVideoPacket(Packet packet) throws IOException {
            videoOutputTrack.addFrame(packet);
        }

        @Override
        protected Picture8Bit decodeVideo(ByteBuffer data, Picture8Bit target1) {
            return ((H264Decoder) videoDecoder).decodeFrame8BitFromNals(H264Utils.splitFrame(data), target1.getData());
        }

        @Override
        protected EncodedFrame encodeVideo(Picture8Bit frame, ByteBuffer _out) {
            return videoEncoder.encodeFrame8Bit(frame, _out);
        }

        protected boolean haveAudio() {
            return audioInputTrack != null;
        }

        @Override
        protected Packet inputAudioPacket() throws IOException {
            if (audioInputTrack == null)
                return null;
            return audioInputTrack.nextFrame();
        }

        @Override
        protected void outputAudioPacket(Packet audioPkt) throws IOException {
            audioOutputTrack.addFrame(audioPkt);
        }

        @Override
        protected ByteBuffer decodeAudio(ByteBuffer audioPkt) throws IOException {
            AudioBuffer decodeFrame = audioDecoder.decodeFrame(audioPkt, null);
            if (audioOutputTrack == null) {
                this.audioOutputTrack = muxer.addPCMAudioTrack(decodeFrame.getFormat());
            }
            return decodeFrame.getData();
        }

        @Override
        protected ByteBuffer encodeAudio(ByteBuffer wrap) {
            return wrap;
        }

        @Override
        protected boolean seek(int frame) throws IOException {
            Packet inFrame;

            if (videoInputTrack instanceof SeekableDemuxerTrack) {
                SeekableDemuxerTrack seekable = (SeekableDemuxerTrack) videoInputTrack;
                seekable.gotoFrame(frame);
                while ((inFrame = inputVideoPacket()) != null && !inFrame.isKeyFrame())
                    ;
                seekable.gotoFrame(inFrame.getFrameNo());
            } else {
                Logger.error("Can not seek in " + videoInputTrack + " container.");
                return false;
            }
            return true;
        }

        @Override
        protected int getBufferSize(Picture8Bit frame) {
            return videoEncoder.estimateBufferSize(frame);
        }

        @Override
        protected List<Filter> getFilters() {
            List<Filter> filters = new ArrayList<Filter>();
            if (cmd.getBooleanFlag(FLAG_DUMPMV))
                filters.add(new DumpMvFilter(false));
            else if (cmd.getBooleanFlag(FLAG_DUMPMVJS))
                filters.add(new DumpMvFilter(true));
            return filters;
        }
    }

    private static class DumpMvFilter implements Filter {
        private boolean js;

        public DumpMvFilter(boolean js) {
            this.js = js;
        }

        @Override
        public Picture8Bit filter(Picture8Bit picture, PixelStore pixelStore) {
            Frame dec = (Frame) picture;
            if (!js)
                dumpMvTxt(dec);
            else
                dumpMvJs(dec);
            return picture;
        }

        private void dumpMvTxt(Frame dec) {
            System.err.println("FRAME ================================================================");
            if (dec.getFrameType() == SliceType.I)
                return;
            int[][][][] mvs = dec.getMvs();
            for (int i = 0; i < 2; i++) {

                System.err.println((i == 0 ? "BCK" : "FWD")
                        + " ===========================================================================");
                for (int blkY = 0; blkY < mvs[i].length; ++blkY) {
                    StringBuilder line0 = new StringBuilder();
                    StringBuilder line1 = new StringBuilder();
                    StringBuilder line2 = new StringBuilder();
                    StringBuilder line3 = new StringBuilder();
                    line0.append("+");
                    line1.append("|");
                    line2.append("|");
                    line3.append("|");
                    for (int blkX = 0; blkX < mvs[i][0].length; ++blkX) {
                        line0.append("------+");
                        line1.append(String.format("%6d|", mvs[i][blkY][blkX][0]));
                        line2.append(String.format("%6d|", mvs[i][blkY][blkX][1]));
                        line3.append(String.format("    %2d|", mvs[i][blkY][blkX][2]));
                    }
                    System.err.println(line0.toString());
                    System.err.println(line1.toString());
                    System.err.println(line2.toString());
                    System.err.println(line3.toString());
                }
                if (dec.getFrameType() != SliceType.B)
                    break;
            }
        }

        private void dumpMvJs(Frame dec) {
            System.err.println("{");
            if (dec.getFrameType() == SliceType.I)
                return;
            int[][][][] mvs = dec.getMvs();
            for (int i = 0; i < 2; i++) {

                System.err.println((i == 0 ? "backRef" : "forwardRef") + ": [");
                for (int blkY = 0; blkY < mvs[i].length; ++blkY) {
                    for (int blkX = 0; blkX < mvs[i][0].length; ++blkX) {
                        System.err.println("{x: " + blkX + ", y: " + blkY + ", mx: " + mvs[i][blkY][blkX][0] + ", my: "
                                + mvs[i][blkY][blkX][1] + ", ridx:" + mvs[i][blkY][blkX][2] + "},");
                    }
                }
                System.err.println("],");
                if (dec.getFrameType() != SliceType.B)
                    break;
            }
            System.err.println("}");
        }
    }

    @Override
    protected void additionalFlags(Map<String, String> flags) {
        // flags.put(FLAG_RAW, "Input AnnexB stream (raw h.264 elementary
        // stream)");
        flags.put(FLAG_DUMPMV, "Dump motion vectors from frames");
        flags.put(FLAG_DUMPMVJS, "Dump motion vectors from frames in JSon format");
        // flags.put(FLAG_MAX_FRAMES, "Maximum number of frames to ouput");
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
        return TranscodeMain.codecs(Codec.H264);
    }

    @Override
    public Set<Codec> outputVideoCodec() {
        return TranscodeMain.codecs(Codec.PRORES);
    }

    @Override
    public Set<Codec> inputAudioCodec() {
        return TranscodeMain.codecs(Codec.AAC);
    }

    @Override
    public Set<Codec> outputAudioCodec() {
        return TranscodeMain.codecs(Codec.PCM);
    }

    @Override
    protected GenericTranscoder getTranscoder(Cmd cmd, Profile profile) {
        return new Avc2proresTranscoder(cmd, profile);
    }
}