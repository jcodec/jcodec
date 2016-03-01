package org.jcodec.samples.transcode;

import static java.lang.Math.min;
import static java.lang.String.format;
import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.readableFileChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;
import static org.jcodec.common.io.NIOUtils.writableFileChannel;
import static org.jcodec.common.model.ColorSpace.RGB;
import static org.jcodec.common.model.Rational.HALF;
import static org.jcodec.common.model.Unit.SEC;
import static org.jcodec.common.tools.MainUtils.tildeExpand;
import static org.jcodec.containers.mp4.TrackType.SOUND;
import static org.jcodec.containers.mp4.TrackType.VIDEO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jcodec.codecs.aac.AACConts;
import org.jcodec.codecs.aac.ADTSParser;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.MappedH264ES;
import org.jcodec.codecs.h264.encode.DumbRateControl;
import org.jcodec.codecs.h264.encode.H264FixedRateControl;
import org.jcodec.codecs.h264.encode.RateControl;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.codecs.mjpeg.JpegDecoder;
import org.jcodec.codecs.mjpeg.JpegToThumb2x2;
import org.jcodec.codecs.mjpeg.JpegToThumb4x4;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.mpeg4.mp4.EsdsBox;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.codecs.prores.ProresToThumb;
import org.jcodec.codecs.prores.ProresToThumb2x2;
import org.jcodec.codecs.prores.ProresToThumb4x4;
import org.jcodec.codecs.vp8.VP8Decoder;
import org.jcodec.codecs.vpx.IVFMuxer;
import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.codecs.y4m.Y4MDecoder;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.DemuxerTrackMeta.Type;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.JCodecUtil.Format;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer.VideoTrack;
import org.jcodec.containers.mkv.muxer.MKVMuxer;
import org.jcodec.containers.mkv.muxer.MKVMuxerTrack;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.FramesMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mps.MPEGDemuxer;
import org.jcodec.containers.mps.MPEGDemuxer.MPEGDemuxerTrack;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.RgbToBgr;
import org.jcodec.scale.RgbToYuv420p;
import org.jcodec.scale.RgbToYuv422p;
import org.jcodec.scale.Transform;
import org.jcodec.scale.Transform.Levels;
import org.jcodec.scale.Transform8Bit;
import org.jcodec.scale.Yuv420pToRgb;
import org.jcodec.scale.Yuv420pToYuv422p;
import org.jcodec.scale.Yuv420pToYuv422p8Bit;
import org.jcodec.scale.Yuv422pToRgb;
import org.jcodec.scale.Yuv422pToYuv420p;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class TranscodeMain {

    private static final String APPLE_PRO_RES_422 = "Apple ProRes 422";

    private static Map<String, Profile> profiles = new HashMap<String, Profile>();

    static {
        profiles.put("avc2png", new Avc2png());
        profiles.put("avc2prores", new Avc2prores());
        profiles.put("jpeg2avc", new Jpeg2avc());
        profiles.put("hls2png", new Hls2png());
        profiles.put("mkv2png", new Mkv2png());
        profiles.put("mpeg2img", new Mpeg2img());
        profiles.put("png2avc", new Png2avc());
        profiles.put("png2mkv", new Png2mkv());
        profiles.put("png2prores", new Png2prores());
        profiles.put("png2vp8", new Png2vp8());
        profiles.put("png2webm", new Png2webm());
        profiles.put("prores2avc", new Prores2avc());
        profiles.put("prores2png", new Prores2png());
        profiles.put("prores2vp8", new Prores2vp8());
        profiles.put("prores2webm", new Prores2webm());
        profiles.put("ts2mp4", new Ts2mp4());
        profiles.put("tsavc2png", new TsAvc2Png());
        profiles.put("y4m2prores", new Y4m2prores());
        profiles.put("webm2png", new Webm2png());
    }

    public static void main(String[] args) throws Exception {
        Cmd cmd = MainUtils.parseArguments(args);
        String profileName = cmd.getArg(0);
        Profile profile = profiles.get(profileName);
        if (profile == null) {
            System.err.println("Profile: " + profileName + " is not supported");
            Set<Entry<String, Profile>> entrySet = profiles.entrySet();
            for (Entry<String, Profile> entry : entrySet) {
                System.err.println(entry.getKey());
                entry.getValue().printHelp(System.err);
            }
            return;
        }
        cmd.popArg();
        profile.transcode(cmd);
    }

    protected static interface Profile {
        void transcode(Cmd cmd) throws IOException;

        void printHelp(PrintStream err);
    }

    protected static class Png2webm implements Profile {
        
        @Override
        public void transcode(Cmd cmd) throws IOException {
            FileChannelWrapper sink = null;
            try {
                sink = NIOUtils.writableChannel(tildeExpand(cmd.getArg(1)));
                VP8Encoder encoder = VP8Encoder.createVP8Encoder(10); // qp
                RgbToYuv420p transform = new RgbToYuv420p(0, 0);

                MKVMuxer muxer = new MKVMuxer();
                MKVMuxerTrack videoTrack = null;

                int i;
                for (i = 0; i < cmd.getIntegerFlagD("maxFrames", Integer.MAX_VALUE); i++) {
                    File nextImg = new File(String.format(cmd.getArg(0), i));
                    if (!nextImg.exists())
                        continue;

                    BufferedImage rgb = ImageIO.read(nextImg);

                    if (videoTrack == null)
                        videoTrack = muxer.createVideoTrack(new Size(rgb.getWidth(), rgb.getHeight()), "V_VP8");

                    Picture yuv = Picture.create(rgb.getWidth(), rgb.getHeight(), ColorSpace.YUV420);
                    transform.transform(AWTUtil.fromBufferedImageRGB(rgb), yuv);
                    ByteBuffer buf = ByteBuffer.allocate(rgb.getWidth() * rgb.getHeight() * 3);

                    ByteBuffer ff = encoder.encodeFrame(yuv, buf);

                    videoTrack.addSampleEntry(ff, i - 1);
                }
                if (i == 1) {
                    System.out.println("Image sequence not found");
                    return;
                }
                muxer.mux(sink);
            } finally {
                IOUtils.closeQuietly(sink);
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
    }

    protected static class Prores2vp8 implements Profile {

        public static final String FLAG_THUMBNAIL = "thumbnail";

        @Override
        public void transcode(Cmd cmd) throws IOException {
            SeekableByteChannel sink = null;
            SeekableByteChannel source = null;
            try {
                sink = writableChannel(tildeExpand(cmd.getArg(1)));
                source = readableChannel(tildeExpand(cmd.getArg(0)));

                MP4Demuxer demux = new MP4Demuxer(source);

                Transform transform = new Yuv422pToYuv420p(0, 0);

                VP8Encoder encoder = VP8Encoder.createVP8Encoder(10); // qp

                IVFMuxer muxer = null;

                AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();

                VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];
                Picture target1 = Picture.create(1920, 1088, ColorSpace.YUV422);
                Picture target2 = null;
                ByteBuffer _out = ByteBuffer.allocate(ine.getWidth() * ine.getHeight() * 6);

                int fps = (int) (inTrack.getFrameCount() / inTrack.getDuration().scalar());

                MP4Packet inFrame;
                int totalFrames = (int) inTrack.getFrameCount();
                long start = System.currentTimeMillis();
                ProresDecoder decoder;
                if (cmd.getBooleanFlagD(FLAG_THUMBNAIL, false)) {
                    decoder = new ProresToThumb2x2();
                } else {
                    decoder = new ProresDecoder();
                }
                for (int i = 0; (inFrame = (MP4Packet) inTrack.nextFrame()) != null; i++) {
                    Picture dec = decoder.decodeFrame(inFrame.getData(), target1.getData());
                    if (target2 == null) {
                        target2 = Picture.create(dec.getWidth(), dec.getHeight(), ColorSpace.YUV420);
                    }
                    transform.transform(dec, target2);
                    _out.clear();
                    ByteBuffer result = encoder.encodeFrame(target2, _out);
                    if (muxer == null)
                        muxer = new IVFMuxer(sink, dec.getWidth(), dec.getHeight(), fps);

                    Packet packet = Packet.createPacket(result, inFrame.getMediaPts(), inFrame.getTimescale(),
                            inFrame.getDuration(), inFrame.getFrameNo(), true, null);

                    muxer.addFrame(packet);
                    if (i % 100 == 0) {
                        long elapse = System.currentTimeMillis() - start;
                        System.out.println((i * 100 / totalFrames) + "%, " + (i * 1000 / elapse) + "fps");
                    }
                }
                muxer.close();
            } finally {
                if (sink != null)
                    sink.close();
                if (source != null)
                    source.close();
            }
        }

        @Override
        public void printHelp(PrintStream err) {
            MainUtils.printHelpVarArgs(new HashMap<String, String>() {
                {
                    put(FLAG_THUMBNAIL, "Use ProRes thumbnail decoder");
                }
            }, "in file", "out file");
        }
    }

    protected static class Prores2webm implements Profile {
        public static final String FLAG_THUMBNAIL = "thumbnail";

        @Override
        public void transcode(Cmd cmd) throws IOException {
            SeekableByteChannel sink = null;
            SeekableByteChannel source = null;
            try {
                sink = writableChannel(tildeExpand(cmd.getArg(1)));
                source = readableChannel(tildeExpand(cmd.getArg(0)));

                MP4Demuxer demux = new MP4Demuxer(source);

                Transform transform = new Yuv422pToYuv420p(0, 0);

                VP8Encoder encoder = VP8Encoder.createVP8Encoder(10); // qp

                MKVMuxer muxer = new MKVMuxer();
                MKVMuxerTrack videoTrack = null;

                AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();

                VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];
                Picture target1 = Picture.create(1920, 1088, ColorSpace.YUV422);
                Picture target2 = null;
                ByteBuffer _out = ByteBuffer.allocate(ine.getWidth() * ine.getHeight() * 6);

                int fps = (int) (inTrack.getFrameCount() / inTrack.getDuration().scalar());

                ProresDecoder decoder;
                if (cmd.getBooleanFlagD(FLAG_THUMBNAIL, false)) {
                    decoder = new ProresToThumb2x2();
                } else {
                    decoder = new ProresDecoder();
                }
                MP4Packet inFrame;
                int totalFrames = (int) inTrack.getFrameCount();
                long start = System.currentTimeMillis();
                for (int i = 0; (inFrame = (MP4Packet) inTrack.nextFrame()) != null; i++) {
                    Picture dec = decoder.decodeFrame(inFrame.getData(), target1.getData());
                    if (target2 == null) {
                        target2 = Picture.create(dec.getWidth(), dec.getHeight(), ColorSpace.YUV420);
                    }
                    transform.transform(dec, target2);
                    _out.clear();

                    ByteBuffer result = encoder.encodeFrame(target2, _out);
                    if (videoTrack == null)
                        videoTrack = muxer.createVideoTrack(new Size(dec.getWidth(), dec.getHeight()), "V_VP8");

                    // Packet packet = new Packet(result, inFrame.getMediaPts(),
                    // inFrame.getTimescale(),
                    // inFrame.getDuration(), inFrame.getFrameNo(), true, null);
                    byte[] array = new byte[result.limit()];
                    System.arraycopy(result.array(), result.position(), array, 0, array.length);
                    videoTrack.addSampleEntry(ByteBuffer.wrap(array), i - 1);

                    if (i % 100 == 0) {
                        long elapse = System.currentTimeMillis() - start;
                        System.out.println((i * 100 / totalFrames) + "%, " + (i * 1000 / elapse) + "fps");
                    }
                }
                muxer.mux(sink);
            } finally {
                if (sink != null)
                    sink.close();
                if (source != null)
                    source.close();
            }

        }

        @Override
        public void printHelp(PrintStream err) {
            MainUtils.printHelpVarArgs(new HashMap<String, String>() {
                {
                    put(FLAG_THUMBNAIL, "Use ProRes thumbnail decoder");
                }
            }, "in file", "out file");
        }
    }

    protected static class Png2vp8 implements Profile {
        @Override
        public void transcode(Cmd cmd) throws IOException {
            FileChannelWrapper sink = null;
            try {
                sink = NIOUtils.writableChannel(tildeExpand(cmd.getArg(1)));
                VP8Encoder encoder = VP8Encoder.createVP8Encoder(10); // qp
                RgbToYuv420p transform = new RgbToYuv420p(0, 0);

                IVFMuxer muxer = null;

                int i;
                for (i = 0; i < cmd.getIntegerFlagD("maxFrames", Integer.MAX_VALUE); i++) {
                    File nextImg = new File(String.format(cmd.getArg(0), i));
                    if (!nextImg.exists())
                        continue;

                    BufferedImage rgb = ImageIO.read(nextImg);
                    Picture yuv = Picture.create(rgb.getWidth(), rgb.getHeight(), ColorSpace.YUV420);
                    transform.transform(AWTUtil.fromBufferedImageRGB(rgb), yuv);
                    ByteBuffer buf = ByteBuffer.allocate(rgb.getWidth() * rgb.getHeight() * 3);

                    ByteBuffer ff = encoder.encodeFrame(yuv, buf);
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
    }

    protected static class Jpeg2avc implements Profile {
        public static final String FLAG_DOWNSCALE = "downscale";

        @Override
        public void transcode(Cmd cmd) throws IOException {
            SeekableByteChannel sink = null;
            SeekableByteChannel source = null;
            try {
                sink = writableChannel(tildeExpand(cmd.getArg(1)));
                source = readableChannel(tildeExpand(cmd.getArg(0)));

                MP4Demuxer demux = new MP4Demuxer(source);
                MP4Muxer muxer = MP4Muxer.createMP4Muxer(sink, Brand.MOV);

                Transform8Bit transform = null;

                H264Encoder encoder = new H264Encoder(new DumbRateControl());

                AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();
                FramesMP4MuxerTrack outTrack = muxer.addTrack(TrackType.VIDEO, (int) inTrack.getTimescale());

                VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];
                Picture8Bit target1 = Picture8Bit.create(1920, 1088, ColorSpace.YUV444);
                Picture8Bit target2 = null;
                ByteBuffer _out = ByteBuffer.allocate(ine.getWidth() * ine.getHeight() * 6);

                Integer downscale = cmd.getIntegerFlag(FLAG_DOWNSCALE);
                JpegDecoder decoder;
                if (downscale == null || downscale == 1) {
                    decoder = new JpegDecoder(false, false);
                } else if (downscale == 2) {
                    decoder = new JpegToThumb4x4(false, false);
                } else if (downscale == 4) {
                    decoder = new JpegToThumb2x2(false, false);
                } else {
                    throw new IllegalArgumentException("Downscale factor of " + downscale
                            + " is not supported ([2,4]).");
                }

                Set<ByteBuffer> spsList = new HashSet<ByteBuffer>();
                Set<ByteBuffer> ppsList = new HashSet<ByteBuffer>();
                Packet inFrame;
                int totalFrames = (int) inTrack.getFrameCount();
                long start = System.currentTimeMillis();
                for (int i = 0; (inFrame = inTrack.nextFrame()) != null && i < 100; i++) {
                    Picture8Bit dec = decoder.decodeFrame8Bit(inFrame.getData(), target1.getData());
                    if (transform == null) {
                        transform = ColorUtil.getTransform8Bit(dec.getColor(), encoder.getSupportedColorSpaces()[0]);
                    }
                    if (target2 == null) {
                        target2 = Picture8Bit.create(dec.getWidth(), dec.getHeight(),
                                encoder.getSupportedColorSpaces()[0]);
                    }
                    transform.transform(dec, target2);
                    _out.clear();
                    ByteBuffer result = encoder.encodeFrame8Bit(target2, _out);
                    H264Utils.wipePSinplace(result, spsList, ppsList);
                    H264Utils.encodeMOVPacket(result);
                    outTrack.addFrame(MP4Packet.createMP4PacketWithData((MP4Packet) inFrame, result));

                    if (i % 100 == 0) {
                        long elapse = System.currentTimeMillis() - start;
                        System.out.println((i * 100 / totalFrames) + "%, " + (i * 1000 / elapse) + "fps");
                    }
                }
                outTrack.addSampleEntry(H264Utils.createMOVSampleEntryFromSpsPpsList(new ArrayList<ByteBuffer>(spsList),
                        new ArrayList<ByteBuffer>(ppsList), 4));

                muxer.writeHeader();
            } finally {
                if (sink != null)
                    sink.close();
                if (source != null)
                    source.close();
            }
        }

        @Override
        public void printHelp(PrintStream err) {
            MainUtils.printHelpVarArgs(new HashMap<String, String>() {
                {
                    put(FLAG_DOWNSCALE, "Downscale factor: [2, 4].");
                }
            }, "in file", "out file");
        }
    }

    protected static class Webm2png implements Profile {
        @Override
        public void transcode(Cmd cmd) throws IOException {
            File file = tildeExpand(cmd.getArg(0));
            if (!file.exists()) {
                System.out.println("Input file doesn't exist");
                return;
            }

            FileInputStream inputStream = new FileInputStream(file);
            try {
                MKVDemuxer demux = MKVDemuxer.getDemuxer(new FileChannelWrapper(inputStream.getChannel()));

                VP8Decoder decoder = new VP8Decoder();
                Transform transform = new Yuv420pToRgb(0, 0);

                DemuxerTrack inTrack = demux.getVideoTrack();

                Picture rgb = Picture.create(demux.getPictureWidth(), demux.getPictureHeight(), ColorSpace.RGB);
                BufferedImage bi = new BufferedImage(demux.getPictureWidth(), demux.getPictureHeight(),
                        BufferedImage.TYPE_3BYTE_BGR);

                Packet inFrame;
                for (int i = 1; (inFrame = inTrack.nextFrame()) != null && i <= 10;) {
                    if (!inFrame.isKeyFrame())
                        continue;

                    try {
                        decoder.decode(inFrame.getData());
                    } catch (AssertionError ae) {
                        ae.printStackTrace(System.err);
                        continue;
                    }
                    Picture pic = decoder.getPicture();
                    if (bi == null)
                        bi = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                    if (rgb == null)
                        rgb = Picture.create(pic.getWidth(), pic.getHeight(), RGB);
                    transform.transform(pic, rgb);
                    AWTUtil.toBufferedImage(rgb, bi);
                    ImageIO.write(bi, "png", new File(format(cmd.getArg(1), i++)));

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
            }, "pattern", "out file");
        }
    }

    protected static class Png2mkv implements Profile {
        @Override
        public void transcode(Cmd cmd) throws IOException {
            FileOutputStream fos = new FileOutputStream(tildeExpand(cmd.getArg(1)));
            FileChannelWrapper sink = null;
            try {
                sink = new FileChannelWrapper(fos.getChannel());
                MKVMuxer muxer = new MKVMuxer();

                H264Encoder encoder = H264Encoder.createH264Encoder();
                RgbToYuv420p transform = new RgbToYuv420p(0, 0);

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

                    Picture yuv = Picture.create(rgb.getWidth(), rgb.getHeight(), ColorSpace.YUV420);
                    transform.transform(AWTUtil.fromBufferedImageRGB(rgb), yuv);
                    ByteBuffer buf = ByteBuffer.allocate(rgb.getWidth() * rgb.getHeight() * 3);

                    ByteBuffer ff = encoder.encodeFrame(yuv, buf);
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
    }

    protected static class Mkv2png implements Profile {
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
                MKVDemuxer demux = MKVDemuxer.getDemuxer(new FileChannelWrapper(inputStream.getChannel()));

                H264Decoder decoder = new H264Decoder();
                Transform transform = new Yuv420pToRgb(0, 0);

                DemuxerTrack inTrack = demux.getVideoTrack();

                Picture rgb = Picture.create(demux.getPictureWidth(), demux.getPictureHeight(), ColorSpace.RGB);
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
                    Frame pic = (Frame) decoder
                            .decodeFrame8BitFromNals(H264Utils.splitMOVPacket(inFrame.getData(), avcC), buf.getData());
                    if (pic.getPOC() == 0) {
                        prevGopsSize += gopSize;
                        gopSize = 1;
                    } else
                        gopSize++;

                    if (bi == null)
                        bi = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                    if (rgb == null)
                        rgb = Picture.create(pic.getWidth(), pic.getHeight(), RGB);
                    transform.transform(pic.toPicture(8), rgb);
                    AWTUtil.toBufferedImage(rgb, bi);
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
    }

    protected static class Avc2prores implements Profile {
        private static final String FLAG_RAW = "raw";
        private static final String FLAG_MAX_FRAMES = "max-frames";
        private static final String FLAG_DUMPMV = "dumpMv";
        private static final String FLAG_DUMPMVJS = "dumpMvJs";

        public void transcode(Cmd cmd) throws IOException {
            SeekableByteChannel sink = null;
            SeekableByteChannel source = null;
            boolean raw = cmd.getBooleanFlagD(FLAG_RAW, false);
            try {
                sink = writableFileChannel(cmd.getArg(1));

                int totalFrames = Integer.MAX_VALUE;
                PixelAspectExt pasp = null;
                DemuxerTrack videoTrack;
                int width = 0, height = 0;
                H264Decoder decoder = null;
                if (!raw) {
                    source = readableFileChannel(cmd.getArg(0));
                    MP4Demuxer demux = new MP4Demuxer(source);
                    AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();
                    VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];

                    totalFrames = (int) inTrack.getFrameCount();
                    pasp = Box.findFirst(inTrack.getSampleEntries()[0], PixelAspectExt.class, "pasp");

                    decoder = H264Decoder.createH264DecoderFromCodecPrivate(inTrack.getMeta().getCodecPrivate());

                    videoTrack = inTrack;

                    width = (ine.getWidth() + 15) & ~0xf;
                    height = (ine.getHeight() + 15) & ~0xf;
                } else {
                    videoTrack = new MappedH264ES(NIOUtils.fetchFromFile(new File(cmd.getArg(0))));
                }
                MP4Muxer muxer = MP4Muxer.createMP4Muxer(sink, Brand.MOV);

                ProresEncoder encoder = new ProresEncoder(ProresEncoder.Profile.HQ, false);

                Transform8Bit transform = new Yuv420pToYuv422p8Bit();
                boolean dumpMv = cmd.getBooleanFlagD(FLAG_DUMPMV, false);
                boolean dumpMvJs = cmd.getBooleanFlagD(FLAG_DUMPMVJS, false);

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
                    dt.gotoFrame(inFrame.getFrameNo());
                }
                long totalH264 = 0, totalProRes = 0;
                int maxFrames = cmd.getIntegerFlagD(FLAG_MAX_FRAMES, Integer.MAX_VALUE);
                for (i = 0; (gopLen + i) < maxFrames && (inFrame = videoTrack.nextFrame()) != null;) {
                    ByteBuffer data = inFrame.getData();
                    Picture8Bit target1;
                    Frame dec;
                    if (!raw) {
                        target1 = Picture8Bit.create(width, height, ColorSpace.YUV420);
                        long start = System.nanoTime();
                        dec = decoder.decodeFrame8BitFromNals(H264Utils.splitFrame(data), target1.getData());
                        totalH264 += (System.nanoTime() - start);
                        if (dumpMv)
                            dumpMv(i, dec);
                        if (dumpMvJs)
                            dumpMvJs(i, dec);
                    } else {
                        SeqParameterSet sps = ((MappedH264ES) videoTrack).getSps()[0];
                        width = (sps.pic_width_in_mbs_minus1 + 1) << 4;
                        height = H264Utils.getPicHeightInMbs(sps) << 4;
                        target1 = Picture8Bit.create(width, height, ColorSpace.YUV420);
                        long start = System.nanoTime();
                        dec = decoder.decodeFrame8Bit(data, target1.getData());
                        totalH264 += (System.nanoTime() - start);
                        if (dumpMv)
                            dumpMv(i, dec);
                        if (dumpMvJs)
                            dumpMvJs(i, dec);
                    }
                    if (outTrack == null) {
                        outTrack = muxer.addVideoTrack("apch", new Size(dec.getCroppedWidth(), dec.getCroppedHeight()),
                                APPLE_PRO_RES_422, timescale);
                        if (pasp != null)
                            outTrack.getEntries().get(0).add(pasp);
                    }
                    if (dec.getPOC() == 0 && gopLen > 0) {
                        totalProRes += outGOP(encoder, transform, timescale, frameDuration, outTrack, gopLen, gop,
                                min(totalFrames, maxFrames), i, width, height);
                        i += gopLen;
                        gopLen = 0;
                    }
                    gop[gopLen++] = dec;
                }
                if (gopLen > 0) {
                    totalProRes += outGOP(encoder, transform, timescale, frameDuration, outTrack, gopLen, gop,
                            min(totalFrames, maxFrames), i, width, height);
                }
                muxer.writeHeader();
                System.out.println(((1000000000L * (i + gopLen)) / totalH264) + "fps (h.264 decoding).");
                System.out.println(((1000000000L * (i + gopLen)) / totalProRes) + "fps (ProRes encoding).");
            } finally {
                if (sink != null)
                    sink.close();
                if (source != null)
                    source.close();
            }
        }

        private void dumpMv(int frameNo, Frame dec) {
            System.err.println("FRAME " + String.format("%08d", frameNo)
                    + " ================================================================");
            if (dec.getFrameType() == SliceType.I)
                return;
            int[][][][] mvs = dec.getMvs();
            for (int i = 0; i < 2; i++) {

                System.err.println((i == 0 ? "BCK" : "FWD")
                        + " ===========================================================================");
                for (int blkY = 0; blkY < mvs[i].length; ++blkY) {
                    StringBuilder line0 = new StringBuilder();
                    StringBuilder line1 = new StringBuilder();
                    StringBuilder line2 = new StringBuilder();
                    StringBuilder line3 = new StringBuilder();
                    line0.append("+");
                    line1.append("|");
                    line2.append("|");
                    line3.append("|");
                    for (int blkX = 0; blkX < mvs[i][0].length; ++blkX) {
                        line0.append("------+");
                        line1.append(String.format("%6d|", mvs[i][blkY][blkX][0]));
                        line2.append(String.format("%6d|", mvs[i][blkY][blkX][1]));
                        line3.append(String.format("    %2d|", mvs[i][blkY][blkX][2]));
                    }
                    System.err.println(line0.toString());
                    System.err.println(line1.toString());
                    System.err.println(line2.toString());
                    System.err.println(line3.toString());
                }
                if (dec.getFrameType() != SliceType.B)
                    break;
            }
        }

        private void dumpMvJs(int frameNo, Frame dec) {
            System.err.println("{frameNo: " + frameNo + ",");
            if (dec.getFrameType() == SliceType.I)
                return;
            int[][][][] mvs = dec.getMvs();
            for (int i = 0; i < 2; i++) {

                System.err.println((i == 0 ? "backRef" : "forwardRef") + ": [");
                for (int blkY = 0; blkY < mvs[i].length; ++blkY) {
                    for (int blkX = 0; blkX < mvs[i][0].length; ++blkX) {
                        System.err.println("{x: " + blkX + ", y: " + blkY + ", mx: " + mvs[i][blkY][blkX][0] + ", my: "
                                + mvs[i][blkY][blkX][1] + ", ridx:" + mvs[i][blkY][blkX][2] + "},");
                    }
                }
                System.err.println("],");
                if (dec.getFrameType() != SliceType.B)
                    break;
            }
            System.err.println("}");
        }

        private static long outGOP(ProresEncoder encoder, Transform8Bit transform, int timescale, int frameDuration,
                FramesMP4MuxerTrack outTrack, int gopLen, Frame[] gop, int totalFrames, int i, int codedWidth,
                int codedHeight) throws IOException {

            long totalTime = 0;
            ByteBuffer _out = ByteBuffer.allocate(codedWidth * codedHeight * 6);
            Picture8Bit target2 = Picture8Bit.create(codedWidth, codedHeight, ColorSpace.YUV422);
            Arrays.sort(gop, 0, gopLen, Frame.POCAsc);
            for (int g = 0; g < gopLen; g++) {
                Frame frame = gop[g];
                transform.transform(frame, target2);
                target2.setCrop(frame.getCrop());
                _out.clear();
                long start = System.nanoTime();
                encoder.encodeFrame8Bit(target2, _out);
                totalTime += System.nanoTime() - start;
                // TODO: Error if chunk has more then one frame
                outTrack.addFrame(MP4Packet.createMP4Packet(_out, i * frameDuration, timescale, frameDuration, i, true,
                        null, 0, i * frameDuration, 0));

                if (i % 100 == 0)
                    System.out.println((i * 100 / totalFrames) + "%");
                i++;
            }
            return totalTime;
        }

        @Override
        public void printHelp(PrintStream err) {
            MainUtils.printHelpVarArgs(new HashMap<String, String>() {
                {
                    put(FLAG_RAW, "Input AnnexB stream (raw h.264 elementary stream)");
                    put(FLAG_DUMPMV, "Dump motion vectors from frames");
                    put(FLAG_DUMPMVJS, "Dump motion vectors from frames in JSon format");
                    put(FLAG_MAX_FRAMES, "Maximum number of frames to ouput");
                }
            }, "in file", "pattern");
        }
    }

    protected static class TsAvc2Png implements Profile {

        @Override
        public void transcode(Cmd cmd) throws IOException {
            SeekableByteChannel bc = null;
            try {
                bc = NIOUtils.readableFileChannel(cmd.getArg(0));
                Set<Integer> programs = MTSDemuxer.getProgramsFromChannel(bc);
                for (int program : programs) {
                    System.out.println("Transcoding program " + String.format("%x", program));
                    bc.setPosition(0);
                    MTSDemuxer mts = new MTSDemuxer(bc, program);
                    // We ignore all audio tracks
                    for (MPEGDemuxerTrack track : mts.getAudioTracks()) {
                        track.ignore();
                    }
                    List<? extends MPEGDemuxerTrack> videoTracks = mts.getVideoTracks();
                    if (videoTracks.size() == 0)
                        continue;
                    MPEGDemuxerTrack videoTrack = videoTracks.remove(0);
                    // We ignore all video tracks but the first
                    for (MPEGDemuxerTrack track : videoTracks) {
                        track.ignore();
                    }
                    // 500k buffer for the raw frame
                    ByteBuffer bb = ByteBuffer.allocate(500 << 10);
                    Picture8Bit tmp = Picture8Bit.create(1920, 1088, ColorSpace.YUV420);
                    VideoDecoder vd = null;

                    Packet packet;
                    int i = 0;
                    while ((packet = videoTrack.nextFrame(bb)) != null) {
                        ByteBuffer data = packet.getData();
                        if (vd == null)
                            vd = JCodecUtil.detectDecoder(data.duplicate());
                        Picture8Bit pic = vd.decodeFrame8Bit(data, tmp.getData());
                        if (pic != null) {
                            AWTUtil.savePicture(pic, "png", tildeExpand(format(cmd.getArg(1), i++)));
                        }
                    }
                }
            } finally {
                NIOUtils.closeQuietly(bc);
            }
        }

        @Override
        public void printHelp(PrintStream err) {
        }
    }

    protected static class Ts2mp4 implements Profile {
        public void transcode(Cmd cmd) throws IOException {
            File fin = new File(cmd.getArg(0));
            SeekableByteChannel sink = null;
            List<SeekableByteChannel> sources = new ArrayList<SeekableByteChannel>();
            try {
                sink = writableFileChannel(cmd.getArg(1));
                MP4Muxer muxer = MP4Muxer.createMP4Muxer(sink, Brand.MP4);

                Set<Integer> programs = MTSDemuxer.getPrograms(fin);
                MPEGDemuxer.MPEGDemuxerTrack[] srcTracks = new MPEGDemuxer.MPEGDemuxerTrack[100];
                FramesMP4MuxerTrack[] dstTracks = new FramesMP4MuxerTrack[100];
                boolean[] h264 = new boolean[100];
                Packet[] top = new Packet[100];
                int nTracks = 0;
                long minPts = Long.MAX_VALUE;
                ByteBuffer[] used = new ByteBuffer[100];
                for (Integer guid : programs) {
                    SeekableByteChannel sx = readableFileChannel(cmd.getArg(0));
                    sources.add(sx);
                    MTSDemuxer demuxer = new MTSDemuxer(sx, guid);
                    for (MPEGDemuxerTrack track : demuxer.getTracks()) {
                        srcTracks[nTracks] = track;
                        DemuxerTrackMeta meta = track.getMeta();

                        top[nTracks] = track.nextFrame(ByteBuffer.allocate(1920 * 1088));
                        dstTracks[nTracks] = muxer.addTrack(meta.getType() == Type.VIDEO ? VIDEO : SOUND, 90000);
                        if (meta.getType() == Type.VIDEO) {
                            h264[nTracks] = true;
                        }
                        used[nTracks] = ByteBuffer.allocate(1920 * 1088);
                        if (top[nTracks].getPts() < minPts)
                            minPts = top[nTracks].getPts();
                        nTracks++;
                    }
                }

                long[] prevDuration = new long[100];
                while (true) {
                    long min = Integer.MAX_VALUE;
                    int mini = -1;
                    for (int i = 0; i < nTracks; i++) {
                        if (top[i] != null && top[i].getPts() < min) {
                            min = top[i].getPts();
                            mini = i;
                        }
                    }
                    if (mini == -1)
                        break;

                    Packet next = srcTracks[mini].nextFrame(used[mini]);
                    if (next != null)
                        prevDuration[mini] = next.getPts() - top[mini].getPts();
                    muxPacket(top[mini], dstTracks[mini], h264[mini], minPts, prevDuration[mini]);
                    used[mini] = top[mini].getData();
                    used[mini].clear();
                    top[mini] = next;
                }

                muxer.writeHeader();

            } finally {
                for (SeekableByteChannel sx : sources) {
                    NIOUtils.closeQuietly(sx);
                }
                NIOUtils.closeQuietly(sink);
            }
        }

        private static void muxPacket(Packet packet, FramesMP4MuxerTrack dstTrack, boolean h264, long minPts,
                long duration) throws IOException {
            if (h264) {
                if (dstTrack.getEntries().size() == 0) {
                    List<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
                    List<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();
                    H264Utils.wipePSinplace(packet.getData(), spsList, ppsList);
                    dstTrack.addSampleEntry(H264Utils.createMOVSampleEntryFromSpsPpsList(spsList, ppsList, 4));
                } else {
                    H264Utils.wipePSinplace(packet.getData(), null, null);
                }
                H264Utils.encodeMOVPacket(packet.getData());
            } else {
                org.jcodec.codecs.aac.ADTSParser.Header header = ADTSParser.read(packet.getData());
                if (dstTrack.getEntries().size() == 0) {

                    AudioSampleEntry ase = AudioSampleEntry.createAudioSampleEntry(Header.createHeader("mp4a", 0),
                            (short) 1, (short) AACConts.AAC_CHANNEL_COUNT[header.getChanConfig()], (short) 16,
                            AACConts.AAC_SAMPLE_RATES[header.getSamplingIndex()], (short) 0, 0, 0, 0, 0, 0, 0, 2,
                            (short) 0);

                    dstTrack.addSampleEntry(ase);
                    ase.add(EsdsBox.fromADTS(header));
                }
            }
            dstTrack.addFrame(MP4Packet.createMP4Packet(packet.getData(), packet.getPts() - minPts,
                    packet.getTimescale(), duration, packet.getFrameNo(), packet.isKeyFrame(), packet.getTapeTimecode(),
                    0, packet.getPts() - minPts, 0));
        }

        @Override
        public void printHelp(PrintStream err) {
            MainUtils.printHelpVarArgs(new HashMap<String, String>() {
                {
                }
            }, "in file", "out file");
        }
    }

    protected static class Hls2png implements Profile {
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
                for (int i = 0; (inFrame = track.nextFrame(buf)) != null; i++) {
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
    }

    protected static class Avc2png implements Profile {
        @Override
        public void transcode(Cmd cmd) throws IOException {
            SeekableByteChannel source = null;
            try {
                source = readableFileChannel(cmd.getArg(0));

                MP4Demuxer demux = new MP4Demuxer(source);

                Transform transform = new Yuv420pToRgb(0, 0);

                AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();

                VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];
                Picture target1 = Picture.create((ine.getWidth() + 15) & ~0xf, (ine.getHeight() + 15) & ~0xf,
                        ColorSpace.YUV420J);
                Picture rgb = Picture.create(ine.getWidth(), ine.getHeight(), ColorSpace.RGB);
                ByteBuffer _out = ByteBuffer.allocate(ine.getWidth() * ine.getHeight() * 6);
                BufferedImage bi = new BufferedImage(ine.getWidth(), ine.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

                H264Decoder decoder = H264Decoder
                        .createH264DecoderFromCodecPrivate(inTrack.getMeta().getCodecPrivate());
                RgbToBgr rgbToBgr = new RgbToBgr();

                Packet inFrame;
                int totalFrames = (int) inTrack.getFrameCount();
                for (int i = 0; (inFrame = inTrack.nextFrame()) != null; i++) {
                    ByteBuffer data = inFrame.getData();

                    Picture dec = decoder.decodeFrameFromNals(H264Utils.splitFrame(data), target1.getData());
                    transform.transform(dec, rgb);
                    rgbToBgr.transform(rgb, rgb);
                    _out.clear();

                    AWTUtil.toBufferedImage(rgb, bi);
                    ImageIO.write(bi, "png", new File(format(cmd.getArg(1), i)));
                    if (i % 100 == 0)
                        System.out.println((i * 100 / totalFrames) + "%");
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
    }

    protected static class Prores2avc implements Profile {
        private static final int DEFAULT_FIXED_BITS_PER_MB = 1024;
        private static final String FLAG_THUMBNAIL = "thumbnail";
        private static final String FLAG_RC = "rc";
        private static final String FLAG_BITS_PER_MB = "bitsPerMb";

        @Override
        public void transcode(Cmd cmd) throws IOException {
            SeekableByteChannel sink = null;
            SeekableByteChannel source = null;
            try {
                sink = writableFileChannel(cmd.getArg(1));
                source = readableFileChannel(cmd.getArg(0));

                MP4Demuxer demux = new MP4Demuxer(source);
                MP4Muxer muxer = MP4Muxer.createMP4Muxer(sink, Brand.MP4);

                Transform transform = new Yuv422pToYuv420p(0, 0);

                String rcName = cmd.getStringFlagD(FLAG_RC, "dumb");
                RateControl rc;
                if ("dumb".equals(rcName)) {
                    rc = new DumbRateControl();
                } else if ("fixed".equals(rcName)) {
                    rc = new H264FixedRateControl(cmd.getIntegerFlagD(FLAG_BITS_PER_MB, DEFAULT_FIXED_BITS_PER_MB));
                } else {
                    System.err.println("Unsupported rate control mode: " + rcName);
                    return;
                }

                H264Encoder encoder = new H264Encoder(rc);
                encoder.setKeyInterval(25);

                AbstractMP4DemuxerTrack inTrack = demux.getVideoTrack();
                FramesMP4MuxerTrack outTrack = muxer.addTrack(TrackType.VIDEO, (int) inTrack.getTimescale());

                VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];
                Picture target1 = Picture.create(1920, 1088, ColorSpace.YUV422);
                Picture target2 = null;
                ByteBuffer _out = ByteBuffer.allocate(ine.getWidth() * ine.getHeight() * 6);

                List<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
                List<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();
                ProresDecoder decoder;
                if (cmd.getBooleanFlagD(FLAG_THUMBNAIL, false)) {
                    decoder = new ProresToThumb2x2();
                } else {
                    decoder = new ProresDecoder();
                }
                Packet inFrame;
                int totalFrames = (int) inTrack.getFrameCount();
                long start = System.currentTimeMillis();
                for (int i = 0; (inFrame = inTrack.nextFrame()) != null; i++) {
                    Picture dec = decoder.decodeFrame(inFrame.getData(), target1.getData());
                    if (target2 == null) {
                        target2 = Picture.createCropped(dec.getWidth(), dec.getHeight(), encoder.getSupportedColorSpaces()[0],
                                dec.getCrop());
                    }
                    transform.transform(dec, target2);
                    _out.clear();
                    ByteBuffer result = encoder.encodeFrame(target2, _out);
                    if (rc instanceof H264FixedRateControl) {
                        int mbWidth = (dec.getWidth() + 15) >> 4;
                        int mbHeight = (dec.getHeight() + 15) >> 4;
                        result.limit(((H264FixedRateControl) rc).calcFrameSize(mbWidth * mbHeight));
                    }
                    H264Utils.wipePSinplace(result, spsList, ppsList);
                    NALUnit nu = NALUnit.read(NIOUtils.from(result.duplicate(), 4));
                    H264Utils.encodeMOVPacket(result);
                    MP4Packet pkt = MP4Packet.createMP4PacketWithData((MP4Packet) inFrame, result);
                    pkt.setKeyFrame(nu.type == NALUnitType.IDR_SLICE);
                    outTrack.addFrame(pkt);
                    if (i % 100 == 0) {
                        long elapse = System.currentTimeMillis() - start;
                        System.out.println((i * 100 / totalFrames) + "%, " + (i * 1000 / elapse) + "fps");
                    }
                }
                outTrack.addSampleEntry(
                        H264Utils.createMOVSampleEntryFromSpsPpsList(spsList.subList(0, 1), ppsList.subList(0, 1), 4));

                muxer.writeHeader();
            } finally {
                if (sink != null)
                    sink.close();
                if (source != null)
                    source.close();
            }
        }

        @Override
        public void printHelp(PrintStream err) {
            MainUtils.printHelpVarArgs(new HashMap<String, String>() {
                {
                    put(FLAG_RC, "Rate control algorythm");
                    put(FLAG_THUMBNAIL, "Use ProRes thumbnail decoder");
                }
            }, "in file", "pattern");
        }
    }

    protected static class Png2avc implements Profile {
        @Override
        public void transcode(Cmd cmd) throws IOException {
            FileChannel sink = null;
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(new File(cmd.getArg(1)));
                sink = fos.getChannel();
                H264Encoder encoder = H264Encoder.createH264Encoder();
                RgbToYuv420p transform = new RgbToYuv420p(0, 0);

                int i;
                for (i = 0; i < cmd.getIntegerFlagD("maxFrames", Integer.MAX_VALUE); i++) {
                    File nextImg = new File(String.format(cmd.getArg(0), i));
                    if (!nextImg.exists())
                        break;
                    BufferedImage rgb = ImageIO.read(nextImg);
                    Picture yuv = Picture.create(rgb.getWidth(), rgb.getHeight(), encoder.getSupportedColorSpaces()[0]);
                    transform.transform(AWTUtil.fromBufferedImageRGB(rgb), yuv);
                    ByteBuffer buf = ByteBuffer.allocate(rgb.getWidth() * rgb.getHeight() * 3);

                    ByteBuffer ff = encoder.encodeFrame(yuv, buf);
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
    }

    protected static class Y4m2prores implements Profile {
        public void transcode(Cmd cmd) throws IOException {
            SeekableByteChannel y4m = readableChannel(tildeExpand(cmd.getArg(0)));

            Y4MDecoder frames = new Y4MDecoder(y4m);

            Picture outPic = Picture.create(frames.getWidth(), frames.getHeight(), ColorSpace.YUV420);

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

                Yuv420pToYuv422p color = new Yuv420pToYuv422p(0, 0);
                FramesMP4MuxerTrack videoTrack = muxer.addVideoTrack("apch", frames.getSize(), APPLE_PRO_RES_422,
                        fps.getNum());
                videoTrack.setTgtChunkDuration(HALF, SEC);
                Picture picture = Picture.create(frames.getSize().getWidth(), frames.getSize().getHeight(),
                        ColorSpace.YUV422);
                Picture frame;
                int i = 0;
                ByteBuffer buf = ByteBuffer.allocate(frames.getSize().getWidth() * frames.getSize().getHeight() * 6);
                while ((frame = frames.nextFrame(outPic.getData())) != null) {
                    color.transform(frame, picture);
                    encoder.encodeFrame(picture, buf);
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
    }

    protected static class Prores2png implements Profile {
        private static final String FLAG_DOWNSCALE = "downscale";

        public void transcode(Cmd cmd) throws IOException {
            File file = tildeExpand(cmd.getArg(0));
            if (!file.exists()) {
                System.out.println("Input file doesn't exist");
                return;
            }

            MP4Demuxer rawDemuxer = new MP4Demuxer(readableChannel(file));
            FramesMP4DemuxerTrack videoTrack = (FramesMP4DemuxerTrack) rawDemuxer.getVideoTrack();
            if (videoTrack == null) {
                System.out.println("Video track not found");
                return;
            }
            Yuv422pToRgb transform = new Yuv422pToRgb(0, 0);

            ProresDecoder decoder;
            Integer downscale = cmd.getIntegerFlag(FLAG_DOWNSCALE);
            if (downscale == null) {
                decoder = new ProresDecoder();
            } else if (2 == downscale) {
                decoder = new ProresToThumb4x4();
            } else if (4 == downscale) {
                decoder = new ProresToThumb2x2();
            } else if (8 == downscale) {
                decoder = new ProresToThumb();
            } else {
                throw new IllegalArgumentException("Unsupported downscale factor: " + downscale + ".");
            }

            BufferedImage bi = null;
            Picture rgb = null;
            int i = 0;
            Packet pkt;
            while ((pkt = videoTrack.nextFrame()) != null) {
                Picture buf = Picture.create(1920, 1088, ColorSpace.YUV420);
                Picture pic = decoder.decodeFrame(pkt.getData(), buf.getData());
                if (bi == null)
                    bi = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                if (rgb == null)
                    rgb = Picture.create(pic.getWidth(), pic.getHeight(), RGB);
                transform.transform(pic, rgb);
                new RgbToBgr().transform(rgb, rgb);
                AWTUtil.toBufferedImage(rgb, bi);
                ImageIO.write(bi, "png", tildeExpand(format(cmd.getArg(1), i++)));
            }
        }

        @Override
        public void printHelp(PrintStream err) {
            MainUtils.printHelpVarArgs(new HashMap<String, String>() {
                {
                    put(FLAG_DOWNSCALE, "Downscale factor, i.e. [2, 4, 8].");
                }
            }, "in file", "pattern");
        }
    }

    protected static class Mpeg2img implements Profile {
        private static final String FLAG_DOWNSCALE = "downscale";
        private static final String FLAG_IMG_FORMAT = "img_format";

        public void transcode(Cmd cmd) throws IOException {
            File file = new File(cmd.getArg(0));
            if (!file.exists()) {
                System.out.println("Input file doesn't exist");
                return;
            }

            Format format = JCodecUtil.detectFormat(file);
            FileChannelWrapper ch = readableChannel(file);
            MPEGDemuxer mpsDemuxer;
            if (format == Format.MPEG_PS) {
                mpsDemuxer = new MPSDemuxer(ch);
            } else if (format == Format.MPEG_TS) {
                Set<Integer> programs = MTSDemuxer.getProgramsFromChannel(ch);
                mpsDemuxer = new MTSDemuxer(ch, programs.iterator().next());
            } else
                throw new RuntimeException("Unsupported mpeg container");
            MPEGDemuxerTrack videoTrack = mpsDemuxer.getVideoTracks().get(0);
            if (videoTrack == null) {
                System.out.println("Video track not found");
                return;
            }

            String imgFormat = cmd.getStringFlagD(FLAG_IMG_FORMAT, "jpeg");
            ByteBuffer buf = ByteBuffer.allocate(1920 * 1080 * 6);
            Packet pkt;
            Picture8Bit pix = Picture8Bit.create(1920, 1088, ColorSpace.YUV444);
            pkt = videoTrack.nextFrame(buf);
            if (pkt == null)
                return;
            VideoDecoder decoder = JCodecUtil.detectDecoder(pkt.getData());
            Integer downscale = cmd.getIntegerFlag(FLAG_DOWNSCALE);
            if (downscale != null) {
                decoder = decoder.downscaled(downscale);
                if (decoder == null) {
                    System.out.println("Could not create decoder for downscale ratio: " + downscale);
                }
            }
            for (int i = 0; pkt != null; i++) {
                // System.out.println(i);
                Picture8Bit pic = decoder.decodeFrame8Bit(pkt.getData(), pix.getData());
                AWTUtil.savePicture(pic, imgFormat, new File(String.format(cmd.getArg(1), i)));
                pkt = videoTrack.nextFrame(buf);
            }
        }

        @Override
        public void printHelp(PrintStream err) {
            MainUtils.printHelpVarArgs(new HashMap<String, String>() {
                {
                    put(FLAG_DOWNSCALE, "Decode downscaled.");
                }
            }, "in file", "pattern");
        }
    }

    protected static class Png2prores implements Profile {
        private static final String DEFAULT_PROFILE = "apch";
        private static final String FLAG_FOURCC = "fourcc";
        private static final String FLAG_INTERLACED = "interlaced";

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
                RgbToYuv422p transform = new RgbToYuv422p(0, 0);

                FramesMP4MuxerTrack videoTrack = null;
                int i;
                for (i = 1;; i++) {
                    File nextImg = new File(String.format(cmd.getArg(0), i));
                    if (!nextImg.exists())
                        break;
                    BufferedImage rgb = ImageIO.read(nextImg);

                    if (videoTrack == null) {
                        videoTrack = muxer.addVideoTrack(profile.fourcc, new Size(rgb.getWidth(), rgb.getHeight()),
                                APPLE_PRO_RES_422, 24000);
                        videoTrack.setTgtChunkDuration(HALF, SEC);
                    }
                    Picture yuv = Picture.create(rgb.getWidth(), rgb.getHeight(), ColorSpace.YUV422);
                    transform.transform(AWTUtil.fromBufferedImageRGB(rgb), yuv);
                    ByteBuffer buf = ByteBuffer.allocate(rgb.getWidth() * rgb.getHeight() * 3);

                    encoder.encodeFrame(yuv, buf);
                    // TODO: Error if chunk has more then one frame
                    videoTrack.addFrame(
                            MP4Packet.createMP4Packet(buf, i * 1001, 24000, 1001, i, true, null, 0, i * 1001, 0));
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
    }
}