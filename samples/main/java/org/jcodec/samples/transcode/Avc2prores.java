package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jcodec.codecs.aac.AACUtils;
import org.jcodec.codecs.aac.AACUtils.AACMetadata;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;

class Avc2prores extends TranscodeGenericProfile {
    // private static final String FLAG_RAW = "raw";
    // private static final String FLAG_MAX_FRAMES = "max-frames";
    private static final String FLAG_DUMPMV = "dumpMv";
    private static final String FLAG_DUMPMVJS = "dumpMvJs";
    private MP4Demuxer demux;
    private MP4Muxer muxer;
    private FramesMP4MuxerTrack videoOutputTrack;
    private DemuxerTrack videoInputTrack;
    private DemuxerTrack audioInputTrack;
    private MuxerTrack audioOutputTrack;
    private Decoder audioDecoder;
    private H264Decoder videoDecoder;
    private ProresEncoder videoEncoder;

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
        AbstractMP4DemuxerTrack videoTrack = demux.getVideoTrack();
        videoDecoder = H264Decoder.createH264DecoderFromCodecPrivate(videoTrack.getMeta().getCodecPrivate());
        DemuxerTrack selectedAudioTrack = selectAudioTrack(demux.getAudioTracks());
        if (selectedAudioTrack != null) {
            Logger.info("Using the audio track: " + selectedAudioTrack.getMeta().getIndex());
            this.audioInputTrack = selectedAudioTrack;
            SampleEntry sampleEntry = ((AbstractMP4DemuxerTrack) selectedAudioTrack).getSampleEntries()[0];
            audioDecoder = new Decoder(NIOUtils.toArray(AACUtils.getCodecPrivate(sampleEntry)));
            AACMetadata meta = AACUtils.getMetadata(sampleEntry);
            this.audioOutputTrack = muxer.addPCMAudioTrack(meta.getFormat());
        }
    }

    static final String APPLE_PRO_RES_422 = "Apple ProRes 422";

    @Override
    protected void initEncode(SeekableByteChannel sink) throws IOException {
        muxer = MP4Muxer.createMP4Muxer(sink, Brand.MOV);
        VideoSampleEntry videoSampleEntry = (VideoSampleEntry) ((MP4Demuxer) demux).getVideoTrack()
                .getSampleEntries()[0];
        PixelAspectExt pasp = Box.findFirst(videoSampleEntry, PixelAspectExt.class, "pasp");
        Size dim = videoInputTrack.getMeta().getDimensions();
        videoOutputTrack = muxer.addVideoTrack("apch", dim, APPLE_PRO_RES_422, 25000);
        if (pasp != null)
            videoOutputTrack.getEntries().get(0).add(pasp);
        videoEncoder = new ProresEncoder(ProresEncoder.Profile.HQ, false);
    }

    @Override
    protected void finishEncode() throws IOException {
        muxer.writeHeader();
    }

    @Override
    protected Picture8Bit createPixelBuffer(ColorSpace yuv444) {
        Size dim = videoInputTrack.getMeta().getDimensions();
        return Picture8Bit.create(dim.getWidth(), dim.getHeight(), ColorSpace.YUV420);
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
        videoOutputTrack.setTimescale((int) packet.getTimescale());
        videoOutputTrack.addFrame(packet);
    }

    @Override
    protected Picture8Bit decodeVideo(ByteBuffer data, Picture8Bit target1) {
        return ((H264Decoder) videoDecoder).decodeFrame8BitFromNals(H264Utils.splitFrame(data), target1.getData());
    }

    @Override
    protected ByteBuffer encodeVideo(Picture8Bit frame, ByteBuffer _out) {
        return videoEncoder.encodeFrame8Bit(frame, _out);
    }

    protected boolean haveAudio() {
        return audioInputTrack != null;
    }

    @Override
    protected Packet inputAudioPacket() throws IOException {
        return audioInputTrack.nextFrame();
    }

    @Override
    protected void outputAudioPacket(Packet audioPkt) throws IOException {
        audioOutputTrack.addFrame(audioPkt);
    }

    @Override
    protected ByteBuffer decodeAudio(ByteBuffer audioPkt) throws IOException {
        SampleBuffer sampleBuffer = new SampleBuffer();
        audioDecoder.decodeFrame(NIOUtils.toArray(audioPkt), sampleBuffer);
        if (sampleBuffer.isBigEndian())
            toLittleEndian(sampleBuffer);
        return ByteBuffer.wrap(sampleBuffer.getData());
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
        return (3 * frame.getWidth() * frame.getHeight()) / 2;
    }

    private void toLittleEndian(SampleBuffer sampleBuffer) {
        byte[] data = sampleBuffer.getData();
        for (int i = 0; i < data.length; i += 2) {
            byte tmp = data[i];
            data[i] = data[i + 1];
            data[i + 1] = tmp;
        }
    }

    @Override
    protected List<Filter> getFilters(Cmd cmd) {
        List<Filter> filters = new ArrayList<Filter>();
        if (cmd.getBooleanFlag(FLAG_DUMPMV))
            filters.add(new DumpMvFilter(false));
        else if (cmd.getBooleanFlag(FLAG_DUMPMVJS))
            filters.add(new DumpMvFilter(true));
        return filters;
    }

    private class DumpMvFilter implements Filter {
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
}