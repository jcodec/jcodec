package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.enums.MBPredictionMode;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessGenArrPointer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
public class ModeInfo {
    public MBModeInfo mbmi;
    public BModeInfo[] bmi = new BModeInfo[16];

    public ModeInfo() {
        mbmi = new MBModeInfo();
        for (int i = 0; i < bmi.length; i++) {
            bmi[i] = new BModeInfo();
        }
    }

    public static boolean hasSecondOrder(FullAccessGenArrPointer<ModeInfo> mode_info_context) {
        return !MBPredictionMode.has_no_y_block.contains(mode_info_context.get().mbmi.mode);
    }
}
