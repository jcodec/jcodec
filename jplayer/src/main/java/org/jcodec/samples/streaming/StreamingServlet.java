package org.jcodec.samples.streaming;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.MultiPartOutputStream;
import org.jcodec.common.StringUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.player.filters.MediaInfo.VideoInfo;
import org.jcodec.samples.streaming.Adapter.AdapterTrack;
import org.jcodec.samples.streaming.Adapter.AudioAdapterTrack;
import org.jcodec.samples.streaming.Adapter.VideoAdapterTrack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Streaming component compatible with JCodec streaming protocol
 * 
 * @author The JCodec project
 * 
 */
public class StreamingServlet extends HttpServlet {

    private File base;
    private Map<File, Adapter> map = new HashMap<File, Adapter>();

    public StreamingServlet(File file) {
        this.base = file;
    }

    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.splitS(req.getPathInfo(), "/");

        File file = null;
        Integer track = null, frameS = null, frameE = null;
        for (int i = path.length - 1; i >= 0; i--) {
            File test = new File(base, StringUtils.joinS4(path, "/", 0, i + 1));
            if (test.exists() && !test.isDirectory()) {
                track = i < path.length - 1 ? Integer.parseInt(path[i + 1]) : null;
                if (i < path.length - 2) {
                    String[] split = StringUtils.splitC(path[i + 2], ':');
                    if (split.length == 1) {
                        frameS = frameE = Integer.parseInt(split[0]);
                    } else {
                        frameS = Integer.parseInt(split[0]);
                        frameE = Integer.parseInt(split[1]);
                    }
                }
                file = test;
                break;
            }
        }

        Adapter adapter = getAdapter(file);

        try {
            if (frameS != null && track != null && frameE != null) {
                frame(adapter, track, frameS, frameE, resp);
            } else if (track != null) {
                if (req.getParameter("pts") != null)
                    search(adapter, track, Long.parseLong(req.getParameter("pts")), resp);
                else
                    info(adapter, resp, track);
            } else {
                info(adapter, resp);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } catch (Throwable e2) {
            e2.printStackTrace();
        }
    }

    protected AdapterFactory factory = new AdapterFactory();

    protected Adapter getAdapter(File mvFile) throws IOException {
        Adapter adapter = map.get(mvFile);
        if (adapter == null) {
            adapter = factory.createAdaptor(mvFile);
            map.put(mvFile, adapter);
        }
        return adapter;
    }

    protected void search(Adapter demuxer, int trackNo, long pts, HttpServletResponse resp) throws IOException {
        AdapterTrack track = demuxer.getTrack(trackNo);
        int frameNo = track.search(pts);
        if (frameNo == -1)
            resp.sendError(404);
        else
            frame(demuxer, trackNo, frameNo, frameNo, resp);
    }

    protected void frame(Adapter demuxer, int trackNo, int frameS, int frameE, HttpServletResponse resp)
            throws IOException {
        AdapterTrack track = demuxer.getTrack(trackNo);

        if (track instanceof VideoAdapterTrack) {
            outGOPs(resp, (VideoAdapterTrack) track, frameS, frameE);
        } else {
            outFrames(resp, (AudioAdapterTrack) track, frameS, frameE);
        }
    }

