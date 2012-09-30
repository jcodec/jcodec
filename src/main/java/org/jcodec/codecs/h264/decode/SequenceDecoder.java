package org.jcodec.codecs.h264.decode;

import java.io.IOException;

import org.jcodec.codecs.h264.AccessUnit;
import org.jcodec.codecs.h264.H264Demuxer;
import org.jcodec.codecs.h264.decode.dpb.DecodedPicture;
import org.jcodec.codecs.h264.decode.dpb.DecodedPictureBuffer;
import org.jcodec.codecs.h264.decode.dpb.RefPicManager;
import org.jcodec.codecs.h264.decode.imgop.Cropper;
import org.jcodec.codecs.h264.decode.model.DecodedFrame;
import org.jcodec.codecs.h264.decode.model.IDRFrame;
import org.jcodec.codecs.h264.decode.model.NonIDRFrame;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.RefPicMarking;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.RefPicMarking.InstrType;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.Debug;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Iterates through the pictures in the access unit
 * 
 * @author Jay Codec
 * 
 */
public class SequenceDecoder {

    private H264Demuxer auSource;
    private ErrorResilence er;
    private AccessUnitReader currentAU;

    public class Sequence {
        private DecodedPictureBuffer dpb;
        private RefPicManager refPictureManager;
        private SeqParameterSet sps;
        private Cropper cropper;

        private int seqId;
        private int nextPOC;

        private int prevFrameNumOffset;
        private int prevFrameNum;

        private int prevPicOrderCntMsb;
        private int prevPicOrderCntLsb;

        private int maxFrameNum;
        private int maxPicOrderCntLsb;

        public Sequence() throws IOException {
            sps = currentAU.getSPS();
            seqId = currentAU.getSliceHeader().idr_pic_id;

            maxFrameNum = 1 << (sps.log2_max_frame_num_minus4 + 4);
            maxPicOrderCntLsb = 1 << (sps.log2_max_pic_order_cnt_lsb_minus4 + 4);

            int picSizeInMbs = (sps.pic_width_in_mbs_minus1 + 1) * (sps.pic_height_in_map_units_minus1 + 1);

            dpb = DecodedPictureBuffer.getForProfileAndDimension(sps.level_idc, picSizeInMbs);
            refPictureManager = new RefPicManager(dpb, sps.num_ref_frames, maxFrameNum);
            cropper = new Cropper(sps);
        }

        public SeqParameterSet getSPS() {
            return sps;
        }

        public Picture nextPicture() throws IOException {

            do {
                DecodedPicture needle = null;
                DecodedPicture leastPOC = null;
                DecodedPicture last = null;
                for (DecodedPicture pic : dpb) {
                    if (pic.getPoc() == nextPOC) {
                        needle = pic;
                        break;
                    }
                    if (pic.isDisplay() && (leastPOC == null || pic.getPoc() < leastPOC.getPoc())) {
                        leastPOC = pic;
                    }
                    last = pic;
                }

                if (needle != null) {
                    needle.setDisplayed();
                    if (!needle.hasMMCO5())
                        nextPOC = needle.getPoc() + 1;
                    else
                        nextPOC = 1;
                    dpb.bumpPictures();
                    return cropper.crop(needle.getPicture());
                }

                boolean lastMMCO5 = last != null && last.hasMMCO5() && last.isDisplay();

                if (dpb.isFull() || lastMMCO5 || !processNextAccessUnit()) {
                    if (leastPOC != null) {
                        leastPOC.setDisplayed();
                        if (!leastPOC.hasMMCO5())
                            nextPOC = leastPOC.getPoc() + 1;
                        else
                            nextPOC = 1;
                        dpb.bumpPictures();
                        return cropper.crop(leastPOC.getPicture());
                    } else {
                        return null;
                    }
                }

            } while (true);
        }

        private boolean processNextAccessUnit() throws IOException {

            if (currentAU == null)
                return false;

            SeqParameterSet curSPS = currentAU.getSPS();

            boolean nextSequence = currentAU.getNU().type == NALUnitType.IDR_SLICE
                    && currentAU.getSliceHeader().idr_pic_id != seqId;
            if (nextSequence || curSPS.seq_parameter_set_id != sps.seq_parameter_set_id)
                return false;

            SliceHeader sh = currentAU.getSliceHeader();

            if (!detectGap(sh)) {
                return reallyProcessNextAU(sh);
            } else {
                issueNonExistingPic(sh);
                return true;
            }
        }

