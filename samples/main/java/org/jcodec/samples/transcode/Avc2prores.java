package org.jcodec.samples.transcode;

import static java.lang.Math.min;
import static org.jcodec.common.io.NIOUtils.readableFileChannel;
import static org.jcodec.common.io.NIOUtils.writableFileChannel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;

import org.jcodec.codecs.aac.AACUtils;
import org.jcodec.codecs.aac.AACUtils.AACMetadata;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.MappedH264ES;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mp4.muxer.PCMMP4MuxerTrack;
import org.jcodec.scale.Transform8Bit;
import org.jcodec.scale.Yuv420pToYuv422p8Bit;

class Avc2prores implements Profile {
    private static final String FLAG_RAW = "raw";
    private static final String FLAG_MAX_FRAMES = "max-frames";
    private static final String FLAG_DUMPMV = "dumpMv";
    private static final String FLAG_DUMPMVJS = "dumpMvJs";

    @Override
    public void transcode(Cmd cmd) throws IOException {
        SeekableByteChannel sink = null;
        SeekableByteChannel source = null;
        boolean raw = cmd.getBooleanFlagD(FLAG_RAW, false);
        try {
            sink = writableFileChannel(cmd.getArg(1));

            int totalFrames = Integer.MAX_VALUE;
            PixelAspectExt pasp = null;
            DemuxerTrack videoTrack;
            DemuxerTrack audioTrack = null;
            AACMetadata meta = null;
            Decoder aacDecoder = null;
            int width = 0, height = 0;
            H264Decoder decoder = null;
            if (!raw) {
                source = readableFileChannel(cmd.getArg(0));
                MP4Demuxer demux = new MP4Demuxer(source);
                AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();
                VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];

                totalFrames = (int) inTrack.getFrameCount();
                pasp = Box.findFirst(inTrack.getSampleEntries()[0], PixelAspectExt.class, "pasp");

                decoder = H264Decoder.createH264DecoderFromCodecPrivate(inTrack.getMeta().getCodecPrivate());

                videoTrack = inTrack;

                width = (ine.getWidth() + 15) & ~0xf;
                height = (ine.getHeight() + 15) & ~0xf;

                List<AbstractMP4DemuxerTrack> tracks = demux.getAudioTracks();
                AbstractMP4DemuxerTrack selectedTrack = null;
                for (AbstractMP4DemuxerTrack track : tracks) {
                    if (track.getCodec() == Codec.AAC) {
                        selectedTrack = track;
                        break;
                    }
                }
                if (selectedTrack != null) {
                    Logger.info("Using the AAC track: " + selectedTrack.getNo());
                    audioTrack = selectedTrack;
                }
                SampleEntry sampleEntry = selectedTrack.getSampleEntries()[0];
                meta = AACUtils.getMetadata(sampleEntry);
                aacDecoder = new Decoder(NIOUtils.toArray(AACUtils.getCodecPrivate(sampleEntry)));
            } else {
                videoTrack = new MappedH264ES(NIOUtils.fetchFromFile(new File(cmd.getArg(0))));
            }
            MP4Muxer muxer = MP4Muxer.createMP4Muxer(sink, Brand.MOV);
            PCMMP4MuxerTrack pcmOutTrack = null;
            if (audioTrack != null) {
                pcmOutTrack = muxer.addPCMAudioTrack(meta.getFormat());
            }

            ProresEncoder encoder = new ProresEncoder(ProresEncoder.Profile.HQ, false);

            Transform8Bit transform = new Yuv420pToYuv422p8Bit();
            boolean dumpMv = cmd.getBooleanFlagD(FLAG_DUMPMV, false);
            boolean dumpMvJs = cmd.getBooleanFlagD(FLAG_DUMPMVJS, false);

            int timescale = 24000;
            int frameDuration = 1000;
            FramesMP4MuxerTrack outTrack = null;

            int gopLen = 0, i;
            Frame[] gop = new Frame[1000];
            Packet inFrame;

            int sf = 90000;
            if (!raw) {
                AbstractMP4DemuxerTrack dt = (AbstractMP4DemuxerTrack) videoTrack;
                dt.gotoFrame(sf);
                while ((inFrame = videoTrack.nextFrame()) != null && !inFrame.isKeyFrame())
                    ;
                dt.gotoFrame(inFrame.getFrameNo());
            }
            long totalH264 = 0, totalProRes = 0;
            int maxFrames = cmd.getIntegerFlagD(FLAG_MAX_FRAMES, Integer.MAX_VALUE);
            for (i = 0; (gopLen + i) < maxFrames && (inFrame = videoTrack.nextFrame()) != null;) {

                if (audioTrack != null) {
                    Packet audioPkt;
                    do {
                        audioPkt = audioTrack.nextFrame();
                        if (audioPkt == null)
                            break;
                        SampleBuffer sampleBuffer = new SampleBuffer();
                        aacDecoder.decodeFrame(NIOUtils.toArray(audioPkt.getData()), sampleBuffer);
                        if (sampleBuffer.isBigEndian())
                            toLittleEndian(sampleBuffer);
                        pcmOutTrack.addSamples(ByteBuffer.wrap(sampleBuffer.getData()));
                    } while (audioPkt.getPtsD() < inFrame.getPtsD() + 0.2);
                }

                ByteBuffer data = inFrame.getData();
                Picture8Bit target1;
                Frame dec;
                if (!raw) {
                    target1 = Picture8Bit.create(width, height, ColorSpace.YUV420);
                    long start = System.nanoTime();
                    dec = decoder.decodeFrame8BitFromNals(H264Utils.splitFrame(data), target1.getData());
                    totalH264 += (System.nanoTime() - start);
                    if (dumpMv)
                        dumpMv(i, dec);
                    if (dumpMvJs)
                        dumpMvJs(i, dec);
                } else {
                    SeqParameterSet sps = ((MappedH264ES) videoTrack).getSps()[0];
                    width = (sps.pic_width_in_mbs_minus1 + 1) << 4;
                    height = H264Utils.getPicHeightInMbs(sps) << 4;
                    target1 = Picture8Bit.create(width, height, ColorSpace.YUV420);
                    long start = System.nanoTime();
                    dec = decoder.decodeFrame8Bit(data, target1.getData());
                    totalH264 += (System.nanoTime() - start);
                    if (dumpMv)
                        dumpMv(i, dec);
                    if (dumpMvJs)
                        dumpMvJs(i, dec);
                }
                if (outTrack == null) {
                    outTrack = muxer.addVideoTrack("apch", new Size(dec.getCroppedWidth(), dec.getCroppedHeight()),
                            TranscodeMain.APPLE_PRO_RES_422, timescale);
                    if (pasp != null)
                        outTrack.getEntries().get(0).add(pasp);
                }
                if (dec.getPOC() == 0 && gopLen > 0) {
                    totalProRes += outGOP(encoder, transform, timescale, frameDuration, outTrack, gopLen, gop,
                            min(totalFrames, maxFrames), i, width, height);
                    i += gopLen;
                    gopLen = 0;
                }
                gop[gopLen++] = dec;
            }
            if (gopLen > 0) {
                totalProRes += outGOP(encoder, transform, timescale, frameDuration, outTrack, gopLen, gop,
                        min(totalFrames, maxFrames), i, width, height);
            }
            muxer.writeHeader();
            System.out.println(((1000000000L * (i + gopLen)) / totalH264) + "fps (h.264 decoding).");
            System.out.println(((1000000000L * (i + gopLen)) / totalProRes) + "fps (ProRes encoding).");
        } finally {
            if (sink != null)
                sink.close();
            if (source != null)
                source.close();
        }
    }

    private void toLittleEndian(SampleBuffer sampleBuffer) {
        byte[] data = sampleBuffer.getData();
        for (int i = 0; i < data.length; i += 2) {
            byte tmp = data[i];
            data[i] = data[i + 1];
            data[i + 1] = tmp;
        }
    }

    private void dumpMv(int frameNo, Frame dec) {
        System.err.println("FRAME " + String.format("%08d", frameNo)
                + " ================================================================");
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

    private void dumpMvJs(int frameNo, Frame dec) {
        System.err.println("{frameNo: " + frameNo + ",");
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

    private static long outGOP(ProresEncoder encoder, Transform8Bit transform, int timescale, int frameDuration,
            FramesMP4MuxerTrack outTrack, int gopLen, Frame[] gop, int totalFrames, int i, int codedWidth,
            int codedHeight) throws IOException {

        long totalTime = 0;
        ByteBuffer _out = ByteBuffer.allocate(codedWidth * codedHeight * 6);
        Picture8Bit target2 = Picture8Bit.create(codedWidth, codedHeight, ColorSpace.YUV422);
        Arrays.sort(gop, 0, gopLen, Frame.POCAsc);
        for (int g = 0; g < gopLen; g++) {
            Frame frame = gop[g];
            transform.transform(frame, target2);
            target2.setCrop(frame.getCrop());
            _out.clear();
            long start = System.nanoTime();
            encoder.encodeFrame8Bit(target2, _out);
            totalTime += System.nanoTime() - start;
            // TODO: Error if chunk has more then one frame
            outTrack.addFrame(MP4Packet.createMP4Packet(_out, i * frameDuration, timescale, frameDuration, i, true,
                    null, 0, i * frameDuration, 0));

            if (i % 100 == 0)
                System.out.println((i * 100 / totalFrames) + "%");
            i++;
        }
        return totalTime;
    }

    @Override
    public void printHelp(PrintStream err) {
        MainUtils.printHelpVarArgs(new HashMap<String, String>() {
            {
                put(FLAG_RAW, "Input AnnexB stream (raw h.264 elementary stream)");
                put(FLAG_DUMPMV, "Dump motion vectors from frames");
                put(FLAG_DUMPMVJS, "Dump motion vectors from frames in JSon format");
                put(FLAG_MAX_FRAMES, "Maximum number of frames to ouput");
            }
        }, "in file", "pattern");
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MOV, Format.H264);
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