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
 * PCM track that outputs packets of exactly set size in PCM frames
 * 
 * @author The JCodec project
 * 
 */
public class PCMFlatternTrack implements VirtualTrack {

    private static final VirtualPacket[] EMPTY = new VirtualPacket[0];
    private int framesPerPkt;
    private VirtualTrack src;
    private AudioSampleEntry se;
    private int dataLen;
    private double packetDur;
    private VirtualPacket leftover;
    private int leftoverOffset;
    private int frameNo;
    private List<VirtualPacket> pktBuffer = new ArrayList<VirtualPacket>();

    public PCMFlatternTrack(VirtualTrack src, int samplesPerPkt) {
        this.framesPerPkt = samplesPerPkt;
        this.src = src;
        this.se = (AudioSampleEntry) src.getSampleEntry();
        this.dataLen = se.calcFrameSize() * framesPerPkt;
        this.packetDur = (double) framesPerPkt / se.getSampleRate();
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        pktBuffer.clear();

        VirtualPacket pkt = leftover == null ? src.nextPacket() : leftover;
        if (pkt == null)
            return null;
        int rem = dataLen + leftoverOffset;
        do {
            pktBuffer.add(pkt);
            rem -= pkt.getDataLen();
            if (rem > 0)
                pkt = src.nextPacket();
        } while (rem > 0 && pkt != null);

        FlatternPacket result = new FlatternPacket(frameNo, pktBuffer.toArray(EMPTY), leftoverOffset, dataLen
                - Math.max(rem, 0));
        frameNo += framesPerPkt;

        if (rem < 0) {
            leftover = pktBuffer.get(pktBuffer.size() - 1);
            leftoverOffset = leftover.getDataLen() + rem;
        } else {
            leftover = null;
            leftoverOffset = 0;
        }

        return result;
    }

    @Override
    public SampleEntry getSampleEntry() {
        return se;
    }

    @Override
    public void close() {
        src.close();
    }

    private class FlatternPacket implements VirtualPacket {
        private int frameNo;
        private int leading;
        private VirtualPacket[] pN;
        private int dataLen;

        public FlatternPacket(int frameNo, VirtualPacket[] pN, int lead, int dataLen) {
            this.frameNo = frameNo;
            this.leading = lead;
            this.pN = pN;
            this.dataLen = dataLen;
        }

        @Override
        public ByteBuffer getData() throws IOException {
            ByteBuffer result = ByteBuffer.allocate(dataLen);

            ByteBuffer d0 = pN[0].getData();
            NIOUtils.skip(d0, leading);
            NIOUtils.write(result, d0);
            for (int i = 1; i < pN.length && result.hasRemaining(); i++) {
                ByteBuffer dN = pN[i].getData();
                int toWrite = Math.min(dN.remaining(), result.remaining());
                NIOUtils.write(result, dN, toWrite);
            }
            result.flip();
            return result;
        }

        @Override
        public int getDataLen() {
            return dataLen;
        }

        @Override
        public double getPts() {
            return ((double) frameNo * framesPerPkt) / se.getSampleRate();
        }

        @Override
        public double getDuration() {
            return packetDur;
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
}