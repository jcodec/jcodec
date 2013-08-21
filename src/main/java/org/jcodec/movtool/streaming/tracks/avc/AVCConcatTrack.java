package org.jcodec.movtool.streaming.tracks.avc;

import static org.jcodec.codecs.h264.H264Utils.writePPS;
import static org.jcodec.codecs.h264.H264Utils.writeSPS;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.H264Utils.SliceHeaderTweaker;
import org.jcodec.codecs.h264.decode.SliceHeaderReader;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.io.BitReader;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Concats AVC tracks, special logic to merge codec private is applied
 * 
 * TODO: Check SPS/PPS for similarity TODO: Support multiple SPS/PPS per piece
 * 
 * @author The JCodec project
 * 
 */
public class AVCConcatTrack implements VirtualTrack {

    private VirtualTrack[] tracks;
    private int idx = 0;
    private VirtualPacket lastPacket;
    private double offsetPts = 0;
    private int offsetFn = 0;
    private SampleEntry se;
    private AvcCBox[] avcCs;
    private Map<Integer, Integer> map;
    private List<PictureParameterSet> allPps;
    private List<SeqParameterSet> allSps;
    private SliceHeaderTweaker[] tweakers;

    public AVCConcatTrack(VirtualTrack[] tracks) {
        this.tracks = tracks;

        avcCs = new AvcCBox[tracks.length];
        PixelAspectExt pasp = null;

        allPps = new ArrayList<PictureParameterSet>();
        allSps = new ArrayList<SeqParameterSet>();
        tweakers = new H264Utils.SliceHeaderTweaker[tracks.length];
        for (int i = 0; i < tracks.length; i++) {
            SampleEntry se = tracks[i].getSampleEntry();
            if (!(se instanceof VideoSampleEntry))
                throw new RuntimeException("Not a video track.");
            if (!"avc1".equals(se.getFourcc()))
                throw new RuntimeException("Not an AVC track.");

            PixelAspectExt paspL = Box.findFirst(se, PixelAspectExt.class, "pasp");
            if (pasp != null && paspL != null && !pasp.getRational().equals(paspL.getRational()))
                throw new RuntimeException("Can not concat video tracks with different Pixel Aspect Ratio.");
            pasp = paspL;

            AvcCBox avcC = H264Utils.parseAVCC((VideoSampleEntry) se);
            for (ByteBuffer ppsBuffer : avcC.getPpsList()) {
                PictureParameterSet pps = H264Utils.readPPS(NIOUtils.duplicate(ppsBuffer));
                pps.pic_parameter_set_id |= i << 8;
                pps.seq_parameter_set_id |= i << 8;
                allPps.add(pps);
            }
            for (ByteBuffer spsBuffer : avcC.getSpsList()) {
                SeqParameterSet sps = H264Utils.readSPS(NIOUtils.duplicate(spsBuffer));
                sps.seq_parameter_set_id |= i << 8;
                allSps.add(sps);
            }
            final int idx2 = i;
            tweakers[i] = new H264Utils.SliceHeaderTweaker(avcC.getSpsList(), avcC.getPpsList()) {
                protected void tweak(SliceHeader sh) {
                    sh.pic_parameter_set_id = map.get((idx2 << 8) | sh.pic_parameter_set_id);
                }
            };
            avcCs[i] = avcC;
        }
        map = mergePS(allSps, allPps);

        se = H264Utils.createMOVSampleEntry(writeSPS(allSps), writePPS(allPps));
        if (pasp != null)
            se.add(pasp);
    }

