package org.jcodec.movtool.streaming.tracks;
import java.lang.IllegalStateException;
import java.lang.System;
import java.lang.IllegalArgumentException;

import org.jcodec.common.CodecMeta;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

import java.io.IOException;
import java.lang.Runnable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    private List<CachingPacket> cachedPackets;
    private ScheduledFuture<?> policyFuture;

    public CachingTrack(VirtualTrack src, final int policy, ScheduledExecutorService policyExecutor) {
        this.cachedPackets = Collections.synchronizedList(new ArrayList<CachingPacket>());

        if (policy < 1)
            throw new IllegalArgumentException("Caching track with less then 1 entry.");
        this.src = src;
        final CachingTrack self = this;
        policyFuture = policyExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                while (self.cachedPackets.size() > policy) {
                    self.cachedPackets.get(0).wipe();
                }
            }
        }, 200, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public CodecMeta getCodecMeta() {
        return src.getCodecMeta();
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        VirtualPacket pkt = src.nextPacket();
        if (pkt == null)
            return null;
        return new CachingPacket(this, pkt);
    }

    public static class CachingPacket extends VirtualPacketWrapper {
        private ByteBuffer cache;
        private CachingTrack track;

        public CachingPacket(CachingTrack track, VirtualPacket src) {
            super(src);
            this.track = track;
        }

        public synchronized void wipe() {
            if (track.cachedPackets.indexOf(this) == 0) {
                track.cachedPackets.remove(0);
                cache = null;
            }
        }

        public synchronized ByteBuffer getData() throws IOException {
            // This packet will receive new place _in the queue
            track.cachedPackets.remove(this);
            if (cache == null) {
                cache = src.getData();
            }
            track.cachedPackets.add(this);

            return cache == null ? null : cache.duplicate();
        }
    }

    @Override
    public void close() throws IOException {
        if (policyFuture != null)
            policyFuture.cancel(false);
        cachedPackets.clear();
        src.close();
    }

    @Override
    public VirtualEdit[] getEdits() {
        return src.getEdits();
    }

    @Override
    public int getPreferredTimescale() {
        return src.getPreferredTimescale();
    }
}