        private boolean reallyProcessNextAU(SliceHeader sh) throws IOException {
            boolean mmco5 = detectMMCO5(sh.refPicMarkingNonIDR);

            int absFrameNum = updateFrameNumber(sh.frame_num, mmco5);

            int poc = 0;
            if (currentAU.getNU().type == NALUnitType.NON_IDR_SLICE) {
                poc = calcPoc(absFrameNum, currentAU.getNU(), currentAU.getSliceHeader());
            }

            Debug.println("\n****** POC: " + poc + " ********* ");

            DecodedPicture[] refList = refPictureManager.getAllRefs();
            Picture picture = er.decodeAccessUnit(currentAU, refList);

            DecodedFrame frame;
            if (currentAU.getNU().type == NALUnitType.IDR_SLICE) {
                frame = new IDRFrame(picture, currentAU.getNU(), 0, 0, sh.refPicMarkingIDR);
            } else {

                frame = new NonIDRFrame(picture, currentAU.getNU(), mmco5 ? 0 : sh.frame_num, poc,
                        sh.refPicMarkingNonIDR);
            }

            currentAU = nextAU();

            saveReference(frame, mmco5);

            return true;
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

        private void issueNonExistingPic(SliceHeader sh) {
            int nextFrameNum = (prevFrameNum + 1) % maxFrameNum;
            refPictureManager.addNonExisting(nextFrameNum);
            prevFrameNum = nextFrameNum;
        }

        private boolean detectGap(SliceHeader sh) {
            return sh.frame_num != prevFrameNum && sh.frame_num != ((prevFrameNum + 1) % maxFrameNum);
        }

        private int updateFrameNumber(int frameNo, boolean mmco5) {
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

        private int calcPoc(int absFrameNum, NALUnit nu, SliceHeader sh) {
            if (sps.pic_order_cnt_type == 0) {
                return calcPOC0(absFrameNum, nu, sh);
            } else if (sps.pic_order_cnt_type == 1) {
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

            if (sps.num_ref_frames_in_pic_order_cnt_cycle == 0)
                absFrameNum = 0;
            if (nu.nal_ref_idc == 0 && absFrameNum > 0)
                absFrameNum = absFrameNum - 1;

            int expectedDeltaPerPicOrderCntCycle = 0;
            for (int i = 0; i < sps.num_ref_frames_in_pic_order_cnt_cycle; i++)
                expectedDeltaPerPicOrderCntCycle += sps.offsetForRefFrame[i];

            int expectedPicOrderCnt;
            if (absFrameNum > 0) {
                int picOrderCntCycleCnt = (absFrameNum - 1) / sps.num_ref_frames_in_pic_order_cnt_cycle;
                int frameNumInPicOrderCntCycle = (absFrameNum - 1) % sps.num_ref_frames_in_pic_order_cnt_cycle;

                expectedPicOrderCnt = picOrderCntCycleCnt * expectedDeltaPerPicOrderCntCycle;
                for (int i = 0; i <= frameNumInPicOrderCntCycle; i++)
                    expectedPicOrderCnt = expectedPicOrderCnt + sps.offsetForRefFrame[i];
            } else {
                expectedPicOrderCnt = 0;
            }
            if (nu.nal_ref_idc == 0)
                expectedPicOrderCnt = expectedPicOrderCnt + sps.offset_for_non_ref_pic;

            return expectedPicOrderCnt + sh.delta_pic_order_cnt[0];
        }

        private int calcPOC0(int absFrameNum, NALUnit nu, SliceHeader sh) {

            int pocCntLsb = sh.pic_order_cnt_lsb;

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

        private void saveReference(DecodedFrame frame, boolean mmco5) {
            DecodedPicture picture = new DecodedPicture(frame.getPicture(), frame.getPOC(), true,
                    frame.getNU().nal_ref_idc != 0, frame.getFrameNum(), false, mmco5);

            if (frame instanceof IDRFrame) {
                refPictureManager.performIDRMarking(((IDRFrame) frame).getMarking(), picture);
            } else {
                refPictureManager.performMarking(((NonIDRFrame) frame).getMarking(), picture);
            }
        }
    }

    private AccessUnitReader nextAU() throws IOException {
        AccessUnit au = auSource.nextAcceessUnit();
        if (au != null)
            return new AccessUnitReader(au, auSource);

        return null;
    }

    public SequenceDecoder(H264Demuxer auSource, ErrorResilence er) {
        this.auSource = auSource;
        this.er = er;
    }

    public Sequence nextSequence() throws IOException {
        if (currentAU == null)
            currentAU = nextAU();

        while (currentAU != null && currentAU.getNU().type != NALUnitType.IDR_SLICE) {
            currentAU = nextAU();
        }

        if (currentAU == null)
            return null;

        return new Sequence();
    }
}