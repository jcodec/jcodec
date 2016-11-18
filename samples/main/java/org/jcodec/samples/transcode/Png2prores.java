package org.jcodec.samples.transcode;

import static org.jcodec.common.io.NIOUtils.writableChannel;
import static org.jcodec.common.model.Rational.HALF;
import static org.jcodec.common.model.Unit.SEC;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.RgbToYuv422p8Bit;

class Png2prores implements Profile {
    private static final String DEFAULT_PROFILE = "apch";
    private static final String FLAG_FOURCC = "fourcc";
    private static final String FLAG_INTERLACED = "interlaced";

    @Override
    public void transcode(Cmd cmd) throws IOException {
        String fourccName = cmd.getStringFlagD(FLAG_FOURCC, DEFAULT_PROFILE);
        ProresEncoder.Profile profile = getProfile(fourccName);
        if (profile == null) {
            System.out.println("Unsupported fourcc: " + fourccName);
            return;
        }

        SeekableByteChannel sink = null;
        try {
            sink = writableChannel(new File(cmd.getArg(1)));
            MP4Muxer muxer = MP4Muxer.createMP4Muxer(sink, Brand.MOV);
            ProresEncoder encoder = new ProresEncoder(profile, cmd.getBooleanFlagD(FLAG_INTERLACED, false));
            RgbToYuv422p8Bit transform = new RgbToYuv422p8Bit();

            FramesMP4MuxerTrack videoTrack = null;
            int i;
            for (i = 1;; i++) {
                File nextImg = new File(String.format(cmd.getArg(0), i));
                if (!nextImg.exists())
                    break;
                BufferedImage rgb = ImageIO.read(nextImg);

                if (videoTrack == null) {
                    videoTrack = muxer.addVideoTrack(profile.fourcc, new Size(rgb.getWidth(), rgb.getHeight()),
                            TranscodeMain.APPLE_PRO_RES_422, 24000);
                    videoTrack.setTgtChunkDuration(HALF, SEC);
                }
                Picture8Bit yuv = Picture8Bit.create(rgb.getWidth(), rgb.getHeight(), ColorSpace.YUV422);
                transform.transform(AWTUtil.fromBufferedImageRGB8Bit(rgb), yuv);
                ByteBuffer buf = ByteBuffer.allocate(rgb.getWidth() * rgb.getHeight() * 3);

                encoder.encodeFrame8Bit(yuv, buf);
                // TODO: Error if chunk has more then one frame
                videoTrack
                        .addFrame(MP4Packet.createMP4Packet(buf, i * 1001, 24000, 1001, i, true, null, 0, i * 1001, 0));
            }
            if (i == 1) {
                System.out.println("Image sequence not found");
                return;
            }
            muxer.writeHeader();
        } finally {
            if (sink != null)
                sink.close();
        }
    }

    @Override
    public void printHelp(PrintStream err) {
        MainUtils.printHelpVarArgs(new HashMap<String, String>() {
            {
                put(FLAG_FOURCC, "Prores profile fourcc.");
                put(FLAG_INTERLACED, "Should use interlaced encoding?");
            }
        }, "pattern", "out file");
    }

    private static ProresEncoder.Profile getProfile(String fourcc) {
        for (ProresEncoder.Profile profile2 : ProresEncoder.Profile.values()) {
            if (fourcc.equals(profile2.fourcc))
                return profile2;
        }
        return null;
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.IMG);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.MOV);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.PNG);
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