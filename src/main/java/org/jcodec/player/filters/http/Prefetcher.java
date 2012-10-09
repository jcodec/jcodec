package org.jcodec.player.filters.http;

import java.io.IOException;

import org.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Media downloader
 * 
 * @author The JCodec project
 * 
 */
public class Prefetcher extends Thread {
    private int lastFrame;
    private FrameCache cache;
    private volatile boolean cancelled;

    private int pktCounter;
    private long intervalStart;
    private int fetchSpeed;
    private Downloader downloader;

    public Prefetcher(FrameCache cache, Downloader downloader, int frame, int fetchSpeed) {
        this.cache = cache;
        this.downloader = downloader;
        this.lastFrame = frame;
        this.fetchSpeed = fetchSpeed;
    }

    private void oneCycle() throws IOException {
        try {
            if (cache.hasFrame(lastFrame + 1)) {
                lastFrame++;
                return;
            }
            Packet packet = downloader.getFrame(lastFrame + 1, null);

            if (packet == null) {
                cancelled = true;
                return;
            }

            cache.addFrame(packet);

            lastFrame = (int) packet.getFrameNo();

            if (++pktCounter >= 100) {
                Thread.sleep(Math.max((1000 * pktCounter) / fetchSpeed - (System.currentTimeMillis() - intervalStart),
                        0));
                intervalStart = System.currentTimeMillis();
                pktCounter = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            cancelled = true;
        }
    }

    public void run() {
        try {
            while (!cancelled) {
                oneCycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        cancelled = true;
    }
}