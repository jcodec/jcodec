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
public class QuantDetails {
    public FullAccessIntArrPointer[] quant = new FullAccessIntArrPointer[OnyxInt.QINDEX_RANGE];// short
    public FullAccessIntArrPointer[] quant_shift = new FullAccessIntArrPointer[OnyxInt.QINDEX_RANGE];// short
    public FullAccessIntArrPointer[] zbin = new FullAccessIntArrPointer[OnyxInt.QINDEX_RANGE];// short
    public FullAccessIntArrPointer[] round = new FullAccessIntArrPointer[OnyxInt.QINDEX_RANGE];// short
    public FullAccessIntArrPointer[] zrun_zbin_boost = new FullAccessIntArrPointer[OnyxInt.QINDEX_RANGE];// short
    public FullAccessIntArrPointer[] quant_fast = new FullAccessIntArrPointer[OnyxInt.QINDEX_RANGE];// short

    public QuantDetails() {
        FullAccessIntArrPointer[][] temp = { quant, quant_shift, zbin, round, zrun_zbin_boost, quant_fast };
        for (FullAccessIntArrPointer[] q : temp) {
            for (int i = 0; i < q.length; i++) {
                q[i] = new FullAccessIntArrPointer(16);
            }
        }
    }

    public void shallowCopyTo(Block where, int Q) {
        where.quant = quant[Q].shallowCopy();
        where.quant_fast = quant_fast[Q].shallowCopy();
        where.quant_shift = quant_shift[Q].shallowCopy();
        where.zbin = zbin[Q].shallowCopy();
        where.round = round[Q].shallowCopy();
        where.zrun_zbin_boost = zrun_zbin_boost[Q].shallowCopy();

    }

}