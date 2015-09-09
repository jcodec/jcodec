package org.jcodec.common;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.ppm.PPMEncoder;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;
import org.jcodec.containers.flv.FLVDemuxer;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer;
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

    public static Map<Format, Class<?>> formatMapping = new HashMap<Format, Class<?>>() {
        {
            put(Format.MOV, MP4Demuxer.class);
            put(Format.MPEG_PS, MPSDemuxer.class);
            put(Format.MPEG_TS, MTSDemuxer.class);
            put(Format.FLV, FLVDemuxer.class);
        }
    };

    public static Map<Codec, Class<? extends VideoDecoder>> codecMapping = new HashMap<Codec, Class<? extends VideoDecoder>>() {
        {
            put(Codec.PRORES, ProresDecoder.class);
            put(Codec.MPEG2, MPEGDecoder.class);
            put(Codec.H264, H264Decoder.class);
        }
    };

    public static Format detectFormat(File f) throws IOException {
        return detectFormat(NIOUtils.fetchFrom(f, 200 * 1024));
    }

    public static Format detectFormat(ReadableByteChannel f) throws IOException {
        return detectFormat(NIOUtils.fetchFrom(f, 200 * 1024));
    }

    public static Format detectFormat(ByteBuffer b) {
        int max = 0;
        Format format = null;
        for (Entry<Format, Class<?>> entry : formatMapping.entrySet()) {
            try {
                int score = (Integer) entry.getValue().getMethod("probe", ByteBuffer.class)
                        .invoke(entry.getValue(), b.duplicate());
                if (score > max && score > 0) {
                    max = score;
                    format = entry.getKey();
                }
            } catch (NoSuchMethodException e) {
            } catch (SecurityException e) {
            } catch (IllegalAccessException e) {
            } catch (IllegalArgumentException e) {
            } catch (InvocationTargetException e) {
            }
        }
        return format;
    }

    public static Codec detectDecoder(ByteBuffer b) {
        int maxProbe = 0;
        Codec selected = null;
        for (Entry<Codec, Class<? extends VideoDecoder>> entry : codecMapping.entrySet()) {
            VideoDecoder decoder = instantiateCodec(entry);
            int probe = decoder.probe(b);
            if (probe > maxProbe) {
                selected = entry.getKey();
                maxProbe = probe;
            }
        }
        return selected;
    }

    private static VideoDecoder instantiateCodec(Entry<Codec, Class<? extends VideoDecoder>> entry) {
        try {
            return entry.getValue().newInstance();
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }
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
}