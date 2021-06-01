package org.jcodec.common;

import static org.jcodec.common.Codec.AAC;
import static org.jcodec.common.Codec.JPEG;
import static org.jcodec.common.Codec.MPEG2;
import static org.jcodec.common.Codec.VP8;
import static org.jcodec.common.Format.DASH;
import static org.jcodec.common.Format.DASHURL;
import static org.jcodec.common.Format.IMG;
import static org.jcodec.common.Format.MKV;
import static org.jcodec.common.Format.MOV;
import static org.jcodec.common.Format.MPEG_AUDIO;
import static org.jcodec.common.Format.MPEG_PS;
import static org.jcodec.common.Format.WAV;
import static org.jcodec.common.Format.WEBP;
import static org.jcodec.common.Format.Y4M;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jcodec.codecs.aac.AACDecoder;
import org.jcodec.codecs.h264.BufferH264ES;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.mjpeg.JpegDecoder;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.mpeg4.MPEG4Decoder;
import org.jcodec.codecs.ppm.PPMEncoder;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.vpx.VP8Decoder;
import org.jcodec.codecs.wav.WavDemuxer;
import org.jcodec.common.Tuple._2;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;
import org.jcodec.containers.imgseq.ImageSequenceDemuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mp3.MPEGAudioDemuxer;
import org.jcodec.containers.mp4.demuxer.DashMP4Demuxer;
import org.jcodec.containers.mp4.demuxer.DashMP4Demuxer.Builder;
import org.jcodec.containers.mp4.demuxer.DashStreamDemuxer;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.containers.webp.WebpDemuxer;
import org.jcodec.containers.y4m.Y4MDemuxer;
import org.jcodec.platform.Platform;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class JCodecUtil {

    private static final Map<Codec, Class<?>> decoders = new HashMap<Codec, Class<?>>();
    private static final Map<Format, Class<?>> demuxers = new HashMap<Format, Class<?>>();

    static {
        decoders.put(VP8, VP8Decoder.class);
        decoders.put(Codec.PRORES, ProresDecoder.class);
        decoders.put(MPEG2, MPEGDecoder.class);
        decoders.put(Codec.H264, H264Decoder.class);
        decoders.put(AAC, AACDecoder.class);
        decoders.put(Codec.MPEG4, MPEG4Decoder.class);

        demuxers.put(Format.MPEG_TS, MTSDemuxer.class);
        demuxers.put(MPEG_PS, MPSDemuxer.class);
        demuxers.put(MOV, MP4Demuxer.class);
        demuxers.put(WEBP, WebpDemuxer.class);
        demuxers.put(MPEG_AUDIO, MPEGAudioDemuxer.class);
        demuxers.put(MKV, MKVDemuxer.class);
    };

    public static Format detectFormat(File f) throws IOException {
        return detectFormatBuffer(NIOUtils.fetchFromFileL(f, 200 * 1024));
    }

    public static Format detectFormatChannel(ReadableByteChannel f) throws IOException {
        return detectFormatBuffer(NIOUtils.fetchFromChannel(f, 200 * 1024));
    }

    public static Format detectFormatBuffer(ByteBuffer b) {
        int maxScore = 0;
        Format selected = null;
        for (Map.Entry<Format, Class<?>> vd : demuxers.entrySet()) {
            int score = probe(b.duplicate(), vd.getValue());
            if (score > maxScore) {
                selected = vd.getKey();
                maxScore = score;
            }
        }
        return selected;
    }

    public static Codec detectDecoder(ByteBuffer b) {
        int maxScore = 0;
        Codec selected = null;
        for (Map.Entry<Codec, Class<?>> vd : decoders.entrySet()) {
            int score = probe(b.duplicate(), vd.getValue());
            if (score > maxScore) {
                selected = vd.getKey();
                maxScore = score;
            }
        }
        return selected;
    }

    private static int probe(ByteBuffer b, Class<?> vd) {
        try {
            return Platform.invokeStaticMethod(vd, "probe", new Object[] { b });
        } catch (Exception e) {
        }
        return 0;
    }

    public static VideoDecoder getVideoDecoder(String fourcc) {
        if ("apch".equals(fourcc) || "apcs".equals(fourcc) || "apco".equals(fourcc) || "apcn".equals(fourcc)
                || "ap4h".equals(fourcc))
            return new ProresDecoder();
        else if ("m2v1".equals(fourcc))
            return new MPEGDecoder();
        else
            return null;
    }

    public static void savePictureAsPPM(Picture pic, File file) throws IOException {
        Transform transform = ColorUtil.getTransform(pic.getColor(), ColorSpace.RGB);
        Picture rgb = Picture.create(pic.getWidth(), pic.getHeight(), ColorSpace.RGB);
        transform.transform(pic, rgb);
        NIOUtils.writeTo(new PPMEncoder().encodeFrame(rgb), file);
    }

    public static byte[] asciiString(String fourcc) {
        char[] ch = fourcc.toCharArray();
        byte[] result = new byte[ch.length];
        for (int i = 0; i < ch.length; i++) {
            result[i] = (byte) ch[i];
        }
        return result;
    }

    public static void writeBER32(ByteBuffer buffer, int value) {
        buffer.put((byte) ((value >> 21) | 0x80));
        buffer.put((byte) ((value >> 14) | 0x80));
        buffer.put((byte) ((value >> 7) | 0x80));
        buffer.put((byte) (value & 0x7F));
    }

    public static void writeBER32Var(ByteBuffer bb, int value) {
        for (int i = 0, bits = MathUtil.log2(value); i < 4 && bits > 0; i++) {
            bits -= 7;
            int out = value >> bits;
            if (bits > 0)
                out |= 0x80;
            bb.put((byte) out);
        }
    }

    public static int readBER32(ByteBuffer input) {
        int size = 0;
        for (int i = 0; i < 4; i++) {
            byte b = input.get();
            size = (size << 7) | (b & 0x7f);
            if (((b & 0xff) >> 7) == 0)
                break;
        }
        return size;
    }

    public static int[] getAsIntArray(ByteBuffer yuv, int size) {
        byte[] b = new byte[size];
        int[] result = new int[size];
        yuv.get(b);
        for (int i = 0; i < b.length; i++) {
            result[i] = b[i] & 0xff;
        }
        return result;
    }

    public static String removeExtension(String name) {
        if (name == null)
            return null;
        return name.replaceAll("\\.[^\\.]+$", "");
    }

    public static Demuxer createDemuxer(Format format, String input) throws IOException {
        FileChannelWrapper ch = null;
        if (format.isContained()) {
            ch = NIOUtils.readableFileChannel(input);
        }
        if (MOV == format) {
            return MP4Demuxer.createMP4Demuxer(ch);
        } else if (MPEG_PS == format) {
            return new MPSDemuxer(ch);
        } else if (MKV == format) {
            return new MKVDemuxer(ch);
        } else if (IMG == format) {
            return new ImageSequenceDemuxer(input, Integer.MAX_VALUE);
        } else if (Y4M == format) {
            return new Y4MDemuxer(ch);
        } else if (WEBP == format) {
            return new WebpDemuxer(ch);
        } else if (Format.H264 == format) {
            return new BufferH264ES(NIOUtils.fetchAllFromChannel(ch));
        } else if (WAV == format) {
            return new WavDemuxer(ch);
        } else if (MPEG_AUDIO == format) {
            return new MPEGAudioDemuxer(ch);
        } else if (DASH == format) {
            Builder builder = DashMP4Demuxer.builder();
            String[] split = input.split(":");
            for (String string : split) {
                builder.addTrack().addPattern(string).done();
            }
            return builder.build();
        } else if (DASHURL == format) {
            return new DashStreamDemuxer(new URL(input));
        } else {
            Logger.error("Format " + format + " is not supported");
        }
        return null;
    }

    public static _2<Integer, Demuxer> createM2TSDemuxer(File input, TrackType targetTrack) throws IOException {
        FileChannelWrapper ch = NIOUtils.readableChannel(input);
        MTSDemuxer mts = new MTSDemuxer(ch);
        Set<Integer> programs = mts.getPrograms();
        if (programs.size() == 0) {
            Logger.error("The MPEG TS stream contains no programs");
            return null;
        }
        Tuple._2<Integer, Demuxer> found = null;
        for (Integer pid : programs) {
            ReadableByteChannel program = mts.getProgram(pid);
            if (found != null) {
                program.close();
                continue;
            }
            MPSDemuxer demuxer = new MPSDemuxer(program);
            if (targetTrack == TrackType.AUDIO && demuxer.getAudioTracks().size() > 0
                    || targetTrack == TrackType.VIDEO && demuxer.getVideoTracks().size() > 0) {
                found = Tuple.pair(pid, (Demuxer) demuxer);
                Logger.info("Using M2TS program: " + pid + " for " + targetTrack + " track.");
            } else {
                program.close();
            }
        }
        return found;
    }

    public static AudioDecoder createAudioDecoder(Codec codec, ByteBuffer decoderSpecific) throws IOException {
        if (AAC == codec) {
            return new AACDecoder(decoderSpecific);
        } else {
            Logger.error("Codec " + codec + " is not supported");
        }
        return null;
    }

    public static VideoDecoder createVideoDecoder(Codec codec, ByteBuffer decoderSpecific) {
        if (Codec.H264 == codec) {
            return decoderSpecific != null ? H264Decoder.createH264DecoderFromCodecPrivate(decoderSpecific)
                    : new H264Decoder();
        } else if (MPEG2 == codec) {
            return new MPEGDecoder();
        } else if (VP8 == codec) {
            return new VP8Decoder();
        } else if (JPEG == codec) {
            return new JpegDecoder();
        } else {
            Logger.error("Codec " + codec + " is not supported");
        }
        return null;
    }

}