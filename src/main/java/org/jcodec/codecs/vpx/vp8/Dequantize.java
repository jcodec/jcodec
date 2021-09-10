package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.data.BlockD;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.ReadOnlyIntArrPointer;

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
public class Dequantize {
    static void vp8_dequantize_b(BlockD d, ReadOnlyIntArrPointer DQC) {
        int i;
        FullAccessIntArrPointer DQ = d.dqcoeff;
        ReadOnlyIntArrPointer Q = d.qcoeff;

        for (i = 0; i < 16; ++i) {
            DQ.setRel(i, (short) (Q.getRel(i) * DQC.getRel(i)));
        }
    }

    static void vp8_dequant_idct_add(FullAccessIntArrPointer input, ReadOnlyIntArrPointer dq,
            FullAccessIntArrPointer dest, int stride) {
        int i;

        for (i = 0; i < 16; ++i) {
            input.setRel(i, (short) (dq.getRel(i) * input.getRel(i)));
        }

        IDCTllm.vp8_short_idct4x4llm(input, dest, stride, dest, stride);

        input.memset(0, (short) 0, 16);
    }
}
