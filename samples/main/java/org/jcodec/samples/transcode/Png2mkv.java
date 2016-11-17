package org.jcodec.samples.transcode;

import static java.lang.String.format;
import static org.jcodec.common.tools.MainUtils.tildeExpand;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mkv.muxer.MKVMuxer;
import org.jcodec.containers.mkv.muxer.MKVMuxerTrack;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.RgbToYuv420p8Bit;

class Png2mkv implements Profile {
    @Override
    public void transcode(Cmd cmd) throws IOException {
        FileOutputStream fos = new FileOutputStream(tildeExpand(cmd.getArg(1)));
        FileChannelWrapper sink = null;
        try {
            sink = new FileChannelWrapper(fos.getChannel());
            MKVMuxer muxer = new MKVMuxer();

            H264Encoder encoder = H264Encoder.createH264Encoder();
            RgbToYuv420p8Bit transform = new RgbToYuv420p8Bit();

            MKVMuxerTrack videoTrack = null;
            int i;
            for (i = 1;; i++) {
                File nextImg = tildeExpand(format(cmd.getArg(0), i));
                if (!nextImg.exists())
                    break;
                BufferedImage rgb = ImageIO.read(nextImg);

                if (videoTrack == null) {
                    videoTrack = muxer.createVideoTrack(new Size(rgb.getWidth(), rgb.getHeight()),
                            "V_MPEG4/ISO/AVC");
                }

                Picture8Bit yuv = Picture8Bit.create(rgb.getWidth(), rgb.getHeight(), ColorSpace.YUV420);
                transform.transform(AWTUtil.fromBufferedImageRGB8Bit(rgb), yuv);
                ByteBuffer buf = ByteBuffer.allocate(rgb.getWidth() * rgb.getHeight() * 3);

                ByteBuffer ff = encoder.encodeFrame8Bit(yuv, buf);
                videoTrack.addSampleEntry(ff, i);
            }
            if (i == 1) {
                System.out.println("Image sequence not found");
                return;
            }
            muxer.mux(sink);
        } finally {
            IOUtils.closeQuietly(fos);
            if (sink != null)
                sink.close();

        }
    }

    @Override
    public void printHelp(PrintStream err) {
        MainUtils.printHelpVarArgs(new HashMap<String, String>() {
            {
            }
        }, "pattern", "out file");
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.IMG);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.MKV);
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