package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Edits frames track the way the resulting track is completely edit-less
 * 
 * @author The JCodec project
 * 
 */
public class EditedFramesTrack implements VirtualTrack {
    private VirtualTrack src;
    private List<VirtualPacket>[] buckets;
    private VirtualEdit[] edits;
    private int curEdit;
    private int curPkt;
    private int frameNo;
    private double pts;

    public EditedFramesTrack(VirtualTrack src) throws IOException {
        this.src = src;

        edits = src.getEdits();
        buckets = new List[edits.length];
        for (int i = 0; i < edits.length; i++)
            buckets[i] = new ArrayList<VirtualPacket>();

        VirtualPacket pkt;
        while ((pkt = src.nextPacket()) != null) {
            if (!pkt.isKeyframe())
                throw new IllegalArgumentException(
                        "Can not apply edits to a track that has inter frames, this will result in decoding errors.");
            for (int e = 0; e < edits.length; e++) {
                VirtualEdit ed = edits[e];
                if (pkt.getPts() < ed.getIn() + ed.getDuration() && pkt.getPts() + pkt.getDuration() > ed.getIn()) {
                    buckets[e].add(pkt);
                }
            }
        }
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        if (curEdit >= edits.length)
            return null;
        VirtualPacket pkt = buckets[curEdit].get(curPkt);
        VirtualEdit edt = edits[curEdit];

        double duration = pkt.getDuration();
        double sticksFront = edt.getIn() - pkt.getPts();
        double sticksBack = pkt.getPts() + pkt.getDuration() - (edt.getIn() + edt.getDuration());
        duration -= Math.max(sticksFront, 0) + Math.max(sticksBack, 0);
        // System.out.println(pkt.getDuration() + " --- " + duration);

        VirtualPacket ret = new EditedFramesPacket(pkt, pts, duration, frameNo);

        ++curPkt;
        if (curPkt >= buckets[curEdit].size()) {
            curEdit++;
            curPkt = 0;
        }

        frameNo++;
        pts += duration;

        return ret;
    }

    public static class EditedFramesPacket extends VirtualPacketWrapper {

        private double pts;
        private double duration;
        private int frameNo;

        public EditedFramesPacket(VirtualPacket src, double pts, double duration, int frameNo) {
            super(src);
            this.pts = pts;
            this.duration = duration;
            this.frameNo = frameNo;
        }

        @Override
        public double getPts() {
            return pts;
        }

        @Override
        public double getDuration() {
            return duration;
        }

        @Override
        public boolean isKeyframe() {
            return true;
        }

        @Override
        public int getFrameNo() {
            return frameNo;
        }
    }

    @Override
    public SampleEntry getSampleEntry() {
        return src.getSampleEntry();
    }

    @Override
    public VirtualEdit[] getEdits() {
        return null;
    }

    @Override
    public void close() {
        src.close();
    }

    @Override
    public int getPreferredTimescale() {
        return src.getPreferredTimescale();
    }
}