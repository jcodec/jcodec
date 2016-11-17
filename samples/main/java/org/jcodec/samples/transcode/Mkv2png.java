package org.jcodec.samples.transcode;

import static java.lang.String.format;
import static org.jcodec.common.model.ColorSpace.RGB;
import static org.jcodec.common.tools.MainUtils.tildeExpand;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer.VideoTrack;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.RgbToBgr8Bit;
import org.jcodec.scale.Transform8Bit;
import org.jcodec.scale.Yuv420pToRgb8Bit;

class Mkv2png implements Profile {
    @Override
    public void transcode(Cmd cmd) throws IOException {
        File file = tildeExpand(cmd.getArg(0));
        if (!file.exists()) {
            System.out.println("Input file doesn't exist");
            return;
        }

        FileInputStream inputStream = new FileInputStream(file);
        LinkedList<Packet> presentationStack = new LinkedList<Packet>();
        try {
            MKVDemuxer demux = new MKVDemuxer(new FileChannelWrapper(inputStream.getChannel()));

            H264Decoder decoder = new H264Decoder();
            Transform8Bit transform = new Yuv420pToRgb8Bit();
            RgbToBgr8Bit rgbToBgr = new RgbToBgr8Bit();

            DemuxerTrack inTrack = demux.getVideoTracks().get(0);

            Picture8Bit rgb = Picture8Bit.create(demux.getPictureWidth(), demux.getPictureHeight(), ColorSpace.RGB);
            BufferedImage bi = new BufferedImage(demux.getPictureWidth(), demux.getPictureHeight(),
                    BufferedImage.TYPE_3BYTE_BGR);
            AvcCBox avcC = AvcCBox.createEmpty();
            avcC.parse(((VideoTrack) inTrack).getCodecState());

            decoder.addSps(avcC.getSpsList());
            decoder.addPps(avcC.getPpsList());

            Packet inFrame;
            int gopSize = 0;
            int prevGopsSize = 0;
            for (int i = 1; (inFrame = inTrack.nextFrame()) != null && i <= 200; i++) {
                Picture8Bit buf = Picture8Bit.create(demux.getPictureWidth(), demux.getPictureHeight(),
                        ColorSpace.YUV420);
                Frame pic = (Frame) decoder.decodeFrame8BitFromNals(
                        H264Utils.splitMOVPacket(inFrame.getData(), avcC), buf.getData());
                if (pic.getPOC() == 0) {
                    prevGopsSize += gopSize;
                    gopSize = 1;
                } else
                    gopSize++;

                if (bi == null)
                    bi = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                if (rgb == null)
                    rgb = Picture8Bit.create(pic.getWidth(), pic.getHeight(), RGB);
                transform.transform(pic, rgb);
                rgbToBgr.transform(rgb, rgb);
                AWTUtil.toBufferedImage8Bit(rgb, bi);
                int framePresentationIndex = (pic.getPOC() >> 1) + prevGopsSize;
                System.out.println("farme" + framePresentationIndex + ".png  (" + pic.getPOC() + ">>2 == "
                        + (pic.getPOC() >> 1) + " ) +" + prevGopsSize);
                ImageIO.write(bi, "png", tildeExpand(format(cmd.getArg(1), framePresentationIndex)));
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
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
        return TranscodeMain.formats(Format.MKV);
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