package org.jcodec.codecs.h264;
import org.jcodec.api.NotSupportedException;
import org.jcodec.codecs.h264.decode.SliceHeaderReader;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.RefPicMarking;
import org.jcodec.codecs.h264.io.model.RefPicMarking.InstrType;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.IntObjectMap;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Extracts H.264 frames out H.264 Elementary stream ( according to Annex B )
 * 
 * @author The JCodec project
 * 
 */
public class BufferH264ES implements DemuxerTrack, Demuxer {
    private ByteBuffer bb;
    private IntObjectMap<PictureParameterSet> pps;
    private IntObjectMap<SeqParameterSet> sps;

    // POC and framenum detection
    private int prevFrameNumOffset;
    private int prevFrameNum;
    private int prevPicOrderCntMsb;
    private int prevPicOrderCntLsb;
    private int frameNo;

    public BufferH264ES(ByteBuffer bb) {
        this.pps = new IntObjectMap<PictureParameterSet>();
        this.sps = new IntObjectMap<SeqParameterSet>();

        this.bb = bb;
        this.frameNo = 0;
    }

    @Override
    public Packet nextFrame() {
        ByteBuffer result = bb.duplicate();

        NALUnit prevNu = null;
        SliceHeader prevSh = null;
        while (true) {
            bb.mark();
            ByteBuffer buf = H264Utils.nextNALUnit(bb);
            if (buf == null)
                break;
            // NIOUtils.skip(buf, 4);
            NALUnit nu = NALUnit.read(buf);

            if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) {
                SliceHeader sh = readSliceHeader(buf, nu);

                if (prevNu != null && prevSh != null && !sameFrame(prevNu, nu, prevSh, sh)) {
                    bb.reset();
                    break;
                }
                prevSh = sh;
                prevNu = nu;
            } else if (nu.type == NALUnitType.PPS) {
                PictureParameterSet read = PictureParameterSet.read(buf);
                pps.put(read.picParameterSetId, read);
            } else if (nu.type == NALUnitType.SPS) {
                SeqParameterSet read = SeqParameterSet.read(buf);
                sps.put(read.seqParameterSetId, read);
            }
        }

        result.limit(bb.position());

