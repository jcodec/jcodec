package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;

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
public class MVContext {
    public static final MVContext[] vp8_default_mv_context = {

            new MVContext(new short[] {
                    /* row */
                    162, /* is short */
                    128, /* sign */
                    225, 146, 172, 147, 214, 39, 156, /* short tree */
                    128, 129, 132, 75, 145, 178, 206, 239, 254, 254 /* long bits */
            }),

            new MVContext(new short[] {
                    /* same for column */
                    164, /* is short */
                    128, /**/
                    204, 170, 119, 235, 140, 230, 228, /**/
                    128, 130, 130, 74, 148, 180, 203, 236, 254, 254 /* long bits */
            }) };

    public static final MVContext[] vp8_mv_update_probs = {

            new MVContext(new short[] { 237, 246, 253, 253, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 250, 250,
                    252, 254, 254 }),

            new MVContext(new short[] { 231, 243, 245, 253, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 251, 251,
                    254, 254, 254 }) };

    public FullAccessIntArrPointer prob = new FullAccessIntArrPointer(EntropyMV.MVPcount); /* often come in row, col pairs */

    public MVContext(short[] data) {
        prob.memcopyin(0, data, 0, prob.size());
    }

    public MVContext(MVContext other) {
        prob.memcopyin(0, other.prob, 0, prob.size());
    }
}
