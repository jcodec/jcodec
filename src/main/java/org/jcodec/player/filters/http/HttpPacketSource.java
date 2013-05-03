package org.jcodec.player.filters.http;

import static org.jcodec.player.filters.http.HttpUtils.getHttpClient;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.common.NIOUtils;
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

    public HttpPacketSource(String trackUrl, File trackCache, MediaInfo mediaInfos) throws IOException {
        this.downloader = new Downloader(getHttpClient(trackUrl), trackUrl);

        mi = mediaInfos;

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

    public synchronized Packet getPacket(ByteBuffer buffer) throws IOException {
        Packet pkt = cache.getFrame(frameNo, buffer);
        if (pkt == null) {
            try {
                List<Packet> frames = downloader.getFrames(frameNo, frameNo + 15, null);
                if (frames == null)
                    return null;
                for (Packet packet : frames) {
                    cache.addFrame(packet);
                }
                pkt = copyPkt(buffer, frames.get(0));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        pkt.setTimescale(mi.getTimescale());

        ++frameNo;

        return pkt;
    }

    private Packet copyPkt(ByteBuffer buffer, Packet pkt) {
        ByteBuffer out = buffer.duplicate();
        NIOUtils.write(out, pkt.getData());
        out.flip();
        return new Packet(pkt, out);
    }

    private void restartDownloader(int frameNo) {
        prefetcher.cancel();

        prefetcher = new Prefetcher(cache, downloader, frameNo, fetchSpeed);
        prefetcher.start();
    }

    public synchronized void seek(RationalLarge second) throws IOException {
        long pts = second.multiplyS(mi.getTimescale());
        if (cache.pts2frame(pts) == -1) {
            List<Packet> frames = downloader.seekFrame(pts, null);
            if (frames == null)
                throw new RuntimeException("Can not seek to pts " + pts);
            frameNo = (int) frames.get(0).getFrameNo();
            for (Packet packet : frames) {
                cache.addFrame(packet);
            }
        } else {
            frameNo = cache.pts2frame(pts);
        }
        restartDownloader(frameNo + 15);
    }

    public boolean drySeek(RationalLarge second) throws IOException {
        long pts = second.multiplyS(mi.getTimescale());
        if (cache.pts2frame(pts) == -1) {
            List<Packet> frames = downloader.seekFrame(pts, null);
            if (frames == null)
                return false;
            for (Packet packet : frames) {
                cache.addFrame(packet);
            }
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
        if (frameNo == frame)
            return;
        frameNo = frame;

        restartDownloader(frameNo + 15);
    }
}