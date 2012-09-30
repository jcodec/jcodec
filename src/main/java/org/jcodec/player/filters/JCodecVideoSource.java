package org.jcodec.player.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.jcodec.common.JCodecUtil;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.model.Frame;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.common.tools.Debug;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class JCodecVideoSource implements VideoSource {

    private VideoDecoder decoder;
    private ExecutorService tp;

    private List<byte[]> drain = new ArrayList<byte[]>();
    private MediaInfo.VideoInfo mi;
    private PacketSource src;

    public JCodecVideoSource(PacketSource src) throws IOException {
        Debug.println("Creating video source");
        this.src = src;
        mi = (MediaInfo.VideoInfo) src.getMediaInfo();
        decoder = JCodecUtil.getVideoDecoder(mi.getFourcc());

        tp = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            }
        });
    }

    @Override
    public Frame decode(int[][] buf) throws IOException {
        Packet nextPacket = pickNextPacket();

        if (nextPacket == null)
            return null;

        final Future<Picture> job = tp.submit(new FrameCallable(nextPacket, buf));

        Frame frm = new FutureFrame(job, new RationalLarge(nextPacket.getPts(), nextPacket.getTimescale()),
                new RationalLarge(nextPacket.getDuration(), nextPacket.getTimescale()), mi.getPAR(),
                (int)nextPacket.getFrameNo(), nextPacket.getTapeTimecode(), null);

        return frm;
    }

    public Packet pickNextPacket() throws IOException {
        byte[] buffer;
        synchronized (drain) {
            if (drain.size() == 0) {
                drain.add(allocateBuffer());
            }
            buffer = drain.remove(0);
        }

        return src.getPacket(buffer);
    }

    public class FutureFrame extends Frame {

        private Future<Picture> job;

        public FutureFrame(Future<Picture> job, RationalLarge pts, RationalLarge duration, Rational pixelAspect,
                int frameNo, TapeTimecode tapeTimecode, List<String> messages) {
            super(null, pts, duration, pixelAspect, frameNo, tapeTimecode, messages);
            this.job = job;
        }

        @Override
        public boolean isAvailable() {
            return job.isDone();
        }

        @Override
        public Picture getPic() {
            try {
                return job.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    class FrameCallable implements Callable<Picture> {
        private Packet pkt;
        private int[][] out;

        public FrameCallable(Packet pkt, int[][] out) {
            this.pkt = pkt;
            this.out = out;
        }

        public Picture call() {
            Picture pic = decoder.decodeFrame(pkt.getData(), out);
            synchronized (drain) {
                drain.add(pkt.getData().buffer);
            }
            return pic;
        }
    }

    @Override
    public boolean seek(long clock, long timescale) throws IOException {
        if (!src.seek(mi.getTimescale() * clock / timescale))
            return false;
        pickNextPacket();
        return true;
    }

    private byte[] allocateBuffer() {
        Size dim = mi.getDim();
        return new byte[dim.getWidth() * dim.getHeight() * 2];
    }

    public MediaInfo.VideoInfo getMediaInfo() {
        return (MediaInfo.VideoInfo) src.getMediaInfo();
    }

    @Override
    public boolean canPlayThrough(Rational sec) {
        return src instanceof CachingPacketSource ? ((CachingPacketSource) src).canPlayThrough(sec) : true;
    }

    public void close() throws IOException {
        src.close();
    }
}