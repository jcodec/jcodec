package org.jcodec.player.filters.http;

import static org.apache.commons.lang.StringUtils.trim;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
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
    private URL trackUrl;
    private MediaInfo mi;
    private int fetchSpeed;

    public HttpPacketSource(URL trackUrl, File trackCache) throws IOException {

        this.trackUrl = trackUrl;

        mi = downloadMediaInfo(trackUrl);
        if (trackCache.exists())
            trackCache.delete();
        trackCache.createNewFile();
        trackCache.deleteOnExit();

        cache = new FrameCache(trackCache);

        int fps = (int) (((long) mi.getTimescale() * mi.getNFrames()) / mi.getDuration());
        fetchSpeed = 3 * fps / 2;

        prefetcher = new Prefetcher(cache, trackUrl, 15, fetchSpeed);
        prefetcher.start();

        Debug.println("Opened packet source for: " + trackUrl);
    }

    private MediaInfo downloadMediaInfo(URL trackUrl) throws IOException {
        URLConnection con = trackUrl.openConnection();
        String data = IOUtils.toString(con.getInputStream());
        return parseHeader(StringUtils.split(data, ':'));
    }

    private MediaInfo parseHeader(String[] params) throws IOException {
        for (int i = 0; i < params.length; i++)
            params[i] = trim(params[i]);

        if (params[0].equals("video")) {
            return new MediaInfo.VideoInfo(params[4], Integer.parseInt(params[2]), Long.parseLong(params[1]),
                    Long.parseLong(params[3]), parOr1x1(params[6]), dimOrNull(params[5]));
        } else {
            return new MediaInfo.AudioInfo(params[4], Integer.parseInt(params[2]), Long.parseLong(params[1]),
                    Long.parseLong(params[3]), new AudioFormat(Integer.parseInt(params[5]),
                            Integer.parseInt(params[6]) << 3, Integer.parseInt(params[7]), true,
                            Boolean.parseBoolean(params[8])), Integer.parseInt(params[9]));
        }
    }

    private Size dimOrNull(String value) {
        String[] split = StringUtils.split(value, "x");

        return new Size(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }

    private Rational parOr1x1(String value) {
        String[] split = StringUtils.split(value, "x");
        return new Rational(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }

    public Packet getPacket(byte[] buffer) throws IOException {
        Packet pkt = cache.getFrame(frameNo, buffer);
        if (pkt == null) {
            try {
                pkt = Prefetcher.getFrame(trackUrl, frameNo, buffer);
                cache.addFrame(pkt);
            } catch (FileNotFoundException e) {
                return null;
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

        prefetcher = new Prefetcher(cache, trackUrl, frameNo, fetchSpeed);
        prefetcher.start();
    }

    public boolean seek(long pts) {
        if (cache.pts2frame(pts) == -1) {
            try {
                int frame = Prefetcher.seekFrame(trackUrl, pts);
                if(frame == -1)
                    return false;
                frameNo = frame;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            frameNo = cache.pts2frame(pts);
        }

        restartDownloader(frameNo + 15);

        return true;
    }

    public MediaInfo getMediaInfo() {
        try {
            mi = downloadMediaInfo(trackUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mi;
    }

    @Override
    public void close() throws IOException {
        prefetcher.cancel();
        cache.close();
    }

    public List<int[]> getCached(int threshold) {
        List<int[]> list = new ArrayList<int[]>();
        int[] frames = cache.getFrames();
        Arrays.sort(frames);
        int start = frames[0], i;
        for (i = 1; i < frames.length; i++) {
            if (frames[i] - frames[i - 1] > 1) {
                if (frames[i - 1] - start > threshold)
                    list.add(new int[] { start, frames[i - 1] });
                start = frames[i];
            }
        }

        if (start != frames[i - 1])
            list.add(new int[] { start, frames[i - 1] });

        return list;
    }
}