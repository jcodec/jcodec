package org.jcodec.samples.transcode;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;
import static org.jcodec.common.tools.MainUtils.tildeExpand;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.prores.ProresToThumb2x2;
import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mkv.muxer.MKVMuxer;
import org.jcodec.containers.mkv.muxer.MKVMuxerTrack;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.scale.Transform8Bit;
import org.jcodec.scale.Yuv422pToYuv420p8Bit;

class Prores2webm implements Profile {
    public static final String FLAG_THUMBNAIL = "thumbnail";

    @Override
    public void transcode(Cmd cmd) throws IOException {
        SeekableByteChannel sink = null;
        SeekableByteChannel source = null;
        try {
            sink = writableChannel(tildeExpand(cmd.getArg(1)));
            source = readableChannel(tildeExpand(cmd.getArg(0)));

            MP4Demuxer demux = new MP4Demuxer(source);

            Transform8Bit transform = new Yuv422pToYuv420p8Bit();

            VP8Encoder encoder = VP8Encoder.createVP8Encoder(10); // qp

            MKVMuxer muxer = new MKVMuxer();
            MKVMuxerTrack videoTrack = null;

            AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();

            VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];
            Picture8Bit target1 = Picture8Bit.create(1920, 1088, ColorSpace.YUV422);
            Picture8Bit target2 = null;
            ByteBuffer _out = ByteBuffer.allocate(ine.getWidth() * ine.getHeight() * 6);

            int fps = (int) (inTrack.getFrameCount() / inTrack.getDuration().scalar());

            ProresDecoder decoder;
            if (cmd.getBooleanFlagD(FLAG_THUMBNAIL, false)) {
                decoder = new ProresToThumb2x2();
            } else {
                decoder = new ProresDecoder();
            }
            MP4Packet inFrame;
            int totalFrames = (int) inTrack.getFrameCount();
            long start = System.currentTimeMillis();
            for (int i = 0; (inFrame = (MP4Packet) inTrack.nextFrame()) != null; i++) {
                Picture8Bit dec = decoder.decodeFrame8Bit(inFrame.getData(), target1.getData());
                if (target2 == null) {
                    target2 = Picture8Bit.create(dec.getWidth(), dec.getHeight(), ColorSpace.YUV420);
                }
                transform.transform(dec, target2);
                _out.clear();

                ByteBuffer result = encoder.encodeFrame8Bit(target2, _out);
                if (videoTrack == null)
                    videoTrack = muxer.createVideoTrack(new Size(dec.getWidth(), dec.getHeight()), "V_VP8");

                // Packet packet = new Packet(result, inFrame.getMediaPts(),
                // inFrame.getTimescale(),
                // inFrame.getDuration(), inFrame.getFrameNo(), true, null);
                byte[] array = new byte[result.limit()];
                System.arraycopy(result.array(), result.position(), array, 0, array.length);
                videoTrack.addSampleEntry(ByteBuffer.wrap(array), i - 1);

                if (i % 100 == 0) {
                    long elapse = System.currentTimeMillis() - start;
                    System.out.println((i * 100 / totalFrames) + "%, " + (i * 1000 / elapse) + "fps");
                }
            }
            muxer.mux(sink);
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
        return TranscodeMain.formats(Format.MKV);
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
}