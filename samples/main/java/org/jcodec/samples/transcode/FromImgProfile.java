package org.jcodec.samples.transcode;

import static org.jcodec.common.tools.MainUtils.tildeExpand;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.imgseq.ImageSequenceDemuxer;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform8Bit;

/**
 * Converts image sequence to any format (impemented in derived classes).
 * 
 * @author Stanislav Vitvitskiy
 *
 */
abstract class FromImgProfile implements Profile {
    private static final String FLAG_MAX_FRAMES = "maxFrames";

    // Protected interface
    protected abstract VideoEncoder getEncoder();

    protected abstract FramesMP4MuxerTrack getMuxerTrack(SeekableByteChannel sink, DemuxerTrackMeta inTrackMeta,
            Picture8Bit yuv) throws IOException;

    protected abstract void finalizeMuxer() throws IOException;

    protected abstract MP4Packet encodeFrame(VideoEncoder encoder, Picture8Bit yuv, Packet inPacket, ByteBuffer buf);

    @Override
    public void transcode(Cmd cmd) throws IOException {
        FileChannelWrapper sink = null;
        ImageSequenceDemuxer demuxer = null;
        try {
            sink = NIOUtils.writableFileChannel(cmd.getArg(1));
            VideoEncoder encoder = getEncoder();
            ColorSpace encoderColor = encoder.getSupportedColorSpaces()[0];
            Transform8Bit transform8Bit = ColorUtil.getTransform8Bit(ColorSpace.RGB, encoderColor);

            demuxer = new ImageSequenceDemuxer(tildeExpand(cmd.getArg(0)).getAbsolutePath(),
                    cmd.getIntegerFlagD(FLAG_MAX_FRAMES, Integer.MAX_VALUE));

            Packet inPacket = null;
            FramesMP4MuxerTrack track = null;
            while ((inPacket = demuxer.nextFrame()) != null) {
                byte[] array = NIOUtils.toArray(inPacket.getData());
                BufferedImage rgb = ImageIO.read(new ByteArrayInputStream(array));

                Picture8Bit yuv = Picture8Bit.create(rgb.getWidth(), rgb.getHeight(),
                        encoder.getSupportedColorSpaces()[0]);
                transform8Bit.transform(AWTUtil.fromBufferedImageRGB8Bit(rgb), yuv);
                ByteBuffer buf = ByteBuffer.allocate(rgb.getWidth() * rgb.getHeight() * 3);

                MP4Packet outPacket = encodeFrame(encoder, yuv, inPacket, buf);

                if (track == null) {
                    track = getMuxerTrack(sink, demuxer.getMeta(), yuv);
                }
                track.addFrame(outPacket);
            }
            finalizeMuxer();
            if (inPacket == null) {
                System.out.println("Image sequence not found");
                return;
            }
        } finally {
            IOUtils.closeQuietly(sink);
            IOUtils.closeQuietly(demuxer);
        }
    }

    @Override
    public void printHelp(PrintStream err) {
        MainUtils.printHelpVarArgs(new HashMap<String, String>() {
            {
                put(FLAG_MAX_FRAMES, "Number of frames to transcode");
            }
        }, "pattern", "out file");
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.IMG);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
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