package org.jcodec.player.filters.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.TapeTimecode;

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
    private URL url;
    private int lastFrame;
    private FrameCache cache;
    private volatile boolean cancelled;

    private int pktCounter;
    private long intervalStart;
    private int fetchSpeed;

    public Prefetcher(FrameCache cache, URL url, int frame, int fetchSpeed) {
        this.cache = cache;
        this.url = url;
        this.lastFrame = frame;
        this.fetchSpeed = fetchSpeed;
    }

    private void oneCycle() throws IOException {
        try {
            if (cache.hasFrame(lastFrame + 1)) {
                lastFrame++;
                return;
            }
            Packet packet = getFrame(url, lastFrame + 1);

            // System.out.println("Adding frame [" + url + "]:" +
            // packet.getFrameNo());
            cache.addFrame(packet);

            lastFrame = (int) packet.getFrameNo();

            if (++pktCounter >= 100) {
                Thread.sleep(Math.max((1000 * pktCounter) / fetchSpeed - (System.currentTimeMillis() - intervalStart),
                        0));
                intervalStart = System.currentTimeMillis();
                pktCounter = 0;
            }
        } catch (FileNotFoundException e) {
            cancelled = true;
        } catch (Exception e) {
            e.printStackTrace();
            cancelled = true;
        }
    }

    public static Packet getFrame(URL url, int frameNo, byte[] buffer) throws IOException {
        return readFrame((HttpURLConnection) new URL(url + "/" + frameNo).openConnection(), buffer);
    }

    public static Packet getFrame(URL url, int frameNo) throws IOException {
        return readFrame((HttpURLConnection) new URL(url + "/" + frameNo).openConnection(), null);
    }

    public static int seekFrame(URL url, long pts) throws IOException {

        HttpURLConnection con = (HttpURLConnection) new URL(url + "?pts=" + pts).openConnection();
        if (con.getResponseCode() != 200)
            return -1;
        return Integer.parseInt(IOUtils.toString(con.getInputStream()));
    }

    private static TapeTimecode parseTimecode(String timecodeRaw) {
        if (StringUtils.isEmpty(timecodeRaw))
            return null;
        String[] split = StringUtils.split(timecodeRaw, ":");

        return new TapeTimecode(Short.parseShort(split[0]), Byte.parseByte(split[1]), Byte.parseByte(split[2]),
                Byte.parseByte(split[3]), false);
    }

    private static Packet readFrame(HttpURLConnection c, byte[] bfr) throws IOException {
        if (c.getResponseCode() != 200)
            return null;

        Buffer buffer;
        if (bfr == null)
            buffer = new Buffer(IOUtils.toByteArray(c.getInputStream()));
        else {
            Buffer tmp = new Buffer(bfr);
            IOUtils.copy(c.getInputStream(), tmp.os());
            buffer = new Buffer(bfr, 0, tmp.pos);
        }

        String contentType = c.getHeaderField("Content-Type");

        if (contentType.startsWith("multipart/mixed")) {
            byte[] sep1 = ("\r\n--" + contentType.split(";")[1].split("=")[1]).getBytes();

            List<Packet> result = new ArrayList<Packet>();
            int to;
            do {
                buffer.read(buffer.search(13, 10) + 2);
                to = buffer.search(sep1);
                if (to != -1)
                    result.add(processOne(buffer.read(to)));
            } while (to != -1);

            return result.get(0);
        } else {
            long pts = longOr0(c.getHeaderField("JCodec-PTS"));
            long duration = longOr0(c.getHeaderField("JCodec-Duration"));
            int frameNo = intOr0(c.getHeaderField("JCodec-FrameNo"));
            boolean key = boolOrFalse(c.getHeaderField("JCodec-Key"));
            TapeTimecode timecode = parseTimecode(c.getHeaderField("JCodec-TapeTimecode"));

            return new Packet(buffer, pts, 0, duration, frameNo, key, timecode);
        }
    }

    private static Packet processOne(Buffer read) {
        int data = read.search(13, 10, 13, 10);
        Map<String, String> headers = parseHeaders(read.read(data));
        read.read(4);

        long pts = longOr0(headers.get("JCodec-PTS"));
        long duration = longOr0(headers.get("JCodec-Duration"));
        int frameNo = intOr0(headers.get("JCodec-FrameNo"));
        boolean key = boolOrFalse(headers.get("JCodec-Key"));
        TapeTimecode timecode = parseTimecode(headers.get("JCodec-TapeTimecode"));

        return new Packet(read, pts, 0, duration, frameNo, key, timecode);
    }

    private static List<String> getLines(Buffer buffer) {
        ArrayList<String> lines = new ArrayList<String>();
        int next = buffer.search(13, 10);
        while (next != -1) {
            lines.add(new String(buffer.read(next).toArray()));
            buffer.read(2);
            next = buffer.search(13, 10);
        }

        if (buffer.remaining() > 0)
            lines.add(new String(buffer.toArray()));

        return lines;
    }

    private static Map<String, String> parseHeaders(Buffer read) {
        HashMap<String, String> result = new HashMap<String, String>();
        for (String line : getLines(read)) {
            String[] split = line.split(": ");
            result.put(split[0], split[1]);
        }
        return result;
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

    private static long longOr0(String val) {
        return val == null ? 0 : Long.parseLong(val);
    }

    private static int intOr0(String val) {
        return val == null ? 0 : Integer.parseInt(val);
    }

    private static boolean boolOrFalse(String val) {
        return val == null ? false : Boolean.parseBoolean(val);
    }

    public void cancel() {
        cancelled = true;
    }
}