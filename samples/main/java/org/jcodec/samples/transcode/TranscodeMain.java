package org.jcodec.samples.transcode;

import static java.lang.String.format;
import static org.jcodec.codecs.h264.H264Utils.splitMOVPacket;
import static org.jcodec.common.NIOUtils.readableFileChannel;
import static org.jcodec.common.NIOUtils.writableFileChannel;
import static org.jcodec.common.model.ColorSpace.RGB;
import static org.jcodec.common.model.Rational.HALF;
import static org.jcodec.common.model.Unit.SEC;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

import javax.imageio.ImageIO;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.MappedH264ES;
import org.jcodec.codecs.h264.encode.ConstantRateControl;
import org.jcodec.codecs.h264.encode.DumbRateControl;
import org.jcodec.codecs.h264.encode.RateControl;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.codecs.prores.ProresEncoder.Profile;
import org.jcodec.codecs.prores.ProresToThumb2x2;
import org.jcodec.codecs.vp8.VP8Decoder;
import org.jcodec.codecs.y4m.Y4MDecoder;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.IOUtils;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mkv.MKVDemuxer;
import org.jcodec.containers.mkv.MKVDemuxer.VideoTrack;
import org.jcodec.containers.mkv.MKVMuxer;
import org.jcodec.containers.mkv.MKVMuxer.MKVMuxerTrack;
import org.jcodec.containers.mkv.elements.BlockElement;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4DemuxerException;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.LeafBox;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.FramesMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.RgbToYuv420;
import org.jcodec.scale.RgbToYuv422;
import org.jcodec.scale.Transform;
import org.jcodec.scale.Yuv420pToRgb;
import org.jcodec.scale.Yuv420pToYuv422p;
import org.jcodec.scale.Yuv422pToRgb;
import org.jcodec.scale.Yuv422pToYuv420p;

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
        if ("mpeg2png".equals(args[0]))
            mpeg2png(args[1], args[2]);
        else if ("png2prores".equals(args[0]))
            png2prores(args[1], args[2], args.length > 3 ? args[3] : "apch");
        else if ("y4m2prores".equals(args[0]))
            y4m2prores(args[1], args[2]);
        else if ("png2avc".equals(args[0])) {
            png2avc(args[1], args[2]);
        } else if ("prores2avc".equals(args[0])) {
            prores2avc(args[1], args[2], new ProresDecoder(), new DumbRateControl());
        } else if ("prores2avct".equals(args[0])) {
            ConstantRateControl rc = new ConstantRateControl(512);
            System.out.println("Target frame size: " + rc.calcFrameSize(510));
            prores2avc(args[1], args[2], new ProresToThumb2x2(), rc);
        } else if ("avc2png".equals(args[0])) {
            avc2png(args[1], args[2]);
        } else if ("avc2prores".equals(args[0])) {
            avc2prores(args[1], args[2], false);
        } else if ("avcraw2prores".equals(args[0])) {
            avc2prores(args[1], args[2], true);
        } else if ("png2mkv".equals(args[0])) {
            png2mkv(args[1], args[2]);
        } else if ("mkv2png".equals(args[0])){
            mkv2png(args[1], args[2]);
        } else if ("webm2png".equals(args[0])) {
            webm2png(args[1], args[2]);
        }
    }
    
    private static void webm2png(String in, String out) throws IOException {
        File file = new File(in);
        if (!file.exists()) {
            System.out.println("Input file doesn't exist");
            return;
        }

        FileInputStream inputStream = new FileInputStream(file);
        try {
            MKVDemuxer demux = MKVDemuxer.getDemuxer(inputStream.getChannel());

            VP8Decoder decoder = new VP8Decoder();
            Transform transform = new Yuv420pToRgb(0, 0);

            VideoTrack inTrack = demux.getVideoTrack();

            Picture rgb = Picture.create((int)inTrack.pixelWidth, (int)inTrack.pixelHeight, ColorSpace.RGB);
            BufferedImage bi = new BufferedImage((int)inTrack.pixelWidth, (int)inTrack.pixelHeight, BufferedImage.TYPE_3BYTE_BGR);

            Packet inFrame;
            int totalFrames = (int) inTrack.getFrameCount();
            for (int i = 1; (inFrame = inTrack.getFrames(1)) != null && i <= 200; i++) {
                if (!inFrame.isKeyFrame())
                    continue;
                
                decoder.decode(inFrame.getData());
                Picture pic =  decoder.getPicture();
                if (bi == null)
                    bi = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                if (rgb == null)
                    rgb = Picture.create(pic.getWidth(), pic.getHeight(), RGB);
                transform.transform(pic, rgb);
                AWTUtil.toBufferedImage(rgb, bi);
                ImageIO.write(bi, "png", new File(format(out, i++)));

                if (i % 100 == 0)
                    System.out.println((i * 100 / totalFrames) + "%");
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private static void png2mkv(String pattern, String out) throws IOException {
        FileOutputStream fos = new FileOutputStream(tildeExpand(out));
        FileChannelWrapper sink = null;
        try {
            sink = new FileChannelWrapper(fos.getChannel());
            MKVMuxer muxer = new MKVMuxer(sink);

            H264Encoder encoder = new H264Encoder();
            RgbToYuv420 transform = new RgbToYuv420(0, 0);

            MKVMuxerTrack videoTrack = null;
            int i;
            for (i = 1;; i++) {
                File nextImg = tildeExpand(format(pattern, i));
                if (!nextImg.exists())
                    break;
                BufferedImage rgb = ImageIO.read(nextImg);

                if (videoTrack == null) {
                    videoTrack = muxer.addVideoTrack(new Size(rgb.getWidth(), rgb.getHeight()), "V_MPEG4/ISO/AVC");
                    videoTrack.setTgtChunkDuration(Rational.ONE, SEC);
                }
                Picture yuv = Picture.create(rgb.getWidth(), rgb.getHeight(), ColorSpace.YUV420);
                transform.transform(AWTUtil.fromBufferedImage(rgb), yuv);
                ByteBuffer buf = ByteBuffer.allocate(rgb.getWidth() * rgb.getHeight() * 3);

                ByteBuffer ff = encoder.encodeFrame(buf, yuv);

                BlockElement se = BlockElement.keyFrame(videoTrack.trackId, i-1, ff.array());
                videoTrack.addSampleEntry(se);
            }
            if (i == 1) {
                System.out.println("Image sequence not found");
                return;
            }
            muxer.mux();
        } finally {
            IOUtils.closeQuietly(fos);
            if (sink != null)
                sink.close();
            
        }
    }
    
    public static File tildeExpand(String path) {
        if (path.startsWith("~")) {
            path = path.replaceFirst("~", System.getProperty("user.home"));
        }
        return new File(path);
    }

    private static void mkv2png(String in, String out) throws IOException {
        File file = new File(in);
        if (!file.exists()) {
            System.out.println("Input file doesn't exist");
            return;
        }

        FileInputStream inputStream = new FileInputStream(file);
        try {
            MKVDemuxer demux = MKVDemuxer.getDemuxer(inputStream.getChannel());

            H264Decoder decoder = new H264Decoder();
            Transform transform = new Yuv420pToRgb(0, 0);

            VideoTrack inTrack = demux.getVideoTrack();

            
            Picture target1 = Picture.create(((int)inTrack.pixelWidth + 15) & ~0xf, ((int)inTrack.pixelHeight + 15) & ~0xf, ColorSpace.YUV420);
            Picture rgb = Picture.create((int)inTrack.pixelWidth, (int)inTrack.pixelHeight, ColorSpace.RGB);
            ByteBuffer _out = ByteBuffer.allocate((int)inTrack.pixelWidth * (int)inTrack.pixelHeight * 6);
            BufferedImage bi = new BufferedImage((int)inTrack.pixelWidth, (int)inTrack.pixelHeight, BufferedImage.TYPE_3BYTE_BGR);
            AvcCBox avcC = new AvcCBox();
            avcC.parse(inTrack.codecState);

            decoder.addSps(avcC.getSpsList());
            decoder.addPps(avcC.getPpsList());

            Packet inFrame;
            int totalFrames = (int) inTrack.getFrameCount();
            for (int i = 1; (inFrame = inTrack.getFrames(1)) != null && i <= 200; i++) {
                Picture buf = Picture.create(1920, 1088, ColorSpace.YUV422_10);
                Picture pic = decoder.decodeFrame(inFrame.getData(), buf.getData());
                if (bi == null)
                    bi = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                if (rgb == null)
                    rgb = Picture.create(pic.getWidth(), pic.getHeight(), RGB);
                transform.transform(pic, rgb);
                AWTUtil.toBufferedImage(rgb, bi);
                ImageIO.write(bi, "png", new File(format(out, i++)));

                if (i % 100 == 0)
                    System.out.println((i * 100 / totalFrames) + "%");
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private static void avc2prores(String in, String out, boolean raw) throws IOException {
        SeekableByteChannel sink = null;
        SeekableByteChannel source = null;
        try {

            sink = writableFileChannel(out);

            H264Decoder decoder = new H264Decoder();

            int totalFrames = Integer.MAX_VALUE;
            PixelAspectExt pasp = null;
            DemuxerTrack videoTrack;
            int width = 0, height = 0;
            AvcCBox avcC = null;
            if (!raw) {
                source = readableFileChannel(in);
                MP4Demuxer demux = new MP4Demuxer(source);
                AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();
                VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];

                totalFrames = (int) inTrack.getFrameCount();
                pasp = Box.findFirst(inTrack.getSampleEntries()[0], PixelAspectExt.class, "pasp");

                avcC = Box.as(AvcCBox.class, Box.findFirst(ine, LeafBox.class, "avcC"));
                decoder.addSps(avcC.getSpsList());
                decoder.addPps(avcC.getPpsList());
                videoTrack = inTrack;

                width = (ine.getWidth() + 15) & ~0xf;
                height = (ine.getHeight() + 15) & ~0xf;
            } else {
                videoTrack = new MappedH264ES(NIOUtils.fetchFrom(new File(in)));
            }
            MP4Muxer muxer = new MP4Muxer(sink, Brand.MOV);

            ProresEncoder encoder = new ProresEncoder(Profile.HQ);

            Transform transform = new Yuv420pToYuv422p(2, 0);

            int timescale = 24000;
            int frameDuration = 1000;
            FramesMP4MuxerTrack outTrack = null;

            int gopLen = 0, i;
            Frame[] gop = new Frame[1000];
            Packet inFrame;

            int sf = 90000;
            if (!raw) {
                AbstractMP4DemuxerTrack dt = (AbstractMP4DemuxerTrack) videoTrack;
                dt.gotoFrame(sf);
                while ((inFrame = videoTrack.nextFrame()) != null && !inFrame.isKeyFrame())
                    ;
                // System.out.println(inFrame.getFrameNo() + " - " +
                // inFrame.isKeyFrame());
                dt.gotoFrame(inFrame.getFrameNo());
            }
            for (i = 0; (inFrame = videoTrack.nextFrame()) != null && i < 2000;) {
                ByteBuffer data = inFrame.getData();
                Picture target1;
                Frame dec;
                if (!raw) {
                    target1 = Picture.create(width, height, ColorSpace.YUV420);
                    dec = decoder.decodeFrame(splitMOVPacket(data, avcC), target1.getData());
                } else {
                    // data = ByteBuffer.wrap(NIOUtils.toArray(data));
                    SeqParameterSet sps = ((MappedH264ES) videoTrack).getSps()[0];
                    width = (sps.pic_width_in_mbs_minus1 + 1) << 4;
                    height = H264Utils.getPicHeightInMbs(sps) << 4;
                    target1 = Picture.create(width, height, ColorSpace.YUV420);
                    dec = decoder.decodeFrame(data, target1.getData());
                }
                if (outTrack == null) {
                    outTrack = muxer.addVideoTrack("apch", new Size(dec.getCroppedWidth(), dec.getCroppedHeight()),
                            APPLE_PRO_RES_422, timescale);
                    if (pasp != null)
                        outTrack.getEntries().get(0).add(pasp);
                }
                if (dec.getPOC() == 0 && gopLen > 0) {
                    outGOP(encoder, transform, timescale, frameDuration, outTrack, gopLen, gop, totalFrames, i, width,
                            height);
                    i += gopLen;
                    gopLen = 0;
                }
                gop[gopLen++] = dec;
            }
            if (gopLen > 0) {
                outGOP(encoder, transform, timescale, frameDuration, outTrack, gopLen, gop, totalFrames, i, width,
                        height);
            }
            muxer.writeHeader();
        } finally {
            if (sink != null)
                sink.close();
            if (source != null)
                source.close();
        }
    }

    private static void outGOP(ProresEncoder encoder, Transform transform, int timescale, int frameDuration,
            FramesMP4MuxerTrack outTrack, int gopLen, Frame[] gop, int totalFrames, int i, int codedWidth, int codedHeight)
            throws IOException {

        ByteBuffer _out = ByteBuffer.allocate(codedWidth * codedHeight * 6);
        Picture target2 = Picture.create(codedWidth, codedHeight, ColorSpace.YUV422_10);
        Arrays.sort(gop, 0, gopLen, Frame.POCAsc);
        for (int g = 0; g < gopLen; g++) {
            Frame frame = gop[g];
            transform.transform(frame, target2);
            target2.setCrop(frame.getCrop());
            _out.clear();
            encoder.encodeFrame(_out, target2);
            // TODO: Error if chunk has more then one frame
            outTrack.addFrame(new MP4Packet(_out, i * frameDuration, timescale, frameDuration, i, true, null, i
                    * frameDuration, 0));

            if (i % 100 == 0)
                System.out.println((i * 100 / totalFrames) + "%");
            i++;
        }
    }

    private static void avc2png(String in, String out) throws IOException {
        SeekableByteChannel sink = null;
        SeekableByteChannel source = null;
        try {
            source = readableFileChannel(in);
            sink = writableFileChannel(out);

            MP4Demuxer demux = new MP4Demuxer(source);

            H264Decoder decoder = new H264Decoder();

            Transform transform = new Yuv420pToRgb(0, 0);

            AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();

            VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];
            Picture target1 = Picture.create((ine.getWidth() + 15) & ~0xf, (ine.getHeight() + 15) & ~0xf,
                    ColorSpace.YUV420);
            Picture rgb = Picture.create(ine.getWidth(), ine.getHeight(), ColorSpace.RGB);
            ByteBuffer _out = ByteBuffer.allocate(ine.getWidth() * ine.getHeight() * 6);
            BufferedImage bi = new BufferedImage(ine.getWidth(), ine.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            AvcCBox avcC = Box.as(AvcCBox.class, Box.findFirst(ine, LeafBox.class, "avcC"));

            decoder.addSps(avcC.getSpsList());
            decoder.addPps(avcC.getPpsList());

            Packet inFrame;
            int totalFrames = (int) inTrack.getFrameCount();
            for (int i = 0; (inFrame = inTrack.nextFrame()) != null; i++) {
                ByteBuffer data = inFrame.getData();

                Picture dec = decoder.decodeFrame(splitMOVPacket(data, avcC), target1.getData());
                transform.transform(dec, rgb);
                _out.clear();

                AWTUtil.toBufferedImage(rgb, bi);
                ImageIO.write(bi, "png", new File(format(out, i)));
                if (i % 100 == 0)
                    System.out.println((i * 100 / totalFrames) + "%");
            }
        } finally {
            if (sink != null)
                sink.close();
            if (source != null)
                source.close();
        }
    }

    private static void prores2avc(String in, String out, ProresDecoder decoder, RateControl rc) throws IOException {
        SeekableByteChannel sink = null;
        SeekableByteChannel source = null;
        try {
            sink = writableFileChannel(out);
            source = readableFileChannel(in);

            MP4Demuxer demux = new MP4Demuxer(source);
            MP4Muxer muxer = new MP4Muxer(sink, Brand.MOV);

            Transform transform = new Yuv422pToYuv420p(0, 2);

            H264Encoder encoder = new H264Encoder(rc);

            AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();
            FramesMP4MuxerTrack outTrack = muxer.addTrackForCompressed(TrackType.VIDEO, (int) inTrack.getTimescale());

            VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];
            Picture target1 = Picture.create(1920, 1088, ColorSpace.YUV422_10);
            Picture target2 = null;
            ByteBuffer _out = ByteBuffer.allocate(ine.getWidth() * ine.getHeight() * 6);

            ArrayList<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
            ArrayList<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();
            Packet inFrame;
            int totalFrames = (int) inTrack.getFrameCount();
            long start = System.currentTimeMillis();
            for (int i = 0; (inFrame = inTrack.nextFrame()) != null && i < 100; i++) {
                Picture dec = decoder.decodeFrame(inFrame.getData(), target1.getData());
                if (target2 == null) {
                    target2 = Picture.create(dec.getWidth(), dec.getHeight(), ColorSpace.YUV420);
                }
                transform.transform(dec, target2);
                _out.clear();
                ByteBuffer result = encoder.encodeFrame(_out, target2);
                if (rc instanceof ConstantRateControl) {
                    int mbWidth = (dec.getWidth() + 15) >> 4;
                    int mbHeight = (dec.getHeight() + 15) >> 4;
                    result.limit(((ConstantRateControl) rc).calcFrameSize(mbWidth * mbHeight));
                }
                spsList.clear();
                ppsList.clear();
                H264Utils.encodeMOVPacket(result, spsList, ppsList);
                outTrack.addFrame(new MP4Packet((MP4Packet) inFrame, result));
                if (i % 100 == 0) {
                    long elapse = System.currentTimeMillis() - start;
                    System.out.println((i * 100 / totalFrames) + "%, " + (i * 1000 / elapse) + "fps");
                }
            }
            outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList));

            muxer.writeHeader();
        } finally {
            if (sink != null)
                sink.close();
            if (source != null)
                source.close();
        }
    }

    private static void png2avc(String pattern, String out) throws IOException {
        FileChannel sink = null;
        try {
            sink = new FileOutputStream(new File(out)).getChannel();
            H264Encoder encoder = new H264Encoder();
            RgbToYuv420 transform = new RgbToYuv420(0, 0);

            int i;
            for (i = 0; i < 10000; i++) {
                File nextImg = new File(String.format(pattern, i));
                if (!nextImg.exists())
                    continue;
                BufferedImage rgb = ImageIO.read(nextImg);
                Picture yuv = Picture.create(rgb.getWidth(), rgb.getHeight(), ColorSpace.YUV420);
                transform.transform(AWTUtil.fromBufferedImage(rgb), yuv);
                ByteBuffer buf = ByteBuffer.allocate(rgb.getWidth() * rgb.getHeight() * 3);

                ByteBuffer ff = encoder.encodeFrame(buf, yuv);
                sink.write(ff);
            }
            if (i == 1) {
                System.out.println("Image sequence not found");
                return;
            }
        } finally {
            if (sink != null)
                sink.close();
        }
    }

    static void y4m2prores(String input, String output) throws Exception {
        SeekableByteChannel y4m = readableFileChannel(input);

        Y4MDecoder frames = new Y4MDecoder(y4m);

        Picture outPic = Picture.create(frames.getWidth(), frames.getHeight(), ColorSpace.YUV420);

        SeekableByteChannel sink = null;
        MP4Muxer muxer = null;
        try {
            sink = writableFileChannel(output);
            Rational fps = frames.getFps();
            if (fps == null) {
                System.out.println("Can't get fps from the input, assuming 24");
                fps = new Rational(24, 1);
            }
            muxer = new MP4Muxer(sink);
            ProresEncoder encoder = new ProresEncoder(Profile.HQ);

            Yuv420pToYuv422p color = new Yuv420pToYuv422p(2, 0);
            FramesMP4MuxerTrack videoTrack = muxer.addVideoTrack("apch", frames.getSize(), APPLE_PRO_RES_422, fps.getNum());
            videoTrack.setTgtChunkDuration(HALF, SEC);
            Picture picture = Picture.create(frames.getSize().getWidth(), frames.getSize().getHeight(),
                    ColorSpace.YUV422_10);
            Picture frame;
            int i = 0;
            ByteBuffer buf = ByteBuffer.allocate(frames.getSize().getWidth() * frames.getSize().getHeight() * 6);
            while ((frame = frames.nextFrame(outPic.getData())) != null) {
                color.transform(frame, picture);
                encoder.encodeFrame(buf, picture);
                // TODO: Error if chunk has more then one frame
                videoTrack.addFrame(new MP4Packet(buf, i * fps.getDen(), fps.getNum(), fps.getDen(), i, true, null, i
                        * fps.getDen(), 0));
                i++;
            }
        } finally {
            if (muxer != null)
                muxer.writeHeader();
            if (sink != null)
                sink.close();
        }

    }

    private static void prores2png(String in, String out) throws IOException {
        File file = new File(in);
        if (!file.exists()) {
            System.out.println("Input file doesn't exist");
            return;
        }

        MP4Demuxer rawDemuxer = new MP4Demuxer(readableFileChannel(file));
        FramesMP4DemuxerTrack videoTrack = (FramesMP4DemuxerTrack) rawDemuxer.getVideoTrack();
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
        while ((pkt = videoTrack.nextFrame()) != null) {
            Picture buf = Picture.create(1920, 1088, ColorSpace.YUV422_10);
            Picture pic = decoder.decodeFrame(pkt.getData(), buf.getData());
            if (bi == null)
                bi = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            if (rgb == null)
                rgb = Picture.create(pic.getWidth(), pic.getHeight(), RGB);
            transform.transform(pic, rgb);
            AWTUtil.toBufferedImage(rgb, bi);
            ImageIO.write(bi, "png", new File(format(out, i++)));
        }
    }

    private static void mpeg2png(String in, String out) throws IOException {
        File file = new File(in);
        if (!file.exists()) {
            System.out.println("Input file doesn't exist");
            return;
        }

        MP4Demuxer rawDemuxer = new MP4Demuxer(readableFileChannel(file));
        FramesMP4DemuxerTrack videoTrack = (FramesMP4DemuxerTrack) rawDemuxer.getVideoTrack();
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
        while ((pkt = videoTrack.nextFrame()) != null) {
            Picture buf = Picture.create(1920, 1080, ColorSpace.YUV422_10);
            Picture pic = decoder.decodeFrame(pkt.getData(), buf.getData());
            if (bi == null)
                bi = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            if (rgb == null)
                rgb = Picture.create(pic.getWidth(), pic.getHeight(), RGB);
            transform.transform(pic, rgb);
            AWTUtil.toBufferedImage(rgb, bi);
            ImageIO.write(bi, "png", new File(format(out, i++)));
        }
    }

    private static void png2prores(String pattern, String out, String fourcc) throws IOException, MP4DemuxerException {

        Profile profile = getProfile(fourcc);
        if (profile == null) {
            System.out.println("Unsupported fourcc: " + fourcc);
            return;
        }

        SeekableByteChannel sink = null;
        try {
            sink = writableFileChannel(new File(out));
            MP4Muxer muxer = new MP4Muxer(sink, Brand.MOV);
            ProresEncoder encoder = new ProresEncoder(profile);
            RgbToYuv422 transform = new RgbToYuv422(2, 0);

            FramesMP4MuxerTrack videoTrack = null;
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
                Picture yuv = Picture.create(rgb.getWidth(), rgb.getHeight(), ColorSpace.YUV422);
                transform.transform(AWTUtil.fromBufferedImage(rgb), yuv);
                ByteBuffer buf = ByteBuffer.allocate(rgb.getWidth() * rgb.getHeight() * 3);

                encoder.encodeFrame(buf, yuv);
                // TODO: Error if chunk has more then one frame
                videoTrack.addFrame(new MP4Packet(buf, i * 1001, 24000, 1001, i, true, null, i * 1001, 0));
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