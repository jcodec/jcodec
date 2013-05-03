package org.jcodec.codecs.h264;

import static org.jcodec.codecs.h264.io.model.NALUnitType.IDR_SLICE;

import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.RefPicMarking.InstrType;
import org.jcodec.codecs.h264.io.model.RefPicMarking.Instruction;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * POC ( Picture Order Count ) manager
 * 
 * Picture Order Count is used to represent an order of picture in a GOP ( Group
 * of Pictures ) this is needed to correctly reorder and B-framed GOPs. POC is
 * also used when building lists of reference pictures ( see 8.2.4.2 ).
 * 
 * There are 3 possible ways of assigning POC to decoded pictures:
 * 
 * - Explicit, i.e. POC is directly specified in a slice header in form <POC
 * Pred> + <POC Dec>. <POC Pred> is a significant part of POC ( see 8.2.1.1 ). -
 * Frame based type 1 ( see 8.2.1.2 ). - Frame based type 2 ( see 8.2.1.3 ).
 * 
 * @author The JCodec project
 * 
 */
public class POCManager {

    private int prevPOCMsb;
    private int prevPOCLsb;

    public int calcPOC(SliceHeader firstSliceHeader, NALUnit firstNu) {
        switch (firstSliceHeader.sps.pic_order_cnt_type) {
        case 0:
            return calcPOC0(firstSliceHeader, firstNu);
        case 1:
            return calcPOC1(firstSliceHeader, firstNu);
        case 2:
            return calcPOC2(firstSliceHeader, firstNu);
        default:
            throw new RuntimeException("POC no!!!");
        }

    }

    private int calcPOC2(SliceHeader firstSliceHeader, NALUnit firstNu) {
        return firstSliceHeader.frame_num << 1;
    }

    private int calcPOC1(SliceHeader firstSliceHeader, NALUnit firstNu) {
        return firstSliceHeader.frame_num << 1;
    }

    private int calcPOC0(SliceHeader firstSliceHeader, NALUnit firstNu) {
        if (firstNu.type == IDR_SLICE) {
            prevPOCMsb = prevPOCLsb = 0;
        }
        int maxPOCLsbDiv2 = 1 << (firstSliceHeader.sps.log2_max_pic_order_cnt_lsb_minus4 + 3), maxPOCLsb = maxPOCLsbDiv2 << 1;
        int POCLsb = firstSliceHeader.pic_order_cnt_lsb;
        
        int POCMsb, POC;
        if ((POCLsb < prevPOCLsb) && ((prevPOCLsb - POCLsb) >= maxPOCLsbDiv2))
            POCMsb = prevPOCMsb + maxPOCLsb;
        else if ((POCLsb > prevPOCLsb) && ((POCLsb - prevPOCLsb) > maxPOCLsbDiv2))
            POCMsb = prevPOCMsb - maxPOCLsb;
        else
            POCMsb = prevPOCMsb;

        POC = POCMsb + POCLsb;

        if (firstNu.nal_ref_idc > 0) {
            if (hasMMCO5(firstSliceHeader, firstNu)) {
                prevPOCMsb = 0;
                prevPOCLsb = POC;
            } else {
                prevPOCMsb = POCMsb;
                prevPOCLsb = POCLsb;
            }
        }

        return POC;
    }

    private boolean hasMMCO5(SliceHeader firstSliceHeader, NALUnit firstNu) {
        if (firstNu.type != IDR_SLICE && firstSliceHeader.refPicMarkingNonIDR != null) {
            Instruction[] instructions = firstSliceHeader.refPicMarkingNonIDR.getInstructions();
            for (Instruction instruction : instructions) {
                if (instruction.getType() == InstrType.CLEAR)
                    return true;
            }
        }
        return false;
    }
}