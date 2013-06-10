package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A proxy track that maintains in-memory cache of recently read packets
 * 
 * @author The JCodec project
 * 
 */
public class CachingTrack implements VirtualTrack {
    private VirtualTrack src;
    private List<CachingPacket> cachedPackets = Collections.synchronizedList(new ArrayList<CachingPacket>());

    public CachingTrack(VirtualTrack src, final int policy, ScheduledExecutorService policyExecutor) {
        if (policy < 1)
            throw new IllegalArgumentException("Caching track with less then 1 entry.");
        this.src = src;
        policyExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                while (cachedPackets.size() > policy) {
                    cachedPackets.get(0).wipe();
                }
            }
        }, 200, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public SampleEntry getSampleEntry() {
        return src.getSampleEntry();
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        VirtualPacket pkt = src.nextPacket();
        if (pkt == null)
            return null;
        return new CachingPacket(pkt);
    }

    public class CachingPacket extends VirtualPacketWrapper {
        private ByteBuffer cache;

        public CachingPacket(VirtualPacket src) {
            super(src);
        }

        public synchronized void wipe() {
            if (cachedPackets.indexOf(this) == 0) {
                cachedPackets.remove(0);
                cache = null;
            }
        }

        public synchronized ByteBuffer getData() throws IOException {
            // This packet will receive new place in the queue
            cachedPackets.remove(this);
            if (cache == null) {
                cache = src.getData();
            }
            cachedPackets.add(this);

            return cache.duplicate();
        }
    }

    @Override
    public void close() {
        src.close();
    }

    @Override
    public VirtualEdit[] getEdits() {
        return src.getEdits();
    }
}