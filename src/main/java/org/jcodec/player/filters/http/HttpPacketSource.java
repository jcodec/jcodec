package org.jcodec.player.filters.http;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.http.impl.client.DefaultHttpClient;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.tools.Debug;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.player.filters.PacketSource;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Packet source that deliveres track data over HTTP using JCodec streaming
 * 
 * @author The JCodec project
 * 
 */
public class HttpPacketSource implements PacketSource {

    protected URL url;
    private int frameNo;
    private FrameCache cache;
    private Prefetcher prefetcher;
    private MediaInfo mi;
    private long miUpdateTime;
    private int fetchSpeed;
    private Downloader downloader;

    public HttpPacketSource(String trackUrl, File trackCache) throws IOException {
        this.downloader = new Downloader(new DefaultHttpClient(), trackUrl);

        updateMediaInfo();

        if (trackCache.exists())
            trackCache.delete();
        trackCache.createNewFile();
        trackCache.deleteOnExit();

        cache = new FrameCache(trackCache);

        int fps = (int) (((long) mi.getTimescale() * mi.getNFrames()) / mi.getDuration());
        fetchSpeed = 3 * fps / 2;

        prefetcher = new Prefetcher(cache, downloader, 15, fetchSpeed);
        prefetcher.start();

        Debug.println("Opened packet source for: " + trackUrl);
    }

    private void updateMediaInfo() throws IOException {
        long now = System.currentTimeMillis();
        if (mi == null || now - miUpdateTime > 1000) {
            mi = downloader.downloadMediaInfo();
            miUpdateTime = now;
        }
    }

    public synchronized Packet getPacket(byte[] buffer) throws IOException {
        Packet pkt = cache.getFrame(frameNo, buffer);
        if (pkt == null) {
            try {
                pkt = downloader.getFrame(frameNo, buffer);
                if (pkt == null)
                    return null;
                cache.addFrame(pkt);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        pkt.setTimescale(mi.getTimescale());

        ++frameNo;

        return pkt;
    }

    private void restartDownloader(int frameNo) {
        prefetcher.cancel();

        prefetcher = new Prefetcher(cache, downloader, frameNo, fetchSpeed);
        prefetcher.start();
    }

    public synchronized void seek(RationalLarge second) throws IOException {
        long pts = second.multiplyS(mi.getTimescale());
        if (cache.pts2frame(pts) == -1) {
            Packet pkt = downloader.seekFrame(pts, null);
            if (pkt == null)
                throw new RuntimeException("Can not seek to pts " + pts);
            cache.addFrame(pkt);
            frameNo = (int) pkt.getFrameNo();
        } else {
            frameNo = cache.pts2frame(pts);
        }

        restartDownloader(frameNo + 15);
    }

    public boolean drySeek(RationalLarge second) throws IOException {
        long pts = second.multiplyS(mi.getTimescale());
        if (cache.pts2frame(pts) == -1) {
            Packet pkt = downloader.seekFrame(pts, null);
            if (pkt == null)
                return false;
            cache.addFrame(pkt);
        }

        return true;
    }

    public MediaInfo getMediaInfo() throws IOException {
        updateMediaInfo();
        return mi;
    }

    @Override
    public void close() throws IOException {
        prefetcher.cancel();
        cache.close();
        downloader.close();
    }

    public int[][] getCached() {
        return cache.getCached();
    }

    @Override
    public void gotoFrame(int frame) {
        if(frameNo == frame)
            return;
        frameNo = frame;
        
        restartDownloader(frameNo + 15);
    }
}