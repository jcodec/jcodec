package org.jcodec.movtool.streaming.tracks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.NIOUtils;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Edits PCM sample track placing chunks of PCM data in place creating and
 * edit-less track
 * 
 * @author The JCodec project
 * 
 */
public class EditedPCMTrack implements VirtualTrack {

    private VirtualTrack src;
    private List<VirtualPacket>[] buckets;
    private VirtualEdit[] edits;
    private int curEdit;
    private int curPkt;
    private int frameNo;
    private double pts;
    private int frameSize;
    private float sampleRate;

    public EditedPCMTrack(VirtualTrack src) throws IOException {
        this.src = src;

        edits = src.getEdits();
        buckets = new List[edits.length];
        for (int i = 0; i < edits.length; i++)
            buckets[i] = new ArrayList<VirtualPacket>();

        VirtualPacket pkt;
        while ((pkt = src.nextPacket()) != null) {
            for (int e = 0; e < edits.length; e++) {
                VirtualEdit ed = edits[e];
                if (pkt.getPts() < ed.getIn() + ed.getDuration() && pkt.getPts() + pkt.getDuration() > ed.getIn()) {
                    buckets[e].add(pkt);
                }
            }
        }

        AudioSampleEntry ase = (AudioSampleEntry) src.getSampleEntry();
        frameSize = ase.calcFrameSize();
        sampleRate = ase.getSampleRate();

    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        if (curEdit >= edits.length)
            return null;
        VirtualPacket pkt = buckets[curEdit].get(curPkt);
        VirtualEdit edt = edits[curEdit];

        double start = Math.max(pkt.getPts(), edt.getIn());
        double end = Math.min(pkt.getPts() + pkt.getDuration(), edt.getIn() + edt.getDuration());
        double duration = end - start;
        double lead = start - pkt.getPts();

        VirtualPacket ret = new EditedPCMPacket(pkt, (int) (Math.round(lead * sampleRate) * frameSize),
                (int) (Math.round(duration * sampleRate) * frameSize), pts, duration, frameNo);

        ++curPkt;
        if (curPkt >= buckets[curEdit].size()) {
            curEdit++;
            curPkt = 0;
        }

        frameNo++;
        pts += duration;

        return ret;
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

    public static class EditedPCMPacket implements VirtualPacket {

        private VirtualPacket src;
        private int inBytes;
        private int dataLen;
        private double pts;
        private double duration;
        private int frameNo;

        public EditedPCMPacket(VirtualPacket src, int inBytes, int dataLen, double pts, double duration, int frameNo) {
            this.src = src;
            this.inBytes = inBytes;
            this.dataLen = dataLen;
            this.pts = pts;
            this.duration = duration;
            this.frameNo = frameNo;
        }

        @Override
        public ByteBuffer getData() throws IOException {
            ByteBuffer data = src.getData();
            NIOUtils.skip(data, inBytes);
            return NIOUtils.read(data, dataLen);
        }

        @Override
        public int getDataLen() {
            return dataLen;
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
    public int getPreferredTimescale() {
        return src.getPreferredTimescale();
    }
}