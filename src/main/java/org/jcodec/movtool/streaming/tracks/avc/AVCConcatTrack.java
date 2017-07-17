package org.jcodec.movtool.streaming.tracks.avc;
import java.lang.IllegalStateException;
import java.lang.System;


import static org.jcodec.codecs.h264.H264Utils.readPPSFromBufferList;
import static org.jcodec.codecs.h264.H264Utils.readSPSFromBufferList;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.H264Utils.SliceHeaderTweaker;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Rational;
import org.jcodec.movtool.streaming.CodecMeta;
import org.jcodec.movtool.streaming.VideoCodecMeta;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.VirtualTrack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private CodecMeta se;
    private Map<Integer, Integer> map;
    private List<PictureParameterSet> allPps;
    private List<SeqParameterSet> allSps;
    private SliceHeaderTweaker[] tweakers;

    public AVCConcatTrack(VirtualTrack... arguments) {
        this.tracks = arguments;

        Rational pasp = null;

        allPps = new ArrayList<PictureParameterSet>();
        allSps = new ArrayList<SeqParameterSet>();
        tweakers = new H264Utils.SliceHeaderTweaker[arguments.length];
        map = new HashMap<Integer, Integer>();
        for (int i = 0; i < arguments.length; i++) {
            CodecMeta se = arguments[i].getCodecMeta();
            if (!(se instanceof VideoCodecMeta))
                throw new RuntimeException("Not a video track.");
            if (!"avc1".equals(se.getFourcc()))
                throw new RuntimeException("Not an AVC track.");

            VideoCodecMeta vcm = (VideoCodecMeta) se;

            Rational paspL = vcm.getPasp();
            if (pasp != null && paspL != null && !pasp.equalsRational(paspL))
                throw new RuntimeException("Can not concat video tracks with different Pixel Aspect Ratio.");
            pasp = paspL;

            List<ByteBuffer> rawPPSs = H264Utils.getRawPPS(se.getCodecPrivate());
            for (ByteBuffer ppsBuffer : rawPPSs) {
                PictureParameterSet pps = H264Utils.readPPS(NIOUtils.duplicate(ppsBuffer));
                // Allow up to 256 SPS/PPS per clip
                pps.picParameterSetId |= i << 8;
                pps.seqParameterSetId |= i << 8;
                allPps.add(pps);
            }
            List<ByteBuffer> rawSPSs = H264Utils.getRawSPS(se.getCodecPrivate());
            for (ByteBuffer spsBuffer : rawSPSs) {
                SeqParameterSet sps = H264Utils.readSPS(NIOUtils.duplicate(spsBuffer));
                sps.seqParameterSetId |= i << 8;
                allSps.add(sps);
            }
            int idx2 = i;
            tweakers[i] = new AvccTweaker(rawSPSs, rawPPSs, idx2, this);
        }
        mergePS(allSps, allPps, map);

        VideoCodecMeta codecMeta = (VideoCodecMeta) arguments[0].getCodecMeta();

        se = VideoCodecMeta.createVideoCodecMeta("avc1", H264Utils.saveCodecPrivate(H264Utils.saveSPS(allSps), H264Utils.savePPS(allPps)), codecMeta.getSize(), codecMeta.getPasp());
    }

    private void mergePS(List<SeqParameterSet> allSps, List<PictureParameterSet> allPps, Map<Integer, Integer> map) {
        List<ByteBuffer> spsRef = new ArrayList<ByteBuffer>();
        for (SeqParameterSet sps : allSps) {
            int spsId = sps.seqParameterSetId;
            sps.seqParameterSetId = 0;
            ByteBuffer serial = H264Utils.writeSPS(sps, 32);
            int idx = NIOUtils.find(spsRef, serial);
            if (idx == -1) {
                idx = spsRef.size();
                spsRef.add(serial);
            }
            for (PictureParameterSet pps : allPps) {
                if (pps.seqParameterSetId == spsId)
                    pps.seqParameterSetId = idx;
            }
        }
        List<ByteBuffer> ppsRef = new ArrayList<ByteBuffer>();
        for (PictureParameterSet pps : allPps) {
            int ppsId = pps.picParameterSetId;
            pps.picParameterSetId = 0;
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
            sps.seqParameterSetId = i;
            allSps.add(sps);
        }

        allPps.clear();
        for (int i = 0; i < ppsRef.size(); i++) {
            PictureParameterSet pps = H264Utils.readPPS(ppsRef.get(i));
            pps.picParameterSetId = i;
            allPps.add(pps);
        }
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
                return new AVCConcatPacket(this, nextPacket, offsetPts, offsetFn, idx);
            }
        }
        return null;
    }

    @Override
    public CodecMeta getCodecMeta() {
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

        for (ByteBuffer nal : H264Utils.splitFrame(data)) {
            NALUnit nu = NALUnit.read(nal);

            if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) {

                out.putInt(1);
                nu.write(out);

                tweakers[idx2].run(nal, out, nu);
            } else {
                Logger.warn("Skipping NAL unit: " + nu.type);
            }
        }
        if (out.remaining() >= 5) {
            out.putInt(1);
            new NALUnit(NALUnitType.FILLER_DATA, 0).write(out);
        }

        out.clear();

        return out;
    }

    private static final class AvccTweaker extends H264Utils.SliceHeaderTweaker {
        private final int idx2;
        private AVCConcatTrack track;

        private AvccTweaker(List<ByteBuffer> spsList, List<ByteBuffer> ppsList, int idx2, AVCConcatTrack track) {
            super();
            this.sps = readSPSFromBufferList(spsList);
            this.pps = readPPSFromBufferList(ppsList);
            this.idx2 = idx2;
            this.track = track;
        }

        @Override
        protected void tweak(SliceHeader sh) {
            sh.picParameterSetId = track.map.get((idx2 << 8) | sh.picParameterSetId);
        }
    }

    public static class AVCConcatPacket implements VirtualPacket {
        private VirtualPacket packet;
        private double ptsOffset;
        private int fnOffset;
        private int idx;
		private AVCConcatTrack track;

        public AVCConcatPacket(AVCConcatTrack track, VirtualPacket packet, double ptsOffset, int fnOffset, int idx) {
            this.track = track;
			this.packet = packet;
            this.ptsOffset = ptsOffset;
            this.fnOffset = fnOffset;
            this.idx = idx;
        }

        @Override
        public ByteBuffer getData() throws IOException {
            return track.patchPacket(idx, packet.getData());
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