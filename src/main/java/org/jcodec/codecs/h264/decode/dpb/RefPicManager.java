package org.jcodec.codecs.h264.decode.dpb;

import org.jcodec.codecs.h264.io.model.RefPicMarking;
import org.jcodec.codecs.h264.io.model.RefPicMarkingIDR;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Manages reference pictures, in a way that new ref picture causes one of the
 * old to be removed. Supports memory management operations
 * 
 * @author Jay Codec
 * 
 */
public class RefPicManager {

    private DecodedPictureBuffer dpb;
    private int maxRef;
    private int maxFrameNum;

    public RefPicManager(DecodedPictureBuffer dpb, int maxRef, int maxFrameNum) {
        this.dpb = dpb;
        this.maxRef = maxRef;
        this.maxFrameNum = maxFrameNum;
    }

    private void unrefOldestIfNeeded(DecodedPicture curPic) {
        DecodedPicture oldest = null;
        int oldestPicNum = -1;
        int nRefs = 0;
        for (DecodedPicture pic : dpb) {
            if (pic.isRef()) {
                if (!pic.isLongTerm()) {
                    int refPicNum = getRefPicNum(pic, curPic);
                    if (oldest == null || refPicNum < oldestPicNum) {
                        oldest = pic;
                        oldestPicNum = refPicNum;
                    }
                }
                nRefs++;
            }
        }

        if (nRefs > maxRef - 1 && oldest != null)
            oldest.unreference();
    }

    private int getRefPicNum(DecodedPicture ref, DecodedPicture curPicture) {

        int refFrameNum = ref.getFrameNum();
        if (refFrameNum > curPicture.getFrameNum())
            return refFrameNum - maxFrameNum;
        else
            return refFrameNum;
    }

    private void unrefShortTerm(int picNum, DecodedPicture curPicture) {
        for (DecodedPicture pic : dpb) {
            if (pic.isRef() && !pic.isLongTerm() && getRefPicNum(pic, curPicture) == picNum) {
                pic.unreference();
                break;
            }
        }
    }

    private void unrefLongTerm(int longTermId) {
        for (DecodedPicture pic : dpb) {
            if (pic.isRef() && pic.isLongTerm() && pic.getLtPicId() == longTermId) {
                pic.unreference();
                break;
            }
        }
    }

    private void convert(int shortNo, int longNo, DecodedPicture curPicture) {
        unrefLongTerm(longNo);
        for (DecodedPicture pic : dpb) {
            if (pic.isRef() && !pic.isLongTerm() && getRefPicNum(pic, curPicture) == shortNo) {
                pic.makeLongTerm(longNo);
                break;
            }
        }
    }

    private void truncateLongTerm(int no) {
        for (DecodedPicture pic : dpb) {
            if (pic.isRef() && pic.isLongTerm() && pic.getLtPicId() > no) {
                pic.unreference();
            }
        }
    }

    public void clearAll() {
        for (DecodedPicture pic : dpb) {
            if (pic.isRef())
                pic.unreference();
        }
    }

    public void performMarking(RefPicMarking refPicMarking, DecodedPicture curPic) {

        if (refPicMarking != null) {

            // for (RefPicMarking.Instruction instr : refPicMarking
            // .getInstructions()) {
            // if (instr.getType() == InstrType.CLEAR) {
            // curPic.resetFrameNum();
            // }
            // }

            int curFrameNum = curPic.getFrameNum();
            for (RefPicMarking.Instruction instr : refPicMarking.getInstructions()) {
                switch (instr.getType()) {
                case REMOVE_SHORT:
                    int frm = curFrameNum - instr.getArg1();
                    unrefShortTerm(frm, curPic);
                    break;

                case REMOVE_LONG:
                    unrefLongTerm(instr.getArg1());
                    break;
                case CONVERT_INTO_LONG:
                    int stNo = curFrameNum - instr.getArg1();
                    int ltNo = instr.getArg2();
                    convert(stNo, ltNo, curPic);
                    break;
                case TRUNK_LONG:
                    truncateLongTerm(instr.getArg1() - 1);
                    break;
                case CLEAR:
                    clearAll();
                    break;
                case MARK_LONG:
                    curPic.makeLongTerm(instr.getArg1());
                }
            }
        } else {
            if (curPic.isRef()) {
                unrefOldestIfNeeded(curPic);
            }
        }

        dpb.bumpPictures();
        dpb.add(curPic);
    }

    public void addNonExisting(int frameNo) {
        DecodedPicture pic = new DecodedPicture(null, 0, false, true, frameNo, false, false);

        unrefOldestIfNeeded(pic);
        dpb.bumpPictures();
        dpb.add(pic);
    }

    public void performIDRMarking(RefPicMarkingIDR refPicMarkingIDR, DecodedPicture curPic) {
        clearAll();

        if (refPicMarkingIDR.isUseForlongTerm()) {
            curPic.makeLongTerm(0);
        }

        dpb.bumpPictures();
        dpb.add(curPic);
    }

    public DecodedPicture[] getAllRefs() {
        DecodedPicture[] refs = new DecodedPicture[maxRef];
        int i = 0;
        for (DecodedPicture pic : dpb) {
            if (pic.isRef()) {
                refs[i++] = pic;
            }
        }
        return refs;
    }
}