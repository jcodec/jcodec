package org.jcodec.codecs.h264.decode.dpb;

import org.jcodec.codecs.h264.io.model.RefPicReordering;
import org.jcodec.codecs.h264.io.model.RefPicReordering.InstrType;
import org.jcodec.codecs.h264.io.model.RefPicReordering.ReorderOp;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Helper that builds a reference picture list from the list of pictures
 * currently used for reference. The list is reordered according to the
 * reordering parameter.
 * 
 * @author Jay Codec
 * 
 */
public class RefListBuilder {
    private int maxFrameNum;

    public RefListBuilder(int maxFrameNum) {
        this.maxFrameNum = maxFrameNum;
    }

    public Picture[] buildRefList(DecodedPicture[] buf, RefPicReordering reordering, int frameNo) {

        if (reordering == null) {
            return buildRefList(buf, frameNo);
        } else {
            return buildRefListWithReordering(buf, reordering, frameNo);
        }
    }

    private Picture[] buildRefListWithReordering(DecodedPicture[] buf, RefPicReordering refPicReordering, int frameNo) {

        DecodedPicture[] copy = new DecodedPicture[buf.length];
        System.arraycopy(buf, 0, copy, 0, buf.length);

        Picture[] refList = new Picture[copy.length];

        int predPicNo = frameNo;
        int refIdx = 0;
        for (ReorderOp instr : refPicReordering.getInstructions()) {
            Picture pic;
            if (instr.getType() == InstrType.FORWARD) {
                int picNo = predPicNo + instr.getParam();
                if (picNo >= maxFrameNum)
                    picNo -= maxFrameNum;
                predPicNo = picNo;

                if (picNo > frameNo)
                    picNo -= maxFrameNum;

                pic = withdrawShortTerm(copy, picNo, frameNo);
            } else if (instr.getType() == InstrType.BACKWARD) {
                int picNo = predPicNo - instr.getParam();
                if (picNo < 0)
                    picNo += maxFrameNum;
                predPicNo = picNo;

                if (picNo > frameNo)
                    picNo -= maxFrameNum;

                pic = withdrawShortTerm(copy, picNo, frameNo);
            } else if (instr.getType() == InstrType.LONG_TERM) {
                pic = withdrawLongTerm(copy, instr.getParam());
            } else {
                throw new RuntimeException("Unsupported MM instruction type");
            }
            refList[refIdx++] = pic;
        }

        int len = orderRefList(copy, frameNo);

        for (int i = 0; refIdx < refList.length && i < len; refIdx++, i++) {
            refList[refIdx] = copy[i].getPicture();
        }

        return refList;
    }

    private Picture withdrawLongTerm(DecodedPicture[] buf, int picNo) {
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] != null && buf[i].isLongTerm() == true && buf[i].getLtPicId() == picNo) {
                Picture picture = buf[i].getPicture();
                buf[i] = null;
                return picture;
            }
        }
        return null;
    }

    private Picture withdrawShortTerm(DecodedPicture[] buf, int picNo, int curFrameNo) {
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] != null && buf[i].isLongTerm() == false && getRefPicNum(buf[i], curFrameNo) == picNo) {
                Picture picture = buf[i].getPicture();
                buf[i] = null;
                return picture;
            }
        }
        return null;
    }

    private int getRefPicNum(DecodedPicture ref, int curFrameNo) {

        int refFrameNum = ref.getFrameNum();
        if (refFrameNum > curFrameNo)
            return refFrameNum - maxFrameNum;
        else
            return refFrameNum;
    }

    private Picture[] buildRefList(DecodedPicture[] buf, int curFrameNo) {
        DecodedPicture[] copy = new DecodedPicture[buf.length];
        System.arraycopy(buf, 0, copy, 0, buf.length);

        int len = orderRefList(copy, curFrameNo);

        Picture[] list = new Picture[buf.length];
        for (int i = 0; i < len; i++) {
            list[i] = copy[i].getPicture();
        }

        return list;
    }

    private int orderRefList(DecodedPicture[] buf, int curFrameNo) {

        int len = compactList(buf);

        if (len == 0)
            return 0;

        int firstLTIndex = separatePictures(buf, len);

        sortShortTerm(buf, 0, firstLTIndex - 1, curFrameNo);
        sortLongTerm(buf, firstLTIndex, len - 1);

        return len;
    }

    private void sortShortTerm(DecodedPicture[] buf, int firstIdx, int lastIdx, int curFrameNo) {

        for (int i = firstIdx; i <= lastIdx; ++i) {
            int max = i;
            int maxNo = getRefPicNum(buf[i], curFrameNo);
            for (int j = i + 1; j <= lastIdx; ++j) {
                int jNo = getRefPicNum(buf[j], curFrameNo);
                if (jNo > maxNo) {
                    max = j;
                    maxNo = jNo;
                }
            }
            DecodedPicture tmp = buf[max];
            buf[max] = buf[i];
            buf[i] = tmp;
        }
    }

    private void sortLongTerm(DecodedPicture[] buf, int firstIdx, int lastIdx) {

        for (int i = firstIdx; i <= lastIdx; ++i) {
            int min = i;
            int minNo = buf[i].getLtPicId();
            for (int j = i + 1; j <= lastIdx; ++j) {
                int jNo = buf[j].getLtPicId();
                if (jNo < minNo) {
                    min = j;
                    minNo = jNo;
                }
            }
            DecodedPicture tmp = buf[min];
            buf[min] = buf[i];
            buf[i] = tmp;
        }
    }

    private int separatePictures(DecodedPicture[] buf, int len) {
        int s = 0, l = len - 1;

        while (s <= l) {

            if (!buf[s].isLongTerm()) {
                s++;
                continue;
            }
            if (buf[l].isLongTerm()) {
                l--;
                continue;
            }

            DecodedPicture tmp = buf[s];
            buf[s] = buf[l];
            buf[l] = tmp;
            s++;
            l--;
        }
        return s;
    }

    private int compactList(DecodedPicture[] buf) {
        int i, k;
        for (i = 0, k = 0; i < buf.length; i++) {
            if (buf[i] != null)
                buf[k++] = buf[i];
        }
        return k;
    }
}
