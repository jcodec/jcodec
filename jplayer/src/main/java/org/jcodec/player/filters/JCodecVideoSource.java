package org.jcodec.player.filters;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

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

    private ThreadLocal<VideoDecoder> decoders = new ThreadLocal<VideoDecoder>();
    private ExecutorService tp;

    private List<ByteBuffer> drain = new ArrayList<ByteBuffer>();
    private MediaInfo.VideoInfo mi;
    private PacketSource src;

    private ReentrantLock seekLock = new ReentrantLock();

    private TreeSet<Packet> reordering;

    public JCodecVideoSource(PacketSource src) throws IOException {
        Debug.println("Creating video source");
        this.src = src;
        mi = (MediaInfo.VideoInfo) src.getMediaInfo();

        reordering = createReordering();

        tp = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            }
        });
    }

    private TreeSet<Packet> createReordering() {
        return new TreeSet<Packet>(new Comparator<Packet>() {
            public int compare(Packet o1, Packet o2) {
                return o1.getPts() > o2.getPts() ? 1 : (o1.getPts() == o2.getPts() ? 0 : -1);
            }
        });
    }

    static int cnt = 0;

    @Override
    public Frame decode(byte[][] buf) throws IOException {
        seekLock.lock();
        Packet nextPacket;
        try {
            nextPacket = selectNextPacket();
        } finally {
            seekLock.unlock();
        }

        if (nextPacket == null)
            return null;

        final Future<Picture> job = tp.submit(new FrameCallable(nextPacket, buf));

        Frame frm = new FutureFrame(job, new RationalLarge(nextPacket.getPts(), nextPacket.getTimescale()),
                new RationalLarge(nextPacket.getDuration(), nextPacket.getTimescale()), mi.getPAR(),
                (int) nextPacket.getFrameNo(), nextPacket.getTapeTimecode(), null);

        return frm;
    }

    public Packet selectNextPacket() throws IOException {
        fillReordering();
        if (reordering.size() == 0)
            return null;
        Packet first = reordering.first();
        if (first != null)
            reordering.remove(first);
        return first;
    }

    private void fillReordering() throws IOException {
        while (reordering.size() < 5) {
            Packet pkt = pickNextPacket();
            if (pkt == null)
                break;
            reordering.add(pkt);
        }
    }

    public Packet pickNextPacket() throws IOException {
        ByteBuffer buffer;
        synchronized (drain) {
            if (drain.size() == 0) {
                drain.add(allocateBuffer());
            }
            buffer = drain.remove(0);
            buffer.rewind();
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
        private byte[][] out;

        public FrameCallable(Packet pkt, byte[][] out) {
            this.pkt = pkt;
            this.out = out;
        }

        public Picture call() {
            VideoDecoder decoder = decoders.get();
            if (decoder == null) {
                decoder = JCodecUtil.getVideoDecoder(mi.getFourcc());
                decoders.set(decoder);
            }

            Picture pic = decoder.decodeFrame(pkt.getData(), out);
            synchronized (drain) {
                drain.add(pkt.getData());
            }
            return pic;
        }
    }

    @Override
    public boolean drySeek(RationalLarge sec) throws IOException {
        return src.drySeek(sec);
    }

    @Override
    public void seek(RationalLarge sec) throws IOException {
        seekLock.lock();
        try {
            clearReordering();
            src.seek(sec);
            fillReordering();
            Iterator<Packet> it = reordering.iterator();
            while (it.hasNext()) {
                if (it.next().getPtsR().lessThen(sec))
                    it.remove();
                else
                    break;
            }
        } finally {
            seekLock.unlock();
        }
    }

    private void clearReordering() {
        TreeSet<Packet> old = reordering;
        reordering = createReordering();
        for (Packet packet : old) {
            drain.add(packet.getData());
        }
    }

    private ByteBuffer allocateBuffer() {
        Size dim = mi.getDim();
        return ByteBuffer.allocate(dim.getWidth() * dim.getHeight() * 2);
    }

    public MediaInfo.VideoInfo getMediaInfo() throws IOException {
        return (MediaInfo.VideoInfo) src.getMediaInfo();
    }

    public void close() throws IOException {
        src.close();
    }

    @Override
    public void gotoFrame(int frame) {
        clearReordering();
        src.gotoFrame(frame);
    }
}