package org.jcodec.samples.transcode;

import static org.jcodec.common.io.NIOUtils.readableFileChannel;
import static org.jcodec.common.io.NIOUtils.writableFileChannel;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.encode.DumbRateControl;
import org.jcodec.codecs.h264.encode.H264FixedRateControl;
import org.jcodec.codecs.h264.encode.RateControl;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.prores.ProresToThumb2x2;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.scale.Transform8Bit;
import org.jcodec.scale.Yuv422pToYuv420p8Bit;

class Prores2avc implements Profile {
    private static final int DEFAULT_FIXED_BITS_PER_MB = 1024;
    private static final String FLAG_THUMBNAIL = "thumbnail";
    private static final String FLAG_RC = "rc";
    private static final String FLAG_BITS_PER_MB = "bitsPerMb";

    @Override
    public void transcode(Cmd cmd) throws IOException {
        SeekableByteChannel sink = null;
        SeekableByteChannel source = null;
        try {
            sink = writableFileChannel(cmd.getArg(1));
            source = readableFileChannel(cmd.getArg(0));

            MP4Demuxer demux = new MP4Demuxer(source);
            MP4Muxer muxer = MP4Muxer.createMP4Muxer(sink, Brand.MP4);

            Transform8Bit transform = new Yuv422pToYuv420p8Bit();

            String rcName = cmd.getStringFlagD(FLAG_RC, "dumb");
            RateControl rc;
            if ("dumb".equals(rcName)) {
                rc = new DumbRateControl();
            } else if ("fixed".equals(rcName)) {
                rc = new H264FixedRateControl(cmd.getIntegerFlagD(FLAG_BITS_PER_MB, DEFAULT_FIXED_BITS_PER_MB));
            } else {
                System.err.println("Unsupported rate control mode: " + rcName);
                return;
            }

            H264Encoder encoder = new H264Encoder(rc);
            encoder.setKeyInterval(25);

            AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();
            FramesMP4MuxerTrack outTrack = muxer.addTrack(MP4TrackType.VIDEO, (int) inTrack.getTimescale());

            VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];
            Picture8Bit target1 = Picture8Bit.create(1920, 1088, ColorSpace.YUV422);
            Picture8Bit target2 = null;
            ByteBuffer _out = ByteBuffer.allocate(ine.getWidth() * ine.getHeight() * 6);

            List<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
            List<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();
            ProresDecoder decoder;
            if (cmd.getBooleanFlagD(FLAG_THUMBNAIL, false)) {
                decoder = new ProresToThumb2x2();
            } else {
                decoder = new ProresDecoder();
            }
            Packet inFrame;
            int totalFrames = (int) inTrack.getFrameCount();
            long start = System.currentTimeMillis();
            for (int i = 0; (inFrame = inTrack.nextFrame()) != null; i++) {
                Picture8Bit dec = decoder.decodeFrame8Bit(inFrame.getData(), target1.getData());
                if (target2 == null) {
                    target2 = Picture8Bit.createCropped(dec.getWidth(), dec.getHeight(),
                            encoder.getSupportedColorSpaces()[0], dec.getCrop());
                }
                transform.transform(dec, target2);
                _out.clear();
                ByteBuffer result = encoder.encodeFrame8Bit(target2, _out);
                if (rc instanceof H264FixedRateControl) {
                    int mbWidth = (dec.getWidth() + 15) >> 4;
                    int mbHeight = (dec.getHeight() + 15) >> 4;
                    result.limit(((H264FixedRateControl) rc).calcFrameSize(mbWidth * mbHeight));
                }
                H264Utils.wipePSinplace(result, spsList, ppsList);
                NALUnit nu = NALUnit.read(NIOUtils.from(result.duplicate(), 4));
                H264Utils.encodeMOVPacket(result);
                MP4Packet pkt = MP4Packet.createMP4PacketWithData((MP4Packet) inFrame, result);
                pkt.setKeyFrame(nu.type == NALUnitType.IDR_SLICE);
                outTrack.addFrame(pkt);
                if (i % 100 == 0) {
                    long elapse = System.currentTimeMillis() - start;
                    System.out.println((i * 100 / totalFrames) + "%, " + (i * 1000 / elapse) + "fps");
                }
            }
            outTrack.addSampleEntry(
                    H264Utils.createMOVSampleEntryFromSpsPpsList(spsList.subList(0, 1), ppsList.subList(0, 1), 4));

            muxer.writeHeader();
        } finally {
            if (sink != null)
                sink.close();
            if (source != null)
                source.close();
        }
    }

    @Override
    public void printHelp(PrintStream err) {
        MainUtils.printHelpVarArgs(new HashMap<String, String>() {
            {
                put(FLAG_RC, "Rate control algorythm");
                put(FLAG_THUMBNAIL, "Use ProRes thumbnail decoder");
            }
        }, "in file", "pattern");
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
        return TranscodeMain.codecs(Codec.PRORES);
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
}