    private void outFrames(HttpServletResponse resp, AudioAdapterTrack track, int frameS, int frameE)
            throws IOException {
        Packet packet = ((AudioAdapterTrack) track).getFrame(frameS++);
        if (packet == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        MultiPartOutputStream out = new MultiPartOutputStream(resp.getOutputStream());
        resp.setContentType("multipart/mixed; boundary=" + out.getBoundary());

        outFrame(out, packet);
        while (frameS < frameE) {
            packet = ((AudioAdapterTrack) track).getFrame(frameS++);
            if (packet == null)
                break;
            outFrame(out, packet);
        }

        out.close();
    }

    private void outFrame(MultiPartOutputStream out, Packet packet) throws IOException {
        List<String> headers = new ArrayList<String>();
        headers.add("JCodec-PTS: " + packet.getPts());
        headers.add("JCodec-Duration: " + packet.getDuration());
        headers.add("JCodec-FrameNo: " + packet.getFrameNo());
        headers.add(String.format("Content-Disposition: attachment; filename=frame%08d.raw", packet.getFrameNo()));

        out.startPart("application/octet-stream", headers.toArray(new String[0]));

        Channels.newChannel(out).write(packet.getData());
    }

    private void outGOPs(HttpServletResponse resp, VideoAdapterTrack track, int frameS, int frameE) throws IOException {
        Packet[] gop = ((VideoAdapterTrack) track).getGOP(frameS);
        if (gop == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        MultiPartOutputStream out = new MultiPartOutputStream(resp.getOutputStream());
        resp.setContentType("multipart/mixed; boundary=" + out.getBoundary());

        outGOP(gop, out);
        frameS = nextGop(track, frameS);

        while (frameS != -1 && frameS < frameE) {
            gop = ((VideoAdapterTrack) track).getGOP(frameS);
            outGOP(gop, out);
            frameS = nextGop(track, frameS);
        }

        out.close();
    }

    private int nextGop(VideoAdapterTrack track, int frameS) {
        int curGop = track.gopId(frameS);
        if (curGop == -1)
            return -1;
        while (curGop == track.gopId(frameS))
            frameS++;
        return track.gopId(frameS) == -1 ? -1 : frameS;
    }

    private void outGOP(Packet[] gop, MultiPartOutputStream out) throws IOException {
        for (int i = 0; i < gop.length; i++) {
            Packet packet = gop[i];

            List<String> headers = new ArrayList<String>();
            headers.add("JCodec-PTS: " + packet.getPts());
            headers.add("JCodec-Duration: " + packet.getDuration());
            headers.add("JCodec-FrameNo: " + packet.getFrameNo());
            headers.add("JCodec-Key: " + packet.isKeyFrame());
            headers.add("JCodec-DisplayOrder: " + packet.getDisplayOrder());
            if (packet.getTapeTimecode() != null)
                headers.add("JCodec-TapeTimecode: " + formatTapeTimecode(packet.getTapeTimecode()));
            headers.add(String.format("Content-Disposition: attachment; filename=frame%08d.mpg", packet.getFrameNo()));

            out.startPart("application/octet-stream", headers.toArray(new String[0]));

            Channels.newChannel(out).write(packet.getData());
        }
    }

    private String formatTapeTimecode(TapeTimecode tapeTimecode) {
        return String.format("%02d", tapeTimecode.getHour()) + ":" + String.format("%02d", tapeTimecode.getMinute())
                + ":" + String.format("%02d", tapeTimecode.getSecond()) + (tapeTimecode.isDropFrame() ? ";" : ":")
                + String.format("%02d", tapeTimecode.getFrame());
    }

    protected void info(Adapter demuxer, HttpServletResponse resp) throws IOException {
        PrintStream out = new PrintStream(resp.getOutputStream());
        out.println("[");
        List<AdapterTrack> tracks = demuxer.getTracks();
        for (int i = 0; i < tracks.size(); i++) {
            MediaInfo mediaInfo = tracks.get(i).getMediaInfo();
            trackInfo(out, mediaInfo, i);
            out.print(",");
        }
        out.println("]");
    }

    private void trackInfo(PrintStream out, MediaInfo mediaInfo, int ind) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        out.println("{");
        out.println("\"id\": \"" + ind + "\",");
        out.println("\"type\": \"" + (mediaInfo instanceof VideoInfo ? "video" : "audio") + "\",");
        out.print("\"info\":");
        out.println(gson.toJson(mediaInfo));
        out.println("}");
    }

    protected void info(Adapter adapter, HttpServletResponse resp, int trackNo) throws IOException {
        trackInfo(new PrintStream(resp.getOutputStream()), adapter.getTrack(trackNo).getMediaInfo(), trackNo);
    }
}