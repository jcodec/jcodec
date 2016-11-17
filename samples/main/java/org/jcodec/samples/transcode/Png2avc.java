package org.jcodec.samples.transcode;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.RgbToYuv420p8Bit;

class Png2avc implements Profile {
    @Override
    public void transcode(Cmd cmd) throws IOException {
        FileChannel sink = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(cmd.getArg(1)));
            sink = fos.getChannel();
            H264Encoder encoder = H264Encoder.createH264Encoder();
            RgbToYuv420p8Bit transform = new RgbToYuv420p8Bit();

            int i;
            for (i = 0; i < cmd.getIntegerFlagD("maxFrames", Integer.MAX_VALUE); i++) {
                File nextImg = new File(String.format(cmd.getArg(0), i));
                if (!nextImg.exists())
                    break;
                BufferedImage rgb = ImageIO.read(nextImg);
                Picture8Bit yuv = Picture8Bit.create(rgb.getWidth(), rgb.getHeight(),
                        encoder.getSupportedColorSpaces()[0]);
                transform.transform(AWTUtil.fromBufferedImageRGB8Bit(rgb), yuv);
                ByteBuffer buf = ByteBuffer.allocate(rgb.getWidth() * rgb.getHeight() * 3);

                ByteBuffer ff = encoder.encodeFrame8Bit(yuv, buf);
                sink.write(ff);
            }
            if (i == 1) {
                System.out.println("Image sequence not found");
                return;
            }
        } finally {
            IOUtils.closeQuietly(sink);
            IOUtils.closeQuietly(fos);
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
        return TranscodeMain.formats(Format.H264);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.PNG);
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