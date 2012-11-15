package org.jcodec.samples.transcode;

import static java.lang.String.format;
import static org.jcodec.common.JCodecUtil.bufin;
import static org.jcodec.common.model.ColorSpace.RGB;
import static org.jcodec.common.model.ColorSpace.YUV420;
import static org.jcodec.common.model.Rational.HALF;
import static org.jcodec.common.model.Unit.SEC;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.EnumSet;

import javax.imageio.ImageIO;

import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.codecs.prores.ProresEncoder.Profile;
import org.jcodec.codecs.y4m.Y4MDecoder;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.FileRAInputStream;
import org.jcodec.common.io.FileRAOutputStream;
import org.jcodec.common.io.RAOutputStream;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.Unit;
import org.jcodec.containers.mp4.MP4Demuxer;
import org.jcodec.containers.mp4.MP4Demuxer.DemuxerTrack;
import org.jcodec.containers.mp4.MP4Demuxer.FramesTrack;
import org.jcodec.containers.mp4.MP4DemuxerException;
import org.jcodec.containers.mp4.MP4Muxer;
import org.jcodec.containers.mp4.MP4Muxer.CompressedTrack;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.RgbToYuv422;
import org.jcodec.scale.Yuv420pToYuv422p;
import org.jcodec.scale.Yuv422pToRgb;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class TranscodeMain {

    private static final String APPLE_PRO_RES_422 = "Apple ProRes 422";

    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.out.println("Transcoder. Transcodes a prores movie into a set of png files\n"
                    + "Syntax: <type> <input file> <output file> [profile]\n"
                    + "\ttype: 'png2prores' or 'prores2png'\n" + "\tpng file name should be set c format string\n"
                    + "\tprofile: 'apch' (HQ) , 'apcn' (Standard) , 'apcs' (LT), 'apco' (Proxy).");

            return;
        }
        if ("prores2png".equals(args[0]))
            prores2png(args[1], args[2]);
        else if ("png2prores".equals(args[0]))
            png2prores(args[1], args[2], args.length > 3 ? args[3] : "apch");
        else if ("y4m2prores".equals(args[0]))
            y4m2prores(args[1], args[2]);
    }

    static void y4m2prores(String input, String output) throws Exception {
        InputStream y4m;
        if ("-".equals(input)) {
            y4m = System.in;
        } else {
            y4m = new BufferedInputStream(new FileInputStream(input));
        }

        Y4MDecoder frames = new Y4MDecoder(y4m);

        RAOutputStream sink = null;
        MP4Muxer muxer = null;
        try {
            sink = new FileRAOutputStream(new File(output));
            Rational fps = frames.getFps();
            if (fps == null) {
                System.out.println("Can't get fps from the input, assuming 24");
                fps = new Rational(24, 1);
            }
            muxer = new MP4Muxer(sink);
            ProresEncoder encoder = new ProresEncoder(Profile.HQ);

            Yuv420pToYuv422p color = new Yuv420pToYuv422p(2, 0);
            CompressedTrack videoTrack = muxer.addVideoTrack("apch", frames.getSize(), APPLE_PRO_RES_422, fps.getNum());
            videoTrack.setTgtChunkDuration(HALF, SEC);
            Picture picture = Picture.create(frames.getSize().getWidth(), frames.getSize().getHeight(),
                    ColorSpace.YUV422_10);
            Picture frame;
            int i = 0;
            while ((frame = frames.nextFrame()) != null) {
                color.transform(frame, picture);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encoder.encodeFrame(baos, picture);
                videoTrack.addFrame(new MP4Packet(new Buffer(baos.toByteArray()), i * fps.getDen(), fps.getNum(), fps
                        .getDen(), i, true, null, i * fps.getDen(), 0));
                i++;
            }
        } finally {
            if (muxer != null)
                muxer.writeHeader();
            if (sink != null)
                sink.close();
        }

    }

    private static void prores2png(String in, String out) throws IOException, MP4DemuxerException {
        File file = new File(in);
        if (!file.exists()) {
            System.out.println("Input file doesn't exist");
            return;
        }

        MP4Demuxer rawDemuxer = new MP4Demuxer(bufin(file));
        FramesTrack videoTrack = (FramesTrack) rawDemuxer.getVideoTrack();
        if (videoTrack == null) {
            System.out.println("Video track not found");
            return;
        }
        Yuv422pToRgb transform = new Yuv422pToRgb(2, 0);

        ProresDecoder decoder = new ProresDecoder();
        BufferedImage bi = null;
        Picture rgb = null;
        int i = 0;
        Packet pkt;
        while ((pkt = videoTrack.getFrames(1)) != null) {
            Picture buf = Picture.create(1920, 1080, ColorSpace.YUV422_10);
            Picture pic = decoder.decodeFrame(pkt.getData(), buf.getData());
            if (bi == null)
                bi = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            if (rgb == null)
                rgb = Picture.create(pic.getWidth(), pic.getHeight(), RGB);
            transform.transform(pic, rgb);
            ImageIO.write(bi, "png", new File(format(out, i++)));
        }
    }

    private static void png2prores(String pattern, String out, String fourcc) throws IOException, MP4DemuxerException {

        Profile profile = getProfile(fourcc);
        if (profile == null) {
            System.out.println("Unsupported fourcc: " + fourcc);
            return;
        }

        RAOutputStream sink = null;
        try {
            sink = new FileRAOutputStream(new File(out));
            MP4Muxer muxer = new MP4Muxer(sink);
            ProresEncoder encoder = new ProresEncoder(profile);
            RgbToYuv422 transform = new RgbToYuv422(2, 0);
            Picture yuv = null;

            CompressedTrack videoTrack = null;
            int i;
            for (i = 1;; i++) {
                File nextImg = new File(String.format(pattern, i));
                if (!nextImg.exists())
                    break;
                BufferedImage rgb = ImageIO.read(nextImg);

                if (videoTrack == null) {
                    videoTrack = muxer.addVideoTrack(profile.fourcc, new Size(rgb.getWidth(), rgb.getHeight()),
                            APPLE_PRO_RES_422, 24000);
                    videoTrack.setTgtChunkDuration(HALF, SEC);
                }
                if (yuv == null)
                    yuv = Picture.create(rgb.getWidth(), rgb.getHeight(), YUV420);
                transform.transform(AWTUtil.fromBufferedImage(rgb), yuv);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encoder.encodeFrame(baos, yuv);
                videoTrack.addFrame(new MP4Packet(new Buffer(baos.toByteArray()), i * 1001, 24000, 1001, i, true, null,
                        i * 1001, 0));
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

    private static Profile getProfile(String fourcc) {
        for (Profile profile2 : EnumSet.allOf(Profile.class)) {
            if (fourcc.equals(profile2.fourcc))
                return profile2;
        }
        return null;
    }
}