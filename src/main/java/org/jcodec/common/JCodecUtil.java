package org.jcodec.common;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.ppm.PPMEncoder;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform8Bit;

import java.io.File;
import java.io.IOException;
import java.lang.Runnable;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class JCodecUtil {

    private static final VideoDecoder[] knownDecoders = new VideoDecoder[] { new ProresDecoder(), new MPEGDecoder(),
            new H264Decoder() };

    public enum Format {
        MOV, MPEG_PS, MPEG_TS
    }

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

        if (movScore == 0 && psScore == 0 && tsScore == 0)
            return null;

        return movScore > psScore ? (movScore > tsScore ? Format.MOV : Format.MPEG_TS)
                : (psScore > tsScore ? Format.MPEG_PS : Format.MPEG_TS);
    }

    public static VideoDecoder detectDecoder(ByteBuffer b) {
        int maxProbe = 0;
        VideoDecoder selected = null;
        for (VideoDecoder vd : knownDecoders) {
            int probe = vd.probe(b);
            if (probe > maxProbe) {
                selected = vd;
                maxProbe = probe;
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

    public static ThreadPoolExecutor getPriorityExecutor(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<Runnable>(10, PriorityFuture.COMP)) {

            protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                RunnableFuture<T> newTaskFor = super.newTaskFor(callable);
                return new PriorityFuture<T>(newTaskFor, ((PriorityCallable<T>) callable).getPriority());
            }
        };
    }
}