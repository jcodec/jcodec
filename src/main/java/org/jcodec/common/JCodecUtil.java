package org.jcodec.common;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jcodec.codecs.aac.AACDecoder;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.mjpeg.JpegDecoder;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.ppm.PPMEncoder;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.vp8.VP8Decoder;
import org.jcodec.codecs.y4m.Y4MDecoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.tools.MathUtil;
import org.jcodec.containers.imgseq.ImageSequenceDemuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class JCodecUtil {

    private static final Map<Codec, Class<? extends VideoDecoder>> knownDecoders = new HashMap<Codec, Class<? extends VideoDecoder>>();

    static {
    	knownDecoders.put(Codec.VP8, VP8Decoder.class);
        knownDecoders.put(Codec.PRORES, ProresDecoder.class);
        knownDecoders.put(Codec.MPEG2, MPEGDecoder.class);
        knownDecoders.put(Codec.H264, H264Decoder.class);
    };

    public static Format detectFormat(File f) throws IOException {
        return detectFormatBuffer(NIOUtils.fetchFromFileL(f, 200 * 1024));
    }

    public static Format detectFormatChannel(ReadableByteChannel f) throws IOException {
        return detectFormatBuffer(NIOUtils.fetchFromChannel(f, 200 * 1024));
    }

    public static Format detectFormatBuffer(ByteBuffer b) {
        int movScore = MP4Demuxer.probe(b.duplicate());
        int psScore = MPSDemuxer.probe(b.duplicate());
        int tsScore = MTSDemuxer.probe(b.duplicate());

        if (movScore < 20 && psScore < 20 && tsScore < 20)
            return null;

        return movScore > psScore ? (movScore > tsScore ? Format.MOV : Format.MPEG_TS)
                : (psScore > tsScore ? Format.MPEG_PS : Format.MPEG_TS);
    }

    public static Codec detectDecoder(ByteBuffer b) {
        int maxProbe = 0;
        Codec selected = null;
        for (Map.Entry<Codec, Class<? extends VideoDecoder>> vd : knownDecoders.entrySet()) {
            try {
                Method method = vd.getValue().getDeclaredMethod("probe", ByteBuffer.class);
                if (method != null) {
                    int probe;
                    probe = (Integer) method.invoke(null, b);
                    if (probe > maxProbe) {
                        selected = vd.getKey();
                        maxProbe = probe;
                    }
                }
            } catch (Exception e) {
            }
        }
        return selected;
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

    public static void savePictureAsPPM(Picture8Bit pic, File file) throws IOException {
        Transform8Bit transform = ColorUtil.getTransform8Bit(pic.getColor(), ColorSpace.RGB);
        Picture8Bit rgb = Picture8Bit.create(pic.getWidth(), pic.getHeight(), ColorSpace.RGB);
        transform.transform(pic, rgb);
        NIOUtils.writeTo(new PPMEncoder().encodeFrame8Bit(rgb), file);
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

    public static ThreadPoolExecutor getPriorityExecutor(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<Runnable>(10, PriorityFuture.COMP)) {

            protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                RunnableFuture<T> newTaskFor = super.newTaskFor(callable);
                return new PriorityFuture<T>(newTaskFor, ((PriorityCallable<T>) callable).getPriority());
            }
        };
    }

    public static String removeExtension(String name) {
        if (name == null)
            return null;
        return name.replaceAll("\\.[^\\.]+$", "");
    }

    public static Demuxer createDemuxer(Format format, File input) throws IOException {
        FileChannelWrapper ch = null;
        if (format != Format.IMG) {
            ch = NIOUtils.readableChannel(input);
        }
        switch (format) {
        case MOV:
            return new MP4Demuxer(ch);
        case MPEG_TS:
            return createM2TSDemuxer(ch);
        case MPEG_PS:
            return new MPSDemuxer(ch);
        case MKV:
            return new MKVDemuxer(ch);
        case IMG:
            return new ImageSequenceDemuxer(input);
        case Y4M:
            return new Y4MDecoder(ch);
        default:
            Logger.error("Format " + format + " is not supported");
        }
        return null;
    }

    private static Demuxer createM2TSDemuxer(SeekableByteChannel input) throws IOException {
        Set<Integer> programs = MTSDemuxer.getProgramsFromChannel(input);
        if(programs.size() == 0) {
            Logger.error("The MPEG TS stream contains no programs");
            return null;
        }
        int programId = programs.iterator().next();
        Logger.info("Using M2TS program: " + programId);
        input.setPosition(0);
        return new MTSDemuxer(input, programId);
    }

    public static AudioDecoder createAudioDecoder(Codec codec, ByteBuffer decoderSpecific) throws Exception {
        switch (codec) {
        case AAC:
            return new AACDecoder(decoderSpecific);
        default:
            Logger.error("Codec " + codec + " is not supported");
        }
        return null;
    }
    
    public static VideoDecoder createVideoDecoder(Codec codec, ByteBuffer decoderSpecific) {
        switch (codec) {
        case H264:
            return H264Decoder.createH264DecoderFromCodecPrivate(decoderSpecific);
        case MPEG2:
            return new MPEGDecoder();
        case VP8:
            return new VP8Decoder();
        case JPEG:
            return new JpegDecoder();
        default:
            Logger.error("Codec " + codec + " is not supported");
        }
        return null;
    }
}