package org.jcodec.samples.transcode;

import static java.lang.String.format;
import static org.jcodec.common.io.NIOUtils.readableFileChannel;
import static org.jcodec.common.tools.MainUtils.tildeExpand;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.containers.mps.MPEGDemuxer.MPEGDemuxerTrack;
import org.jcodec.scale.AWTUtil;

class Hls2png implements Profile {
    @Override
    public void transcode(Cmd cmd) throws IOException {
        SeekableByteChannel source = null;
        try {
            Format f = JCodecUtil.detectFormat(tildeExpand(cmd.getArg(0)));
            System.out.println(f);
            source = readableFileChannel(cmd.getArg(0));

            Set<Integer> programs = MTSDemuxer.getProgramsFromChannel(source);
            MTSDemuxer demuxer = new MTSDemuxer(source, programs.iterator().next());

            H264Decoder decoder = new H264Decoder();

            MPEGDemuxerTrack track = demuxer.getVideoTracks().get(0);
            List<? extends MPEGDemuxerTrack> audioTracks = demuxer.getAudioTracks();

            ByteBuffer buf = ByteBuffer.allocate(1920 * 1088);
            Picture8Bit target1 = Picture8Bit.create(1920, 1088, ColorSpace.YUV420J);

            Packet inFrame;
            for (int i = 0; (inFrame = track.nextFrameWithBuffer(buf)) != null; i++) {
                ByteBuffer data = inFrame.getData();
                Picture8Bit dec = decoder.decodeFrame8Bit(data, target1.getData());
                AWTUtil.savePicture(dec, "png", tildeExpand(format(cmd.getArg(1), i)));
            }
        } finally {
            if (source != null)
                source.close();
        }
    }

    @Override
    public void printHelp(PrintStream err) {
        MainUtils.printHelpVarArgs(new HashMap<String, String>() {
            {
            }
        }, "in file", "pattern");
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MPEG_TS);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.IMG);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.H264);
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