package org.jcodec.samples.transcode;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;
import static org.jcodec.common.tools.MainUtils.tildeExpand;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.encode.DumbRateControl;
import org.jcodec.codecs.mjpeg.JpegDecoder;
import org.jcodec.codecs.mjpeg.JpegToThumb2x2;
import org.jcodec.codecs.mjpeg.JpegToThumb4x4;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
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
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform8Bit;

class MP4Jpeg2avc implements Profile {
    public static final String FLAG_DOWNSCALE = "downscale";

    @Override
    public void transcode(Cmd cmd) throws IOException {
        SeekableByteChannel sink = null;
        SeekableByteChannel source = null;
        try {
            sink = writableChannel(tildeExpand(cmd.getArg(1)));
            source = readableChannel(tildeExpand(cmd.getArg(0)));

            MP4Demuxer demux = new MP4Demuxer(source);
            MP4Muxer muxer = MP4Muxer.createMP4Muxer(sink, Brand.MOV);

            Transform8Bit transform = null;

            H264Encoder encoder = new H264Encoder(new DumbRateControl());

            AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();
            FramesMP4MuxerTrack outTrack = muxer.addTrack(MP4TrackType.VIDEO, (int) inTrack.getTimescale());

            VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];
            Picture8Bit target1 = Picture8Bit.create(1920, 1088, ColorSpace.YUV444);
            Picture8Bit target2 = null;
            ByteBuffer _out = ByteBuffer.allocate(ine.getWidth() * ine.getHeight() * 6);

            Integer downscale = cmd.getIntegerFlag(FLAG_DOWNSCALE);
            JpegDecoder decoder;
            if (downscale == null || downscale == 1) {
                decoder = new JpegDecoder();
            } else if (downscale == 2) {
                decoder = new JpegToThumb4x4();
            } else if (downscale == 4) {
                decoder = new JpegToThumb2x2();
            } else {
                throw new IllegalArgumentException("Downscale factor of " + downscale + " is not supported ([2,4]).");
            }

            Set<ByteBuffer> spsList = new HashSet<ByteBuffer>();
            Set<ByteBuffer> ppsList = new HashSet<ByteBuffer>();
            Packet inFrame;
            int totalFrames = (int) inTrack.getFrameCount();
            long start = System.currentTimeMillis();
            for (int i = 0; (inFrame = inTrack.nextFrame()) != null && i < 100; i++) {
                Picture8Bit dec = decoder.decodeFrame8Bit(inFrame.getData(), target1.getData());
                if (transform == null) {
                    transform = ColorUtil.getTransform8Bit(dec.getColor(), encoder.getSupportedColorSpaces()[0]);
                }
                if (target2 == null) {
                    target2 = Picture8Bit.create(dec.getWidth(), dec.getHeight(), encoder.getSupportedColorSpaces()[0]);
                }
                transform.transform(dec, target2);
                _out.clear();
                ByteBuffer result = encoder.encodeFrame8Bit(target2, _out);
                H264Utils.wipePSinplace(result, spsList, ppsList);
                H264Utils.encodeMOVPacket(result);
                outTrack.addFrame(MP4Packet.createMP4PacketWithData((MP4Packet) inFrame, result));

                if (i % 100 == 0) {
                    long elapse = System.currentTimeMillis() - start;
                    System.out.println((i * 100 / totalFrames) + "%, " + (i * 1000 / elapse) + "fps");
                }
            }
            outTrack.addSampleEntry(H264Utils.createMOVSampleEntryFromSpsPpsList(new ArrayList<ByteBuffer>(spsList),
                    new ArrayList<ByteBuffer>(ppsList), 4));

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
                put(FLAG_DOWNSCALE, "Downscale factor: [2, 4].");
            }
        }, "in file", "out file");
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MJPEG);
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
}