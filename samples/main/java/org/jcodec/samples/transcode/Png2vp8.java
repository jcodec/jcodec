package org.jcodec.samples.transcode;

import static org.jcodec.common.tools.MainUtils.tildeExpand;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jcodec.codecs.vpx.IVFMuxer;
import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.RgbToYuv420p8Bit;

class Png2vp8 implements Profile {
    @Override
    public void transcode(Cmd cmd) throws IOException {
        FileChannelWrapper sink = null;
        try {
            sink = NIOUtils.writableChannel(tildeExpand(cmd.getArg(1)));
            VP8Encoder encoder = VP8Encoder.createVP8Encoder(10); // qp
            RgbToYuv420p8Bit transform = new RgbToYuv420p8Bit();

            IVFMuxer muxer = null;

            int i;
            for (i = 0; i < cmd.getIntegerFlagD("maxFrames", Integer.MAX_VALUE); i++) {
                System.out.println(i);
                File nextImg = new File(String.format(cmd.getArg(0), i));
                if (!nextImg.exists())
                    break;

                BufferedImage rgb = ImageIO.read(nextImg);
                Picture8Bit yuv = Picture8Bit.create(rgb.getWidth(), rgb.getHeight(), ColorSpace.YUV420);
                transform.transform(AWTUtil.fromBufferedImageRGB8Bit(rgb), yuv);
                ByteBuffer buf = ByteBuffer.allocate(rgb.getWidth() * rgb.getHeight() * 3);

                ByteBuffer ff = encoder.encodeFrame8Bit(yuv, buf);
                Packet packet = Packet.createPacket(ff, i, 1, 1, i, true, null);

                if (muxer == null)
                    muxer = new IVFMuxer(sink, rgb.getWidth(), rgb.getHeight(), 25);

                muxer.addFrame(packet);
            }
            if (i == 1) {
                System.out.println("Image sequence not found");
                return;
            }
        } finally {
            NIOUtils.closeQuietly(sink);
        }
    }

    @Override
    public void printHelp(PrintStream err) {
        MainUtils.printHelpVarArgs(new HashMap<String, String>() {
            {
                put("maxFrames", "Number of frames to transcode");
            }
        }, "pattern", "out file");
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.IMG);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.IVF);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.PNG);
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