    private Map<Integer, Integer> mergePS(List<SeqParameterSet> allSps, List<PictureParameterSet> allPps) {
        List<ByteBuffer> spsRef = new ArrayList<ByteBuffer>();
        for (SeqParameterSet sps : allSps) {
            int spsId = sps.seq_parameter_set_id;
            sps.seq_parameter_set_id = 0;
            ByteBuffer serial = H264Utils.writeSPS(sps, 32);
            int idx = NIOUtils.find(spsRef, serial);
            if (idx == -1) {
                idx = spsRef.size();
                spsRef.add(serial);
            }
            for (PictureParameterSet pps : allPps) {
                if (pps.seq_parameter_set_id == spsId)
                    pps.seq_parameter_set_id = idx;
            }
        }
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        List<ByteBuffer> ppsRef = new ArrayList<ByteBuffer>();
        for (PictureParameterSet pps : allPps) {
            int ppsId = pps.pic_parameter_set_id;
            pps.pic_parameter_set_id = 0;
            ByteBuffer serial = H264Utils.writePPS(pps, 128);
            int idx = NIOUtils.find(ppsRef, serial);
            if (idx == -1) {
                idx = ppsRef.size();
                ppsRef.add(serial);
            }
            map.put(ppsId, idx);
        }
        allSps.clear();
        for (int i = 0; i < spsRef.size(); i++) {
            SeqParameterSet sps = H264Utils.readSPS(spsRef.get(i));
            sps.seq_parameter_set_id = i;
            allSps.add(sps);
        }

        allPps.clear();
        for (int i = 0; i < ppsRef.size(); i++) {
            PictureParameterSet pps = H264Utils.readPPS(ppsRef.get(i));
            pps.pic_parameter_set_id = i;
            allPps.add(pps);
        }

        return map;
    }

    @Override
    public VirtualPacket nextPacket() throws IOException {
        while (idx < tracks.length) {
            VirtualTrack track = tracks[idx];
            VirtualPacket nextPacket = track.nextPacket();
            if (nextPacket == null) {
                idx++;
                offsetPts += lastPacket.getPts() + lastPacket.getDuration();
                offsetFn += lastPacket.getFrameNo() + 1;
            } else {
                lastPacket = nextPacket;
                return new AVCConcatPacket(nextPacket, offsetPts, offsetFn, idx);
            }
        }
        return null;
    }

    @Override
    public SampleEntry getSampleEntry() {
        return se;
    }

    @Override
    public VirtualEdit[] getEdits() {
        return null;
    }

    @Override
    public int getPreferredTimescale() {
        return tracks[0].getPreferredTimescale();
    }

    @Override
    public void close() throws IOException {
        for (int i = 0; i < tracks.length; i++) {
            tracks[i].close();
        }
    }

    private ByteBuffer patchPacket(final int idx2, ByteBuffer data) {
        ByteBuffer out = ByteBuffer.allocate(data.remaining() + 8);

        for (ByteBuffer nal : H264Utils.splitMOVPacket(data, avcCs[idx2])) {
            NALUnit nu = NALUnit.read(nal);

            if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) {

                ByteBuffer nalSizePosition = out.duplicate();
                out.putInt(0);
                nu.write(out);

                tweakers[idx2].run(nal, out, nu);
                nalSizePosition.putInt(out.position() - nalSizePosition.position() - 4);
            }
        }
        if (out.remaining() >= 5) {
            out.putInt(out.remaining() - 4);
            new NALUnit(NALUnitType.FILLER_DATA, 0).write(out);
        }

        out.clear();

        return out;
    }

    public class AVCConcatPacket implements VirtualPacket {
        private VirtualPacket packet;
        private double ptsOffset;
        private int fnOffset;
        private int idx;

        public AVCConcatPacket(VirtualPacket packet, double ptsOffset, int fnOffset, int idx) {
            this.packet = packet;
            this.ptsOffset = ptsOffset;
            this.fnOffset = fnOffset;
            this.idx = idx;
        }

        @Override
        public ByteBuffer getData() throws IOException {
            return patchPacket(idx, packet.getData());
        }

        @Override
        public int getDataLen() throws IOException {
            return packet.getDataLen() + 8;
        }

        @Override
        public double getPts() {
            return ptsOffset + packet.getPts();
        }

        @Override
        public double getDuration() {
            return packet.getDuration();
        }

        @Override
        public boolean isKeyframe() {
            return packet.isKeyframe();
        }

        @Override
        public int getFrameNo() {
            return fnOffset + packet.getFrameNo();
        }
    }
}