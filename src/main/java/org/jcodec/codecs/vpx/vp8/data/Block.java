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
public class Block {
    public static final int MAX_MODES = 20;

    /* 16 Y blocks, 4 U blocks, 4 V blocks each with 16 entries */
    public FullAccessIntArrPointer src_diff; // short
    public FullAccessIntArrPointer coeff; // short

    /* 16 Y blocks, 4 U blocks, 4 V blocks each with 16 entries */
    FullAccessIntArrPointer quant; // short
    public FullAccessIntArrPointer quant_fast; // short
    public FullAccessIntArrPointer quant_shift; // short
    public FullAccessIntArrPointer zbin; // short
    public FullAccessIntArrPointer zrun_zbin_boost; // short
    public FullAccessIntArrPointer round; // short

    /* Zbin Over Quant value */
    public int zbin_extra; // short

    public FullAccessIntArrPointer base_src; // uchar
    public int src;
    public int src_stride;
    private int base_pos = -1;
    FullAccessIntArrPointer prev_base_src = null;
    private FullAccessIntArrPointer actSrcptr;

    public Block(FullAccessIntArrPointer srcd) {
        src_diff = srcd;
    }

    public FullAccessIntArrPointer getSrcPtr() {
        if (prev_base_src != base_src || base_src.getPos() != base_pos) {
            base_pos = base_src.getPos();
            prev_base_src = base_src;
            actSrcptr = base_src.shallowCopyWithPosInc(src);
        }
        return actSrcptr;
    }
}
