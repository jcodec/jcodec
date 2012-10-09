package org.jcodec.samples.streaming;

import static org.apache.commons.lang.StringUtils.join;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioFormat;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.MultiPartOutputStream;
import org.jcodec.common.model.ChannelLabel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.samples.streaming.Adapter.AdapterTrack;
import org.jcodec.samples.streaming.Adapter.AudioAdapterTrack;
import org.jcodec.samples.streaming.Adapter.VideoAdapterTrack;

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
        String[] path = StringUtils.split(req.getPathInfo(), "/");

        File file = null;
        Integer track = null, frame = null;
        for (int i = path.length - 1; i >= 0; i--) {
            File test = new File(base, StringUtils.join(path, "/", 0, i + 1));
            if (test.exists() && !test.isDirectory()) {
                track = i < path.length - 1 ? Integer.parseInt(path[i + 1]) : null;
                frame = i < path.length - 2 ? Integer.parseInt(path[i + 2]) : null;
                file = test;
                break;
            }
        }

        Adapter adapter = getAdapter(file);

        try {
            if (frame != null) {
                frame(adapter, track, frame, resp);
            } else if (track != null) {
                if (req.getParameter("pts") != null)
                    search(adapter, track, Long.parseLong(req.getParameter("pts")), resp);
                else
                    info(adapter, track, resp);
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
            frame(demuxer, trackNo, frameNo, resp);
    }

    protected void frame(Adapter demuxer, int trackNo, int frame, HttpServletResponse resp) throws IOException {
        AdapterTrack track = demuxer.getTrack(trackNo);

        if (track instanceof VideoAdapterTrack) {
            outGOP(resp, ((VideoAdapterTrack) track).getGOP(frame));
        } else {
            outFrame(resp, ((AudioAdapterTrack) track).getFrame(frame));
        }
    }

    private void outFrame(HttpServletResponse resp, Packet packet) throws IOException {
        if (packet == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.addHeader("JCodec-PTS", String.valueOf(packet.getPts()));
        resp.addHeader("JCodec-Duration", String.valueOf(packet.getDuration()));
        resp.addHeader("JCodec-FrameNo", String.valueOf(packet.getFrameNo()));
        resp.setContentType("application/octet-stream");

        packet.getData().writeTo(resp.getOutputStream());
    }

    private void outGOP(HttpServletResponse resp, Packet[] packets) throws IOException {
        if (packets == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        MultiPartOutputStream out = new MultiPartOutputStream(resp.getOutputStream());
        resp.setContentType("multipart/mixed; boundary=" + out.getBoundary());

        for (int i = 0; i < packets.length; i++) {
            Packet packet = packets[i];

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

            packet.getData().writeTo(out);
        }
        out.close();
    }

    private String formatTapeTimecode(TapeTimecode tapeTimecode) {
        return String.format("%02d", tapeTimecode.getHour()) + ":" + String.format("%02d", tapeTimecode.getMinute())
                + ":" + String.format("%02d", tapeTimecode.getSecond()) + ":"
                + String.format("%02d", tapeTimecode.getFrame());
    }

    protected void info(Adapter demuxer, int trackNo, HttpServletResponse resp) throws IOException {
        PrintStream out = new PrintStream(resp.getOutputStream());
        AdapterTrack track = demuxer.getTracks().get(trackNo);
        out.println(mediaInfo2String(track.getMediaInfo()));
    }

    protected void info(Adapter demuxer, HttpServletResponse resp) throws IOException {
        PrintStream out = new PrintStream(resp.getOutputStream());
        out.println(demuxer.getTracks().size());
    }

    protected String mediaInfo2String(MediaInfo info) {
        if (info instanceof MediaInfo.VideoInfo)
            return video((MediaInfo.VideoInfo) info);
        else if (info instanceof MediaInfo.AudioInfo)
            return audio((MediaInfo.AudioInfo) info);
        throw new RuntimeException("Track should be video or audio");
    }

    private String audio(MediaInfo.AudioInfo info) {
        StringBuilder bldr = new StringBuilder();
        bldr.append("audio:");
        bldr.append(media(info));

        AudioFormat format = info.getFormat();

        bldr.append(String.valueOf((int) format.getSampleRate()));
        bldr.append(':');
        bldr.append(String.valueOf(format.getSampleSizeInBits() >> 3));
        bldr.append(':');
        bldr.append(String.valueOf(format.getChannels()));
        bldr.append(':');
        bldr.append(String.valueOf(format.isBigEndian()));
        bldr.append(':');
        bldr.append(info.getFramesPerPacket());
        bldr.append(':');
        bldr.append(labels(info));

        return bldr.toString();
    }

    private String labels(MediaInfo.AudioInfo info) {
        ChannelLabel[] labels = info.getLabels();
        String[] str = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            str[i] = labels[i].toString();
        }

        return join(str, ",");
    }

    private String media(MediaInfo info) {
        return info.getDuration() + ":" + info.getTimescale() + ":" + info.getNFrames() + ":" + info.getFourcc() + ":"
                + (info.getName() == null ? "" : info.getName()) + ":";
    }

    private String video(MediaInfo.VideoInfo v) {
        StringBuilder bldr = new StringBuilder();
        bldr.append("video:");
        bldr.append(media(v));

        bldr.append(v.getDim().getWidth() + "x" + v.getDim().getHeight());
        bldr.append(':');
        bldr.append(v.getPAR().getNum() + "x" + v.getPAR().getDen());
        return bldr.toString();
    }
}