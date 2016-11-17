package org.jcodec.samples.transcode;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;
import static org.jcodec.common.model.Rational.HALF;
import static org.jcodec.common.model.Unit.SEC;
import static org.jcodec.common.tools.MainUtils.tildeExpand;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.codecs.y4m.Y4MDecoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Rational;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.scale.Yuv420pToYuv422p8Bit;

class Y4m2prores implements Profile {
    public void transcode(Cmd cmd) throws IOException {
        SeekableByteChannel y4m = readableChannel(tildeExpand(cmd.getArg(0)));

        Y4MDecoder frames = new Y4MDecoder(y4m);

        Picture8Bit outPic = Picture8Bit.create(frames.getWidth(), frames.getHeight(), ColorSpace.YUV420);

        SeekableByteChannel sink = null;
        MP4Muxer muxer = null;
        try {
            sink = writableChannel(tildeExpand(cmd.getArg(1)));
            Rational fps = frames.getFps();
            if (fps == null) {
                System.out.println("Can't get fps from the input, assuming 24");
                fps = new Rational(24, 1);
            }
            muxer = MP4Muxer.createMP4MuxerToChannel(sink);
            ProresEncoder encoder = new ProresEncoder(ProresEncoder.Profile.HQ, false);

            Yuv420pToYuv422p8Bit color = new Yuv420pToYuv422p8Bit();
            FramesMP4MuxerTrack videoTrack = muxer.addVideoTrack("apch", frames.getSize(), TranscodeMain.APPLE_PRO_RES_422,
                    fps.getNum());
            videoTrack.setTgtChunkDuration(HALF, SEC);
            Picture8Bit picture = Picture8Bit.create(frames.getSize().getWidth(), frames.getSize().getHeight(),
                    ColorSpace.YUV422);
            Picture8Bit frame;
            int i = 0;
            ByteBuffer buf = ByteBuffer.allocate(frames.getSize().getWidth() * frames.getSize().getHeight() * 6);
            while ((frame = frames.nextFrame8Bit(outPic.getData())) != null) {
                color.transform(frame, picture);
                encoder.encodeFrame8Bit(picture, buf);
                // TODO: Error if chunk has more then one frame
                videoTrack.addFrame(MP4Packet.createMP4Packet(buf, i * fps.getDen(), fps.getNum(), fps.getDen(), i,
                        true, null, 0, i * fps.getDen(), 0));
                i++;
            }
        } finally {
            if (muxer != null)
                muxer.writeHeader();
            if (sink != null)
                sink.close();
        }
    }

    @Override
    public void printHelp(PrintStream err) {
        MainUtils.printHelpVarArgs(new HashMap<String, String>() {
            {
            }
        }, "in file", "out file");
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.Y4M);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.MOV);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.RAW);
    }

    @Override
    public Set<Codec> outputVideoCodec() {
        return TranscodeMain.codecs(Codec.PRORES);
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