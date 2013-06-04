package org.jcodec.codecs.h264;

import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.decode.SliceHeaderReader;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.RefPicMarking;
import org.jcodec.codecs.h264.io.model.RefPicMarking.InstrType;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.IntObjectMap;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Extracts H.264 frames out H.264 Elementary stream ( according to Annex B )
 * 
 * @author Jay Codec
 * 
 */
public class MappedH264ES implements DemuxerTrack {
    private ByteBuffer bb;
    private SliceHeaderReader shr;
    private IntObjectMap<PictureParameterSet> pps = new IntObjectMap<PictureParameterSet>();
    private IntObjectMap<SeqParameterSet> sps = new IntObjectMap<SeqParameterSet>();

    // POC and framenum detection
    private int prevFrameNumOffset;
    private int prevFrameNum;
    private int prevPicOrderCntMsb;
    private int prevPicOrderCntLsb;
    private int frameNo;

    public MappedH264ES(ByteBuffer bb) {
        this.bb = bb;
        this.shr = new SliceHeaderReader();
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
//            NIOUtils.skip(buf, 4);
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
                pps.put(read.pic_parameter_set_id, read);
            } else if (nu.type == NALUnitType.SPS) {
                SeqParameterSet read = SeqParameterSet.read(buf);
                sps.put(read.seq_parameter_set_id, read);
            }
        }

        result.limit(bb.position());

        return prevSh == null ? null : detectPoc(result, prevNu, prevSh);
    }

    private SliceHeader readSliceHeader(ByteBuffer buf, NALUnit nu) {
        BitReader br = new BitReader(buf);
        SliceHeader sh = shr.readPart1(br);
        PictureParameterSet pp = pps.get(sh.pic_parameter_set_id);
        shr.readPart2(sh, nu, sps.get(pp.seq_parameter_set_id), pp, br);
        return sh;
    }

    private boolean sameFrame(NALUnit nu1, NALUnit nu2, SliceHeader sh1, SliceHeader sh2) {
        if (sh1.pic_parameter_set_id != sh2.pic_parameter_set_id)
            return false;

        if (sh1.frame_num != sh2.frame_num)
            return false;

        SeqParameterSet sps = sh1.sps;

        if ((sps.pic_order_cnt_type == 0 && sh1.pic_order_cnt_lsb != sh2.pic_order_cnt_lsb))
            return false;

        if ((sps.pic_order_cnt_type == 1 && (sh1.delta_pic_order_cnt[0] != sh2.delta_pic_order_cnt[0] || sh1.delta_pic_order_cnt[1] != sh2.delta_pic_order_cnt[1])))
            return false;

        if (((nu1.nal_ref_idc == 0 || nu2.nal_ref_idc == 0) && nu1.nal_ref_idc != nu2.nal_ref_idc))
            return false;

        if (((nu1.type == NALUnitType.IDR_SLICE) != (nu2.type == NALUnitType.IDR_SLICE)))
            return false;

        if (sh1.idr_pic_id != sh2.idr_pic_id)
            return false;

        return true;
    }

    private Packet detectPoc(ByteBuffer result, NALUnit nu, SliceHeader sh) {
        int maxFrameNum = 1 << (sh.sps.log2_max_frame_num_minus4 + 4);
        if (detectGap(sh, maxFrameNum)) {
            issueNonExistingPic(sh, maxFrameNum);
        }
        int absFrameNum = updateFrameNumber(sh.frame_num, maxFrameNum, detectMMCO5(sh.refPicMarkingNonIDR));

        int poc = 0;
        if (nu.type == NALUnitType.NON_IDR_SLICE) {
            poc = calcPoc(absFrameNum, nu, sh);
        }
        return new Packet(result, absFrameNum, 1, 1, frameNo++, nu.type == NALUnitType.IDR_SLICE, null, poc);
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
        return sh.frame_num != prevFrameNum && sh.frame_num != ((prevFrameNum + 1) % maxFrameNum);
    }

    private int calcPoc(int absFrameNum, NALUnit nu, SliceHeader sh) {
        if (sh.sps.pic_order_cnt_type == 0) {
            return calcPOC0(nu, sh);
        } else if (sh.sps.pic_order_cnt_type == 1) {
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

        if (sh.sps.num_ref_frames_in_pic_order_cnt_cycle == 0)
            absFrameNum = 0;
        if (nu.nal_ref_idc == 0 && absFrameNum > 0)
            absFrameNum = absFrameNum - 1;

        int expectedDeltaPerPicOrderCntCycle = 0;
        for (int i = 0; i < sh.sps.num_ref_frames_in_pic_order_cnt_cycle; i++)
            expectedDeltaPerPicOrderCntCycle += sh.sps.offsetForRefFrame[i];

        int expectedPicOrderCnt;
        if (absFrameNum > 0) {
            int picOrderCntCycleCnt = (absFrameNum - 1) / sh.sps.num_ref_frames_in_pic_order_cnt_cycle;
            int frameNumInPicOrderCntCycle = (absFrameNum - 1) % sh.sps.num_ref_frames_in_pic_order_cnt_cycle;

            expectedPicOrderCnt = picOrderCntCycleCnt * expectedDeltaPerPicOrderCntCycle;
            for (int i = 0; i <= frameNumInPicOrderCntCycle; i++)
                expectedPicOrderCnt = expectedPicOrderCnt + sh.sps.offsetForRefFrame[i];
        } else {
            expectedPicOrderCnt = 0;
        }
        if (nu.nal_ref_idc == 0)
            expectedPicOrderCnt = expectedPicOrderCnt + sh.sps.offset_for_non_ref_pic;

        return expectedPicOrderCnt + sh.delta_pic_order_cnt[0];
    }

    private int calcPOC0(NALUnit nu, SliceHeader sh) {

        int pocCntLsb = sh.pic_order_cnt_lsb;
        int maxPicOrderCntLsb = 1 << (sh.sps.log2_max_pic_order_cnt_lsb_minus4 + 4);

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

        for (RefPicMarking.Instruction instr : refPicMarkingNonIDR.getInstructions()) {
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
    public boolean gotoFrame(long i) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long getCurFrame() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void seek(double second) {
        // TODO Auto-generated method stub
        
    }
}