        return prevSh == null ? null : detectPoc(result, prevNu, prevSh);
    }

    private SliceHeader readSliceHeader(ByteBuffer buf, NALUnit nu) {
        BitReader br = BitReader.createBitReader(buf);
        SliceHeader sh = SliceHeaderReader.readPart1(br);
        PictureParameterSet pp = pps.get(sh.picParameterSetId);
        SliceHeaderReader.readPart2(sh, nu, sps.get(pp.seqParameterSetId), pp, br);
        return sh;
    }

    private boolean sameFrame(NALUnit nu1, NALUnit nu2, SliceHeader sh1, SliceHeader sh2) {
        if (sh1.picParameterSetId != sh2.picParameterSetId)
            return false;

        if (sh1.frameNum != sh2.frameNum)
            return false;

        SeqParameterSet sps = sh1.sps;

        if ((sps.picOrderCntType == 0 && sh1.picOrderCntLsb != sh2.picOrderCntLsb))
            return false;

        if ((sps.picOrderCntType == 1 && (sh1.deltaPicOrderCnt[0] != sh2.deltaPicOrderCnt[0] || sh1.deltaPicOrderCnt[1] != sh2.deltaPicOrderCnt[1])))
            return false;

        if (((nu1.nal_ref_idc == 0 || nu2.nal_ref_idc == 0) && nu1.nal_ref_idc != nu2.nal_ref_idc))
            return false;

        if (((nu1.type == NALUnitType.IDR_SLICE) != (nu2.type == NALUnitType.IDR_SLICE)))
            return false;

        if (sh1.idrPicId != sh2.idrPicId)
            return false;

        return true;
    }

    private Packet detectPoc(ByteBuffer result, NALUnit nu, SliceHeader sh) {
        int maxFrameNum = 1 << (sh.sps.log2MaxFrameNumMinus4 + 4);
        if (detectGap(sh, maxFrameNum)) {
            issueNonExistingPic(sh, maxFrameNum);
        }
        int absFrameNum = updateFrameNumber(sh.frameNum, maxFrameNum, detectMMCO5(sh.refPicMarkingNonIDR));

        int poc = 0;
        if (nu.type == NALUnitType.NON_IDR_SLICE) {
            poc = calcPoc(absFrameNum, nu, sh);
        }
        return new Packet(result, absFrameNum, 1, 1, frameNo++, nu.type == NALUnitType.IDR_SLICE ? FrameType.KEY : FrameType.INTER, null, poc);
    }

    private int updateFrameNumber(int frameNo, int maxFrameNum, boolean mmco5) {
        int frameNumOffset;
        if (prevFrameNum > frameNo)
            frameNumOffset = prevFrameNumOffset + maxFrameNum;
        else
            frameNumOffset = prevFrameNumOffset;

        int absFrameNum = frameNumOffset + frameNo;

        prevFrameNum = mmco5 ? 0 : frameNo;
        prevFrameNumOffset = frameNumOffset;
        return absFrameNum;
    }

    private void issueNonExistingPic(SliceHeader sh, int maxFrameNum) {
        int nextFrameNum = (prevFrameNum + 1) % maxFrameNum;
        // refPictureManager.addNonExisting(nextFrameNum);
        prevFrameNum = nextFrameNum;
    }

    private boolean detectGap(SliceHeader sh, int maxFrameNum) {
        return sh.frameNum != prevFrameNum && sh.frameNum != ((prevFrameNum + 1) % maxFrameNum);
    }

    private int calcPoc(int absFrameNum, NALUnit nu, SliceHeader sh) {
        if (sh.sps.picOrderCntType == 0) {
            return calcPOC0(nu, sh);
        } else if (sh.sps.picOrderCntType == 1) {
            return calcPOC1(absFrameNum, nu, sh);
        } else {
            return calcPOC2(absFrameNum, nu, sh);
        }
    }

    private int calcPOC2(int absFrameNum, NALUnit nu, SliceHeader sh) {

        if (nu.nal_ref_idc == 0)
            return 2 * absFrameNum - 1;
        else
            return 2 * absFrameNum;
    }

    private int calcPOC1(int absFrameNum, NALUnit nu, SliceHeader sh) {

        if (sh.sps.numRefFramesInPicOrderCntCycle == 0)
            absFrameNum = 0;
        if (nu.nal_ref_idc == 0 && absFrameNum > 0)
            absFrameNum = absFrameNum - 1;

        int expectedDeltaPerPicOrderCntCycle = 0;
        for (int i = 0; i < sh.sps.numRefFramesInPicOrderCntCycle; i++)
            expectedDeltaPerPicOrderCntCycle += sh.sps.offsetForRefFrame[i];

        int expectedPicOrderCnt;
        if (absFrameNum > 0) {
            int picOrderCntCycleCnt = (absFrameNum - 1) / sh.sps.numRefFramesInPicOrderCntCycle;
            int frameNumInPicOrderCntCycle = (absFrameNum - 1) % sh.sps.numRefFramesInPicOrderCntCycle;

            expectedPicOrderCnt = picOrderCntCycleCnt * expectedDeltaPerPicOrderCntCycle;
            for (int i = 0; i <= frameNumInPicOrderCntCycle; i++)
                expectedPicOrderCnt = expectedPicOrderCnt + sh.sps.offsetForRefFrame[i];
        } else {
            expectedPicOrderCnt = 0;
        }
        if (nu.nal_ref_idc == 0)
            expectedPicOrderCnt = expectedPicOrderCnt + sh.sps.offsetForNonRefPic;

        return expectedPicOrderCnt + sh.deltaPicOrderCnt[0];
    }

    private int calcPOC0(NALUnit nu, SliceHeader sh) {

        int pocCntLsb = sh.picOrderCntLsb;
        int maxPicOrderCntLsb = 1 << (sh.sps.log2MaxPicOrderCntLsbMinus4 + 4);

        // TODO prevPicOrderCntMsb should be wrapped!!
        int picOrderCntMsb;
        if ((pocCntLsb < prevPicOrderCntLsb) && ((prevPicOrderCntLsb - pocCntLsb) >= (maxPicOrderCntLsb / 2)))
            picOrderCntMsb = prevPicOrderCntMsb + maxPicOrderCntLsb;
        else if ((pocCntLsb > prevPicOrderCntLsb) && ((pocCntLsb - prevPicOrderCntLsb) > (maxPicOrderCntLsb / 2)))
            picOrderCntMsb = prevPicOrderCntMsb - maxPicOrderCntLsb;
        else
            picOrderCntMsb = prevPicOrderCntMsb;

        if (nu.nal_ref_idc != 0) {
            prevPicOrderCntMsb = picOrderCntMsb;
            prevPicOrderCntLsb = pocCntLsb;
        }

        return picOrderCntMsb + pocCntLsb;
    }

    private boolean detectMMCO5(RefPicMarking refPicMarkingNonIDR) {
        if (refPicMarkingNonIDR == null)
            return false;

        RefPicMarking.Instruction[] instructions = refPicMarkingNonIDR.getInstructions();
        for (int i = 0; i < instructions.length; i++) {
            RefPicMarking.Instruction instr = instructions[i];
            if (instr.getType() == InstrType.CLEAR) {
                return true;
            }
        }
        return false;
    }

    public SeqParameterSet[] getSps() {
        return sps.values(new SeqParameterSet[0]);
    }

    public PictureParameterSet[] getPps() {
        return pps.values(new PictureParameterSet[0]);
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        return null;
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<? extends DemuxerTrack> getTracks() {
        return getVideoTracks();
    }

    @Override
    public List<? extends DemuxerTrack> getVideoTracks() {
        List<DemuxerTrack> tracks = new ArrayList<DemuxerTrack>();
        tracks.add(this);
        return tracks;
    }

    @Override
    public List<? extends DemuxerTrack> getAudioTracks() {
        List<DemuxerTrack> tracks = new ArrayList<DemuxerTrack>();
        return tracks;
    }
}