package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;
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
public class IDCTllm {
    /****************************************************************************
     * Notes:
     *
     * This implementation makes use of 16 bit fixed point verio of two multiply
     * constants: 1. sqrt(2) * cos (pi/8) 2. sqrt(2) * sin (pi/8) Becuase the first
     * constant is bigger than 1, to maintain the same 16 bit fixed point precision
     * as the second one, we use a trick of x * a = x + x*(a-1) so x * sqrt(2) * cos
     * (pi/8) = x + x * (sqrt(2) *cos(pi/8)-1).
     **************************************************************************/
    public static final int cospi8sqrt2minus1 = 20091;
    public static final int sinpi8sqrt2 = 35468;

    public static void vp8_short_idct4x4llm(final PositionableIntArrPointer ip,
            final PositionableIntArrPointer pred_ptr, final int pred_stride, final FullAccessIntArrPointer dst_ptr,
            final int dst_stride) {
        final FullAccessIntArrPointer op = vp8_short_idct4x4NoAdd(ip);
        for (int r = 0; r < 4; ++r) {
            final int basePred = r * pred_stride;
            final int baseDst = r * dst_stride;
            for (int c = 0; c < 4; ++c) {
                dst_ptr.setRel(baseDst + c,
                        CommonUtils.clipPixel((short) (op.getRel(c) + pred_ptr.getRel(basePred + c))));
            }
            op.incBy(4);
        }
    }

    public static FullAccessIntArrPointer vp8_short_idct4x4NoAdd(final PositionableIntArrPointer ip) {
        int inPos = ip.getPos();
        FullAccessIntArrPointer op = new FullAccessIntArrPointer(16);
        final int shortpitch = 4;

        for (int i = 0; i < 4; ++i) {
            final int a1 = ip.get() + ip.getRel(8);
            final int b1 = ip.get() - ip.getRel(8);

            int temp1 = (ip.getRel(4) * sinpi8sqrt2) >> 16;
            int temp2 = ip.getRel(12) + ((ip.getRel(12) * cospi8sqrt2minus1) >> 16);
            final int c1 = temp1 - temp2;

            temp1 = ip.getRel(4) + ((ip.getRel(4) * cospi8sqrt2minus1) >> 16);
            temp2 = (ip.getRel(12) * sinpi8sqrt2) >> 16;
            final int d1 = temp1 + temp2;

            op.set((short) (a1 + d1));
            op.setRel(shortpitch * 3, (short) (a1 - d1));

            op.setRel(shortpitch, (short) (b1 + c1));
            op.setRel(shortpitch << 1, (short) (b1 - c1));

            ip.inc();
            op.inc();
        }
        ip.setPos(inPos);

        op.rewind();

        for (int i = 0; i < 4; ++i) {
            final int a1 = op.get() + op.getRel(2);
            final int b1 = op.get() - op.getRel(2);

            int temp1 = (op.getRel(1) * sinpi8sqrt2) >> 16;
            int temp2 = op.getRel(3) + ((op.getRel(3) * cospi8sqrt2minus1) >> 16);
            final int c1 = temp1 - temp2;

            temp1 = op.getRel(1) + ((op.getRel(1) * cospi8sqrt2minus1) >> 16);
            temp2 = (op.getRel(3) * sinpi8sqrt2) >> 16;
            final int d1 = temp1 + temp2;

            op.set((short) ((a1 + d1 + 4) >> 3));
            op.setRel(3, (short) ((a1 - d1 + 4) >> 3));

            op.setRel(1, (short) ((b1 + c1 + 4) >> 3));
            op.setRel(2, (short) ((b1 - c1 + 4) >> 3));

            op.incBy(shortpitch);
        }
        op.rewind();
        return op;
    }

    static void vp8_dc_only_idct_add(final int input_dc, final PositionableIntArrPointer pred_ptr,
            final int pred_stride, final FullAccessIntArrPointer dst_ptr, final int dst_stride) {
        final int a1 = ((input_dc + 4) >> 3);

        for (int r = 0; r < 4; ++r) {
            final int dstBase = r * dst_stride;
            final int predBase = r * pred_stride;
            for (int c = 0; c < 4; ++c) {
                dst_ptr.setRel(dstBase + c, CommonUtils.clipPixel((short) (a1 + pred_ptr.getRel(predBase + c))));
            }
        }
    }

    public static void vp8_short_inv_walsh4x4(PositionableIntArrPointer ip, FullAccessIntArrPointer mb_dqcoeff) {
        int inPos = ip.getPos();
        FullAccessIntArrPointer op = new FullAccessIntArrPointer(16);
        int i;
        int a1, b1, c1, d1;
        int a2, b2, c2, d2;

        for (i = 0; i < 4; ++i) {
            a1 = ip.get() + ip.getRel(12);
            b1 = ip.getRel(4) + ip.getRel(8);
            c1 = ip.getRel(4) - ip.getRel(8);
            d1 = ip.get() - ip.getRel(12);

            op.set((short) (a1 + b1));
            op.setRel(4, (short) (c1 + d1));
            op.setRel(8, (short) (a1 - b1));
            op.setRel(12, (short) (d1 - c1));
            ip.inc();
            op.inc();
        }
        op.rewind();
        ip.setPos(inPos);

        for (i = 0; i < 4; ++i) {
            a1 = op.get() + op.getRel(3);
            b1 = op.getRel(1) + op.getRel(2);
            c1 = op.getRel(1) - op.getRel(2);
            d1 = op.get() - op.getRel(3);

            a2 = a1 + b1;
            b2 = c1 + d1;
            c2 = a1 - b1;
            d2 = d1 - c1;

            op.setAndInc((short) ((a2 + 3) >> 3));
            op.setAndInc((short) ((b2 + 3) >> 3));
            op.setAndInc((short) ((c2 + 3) >> 3));
            op.setAndInc((short) ((d2 + 3) >> 3));
        }

        op.rewind();
        for (i = 0; i < 16; ++i) {
            mb_dqcoeff.setRel(i << 4, op.getRel(i));
        }
    }

    static void vp8_short_inv_walsh4x4_1(ReadOnlyIntArrPointer input, FullAccessIntArrPointer mb_dqcoeff) {
        int i;
        int a1;

        a1 = ((input.get() + 3) >> 3);
        for (i = 0; i < 16; ++i) {
            mb_dqcoeff.setRel(i << 4, (short) a1);
        }
    }
}
