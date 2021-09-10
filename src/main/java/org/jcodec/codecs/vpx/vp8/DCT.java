package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;

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
public class DCT {
    public static interface TRANSFORM {
        void call(PositionableIntArrPointer input, FullAccessIntArrPointer output, int pitch);
    }

    public static final TRANSFORM vp8_short_fdct8x4 = new TRANSFORM() {
        @Override
        public void call(final PositionableIntArrPointer input, final FullAccessIntArrPointer output, final int pitch) {
            fdct4x4(input, output, pitch);
            input.incBy(4);
            output.incBy(16);
            fdct4x4(input, output, pitch);
            input.incBy(-4);
            output.incBy(-16);
        }
    };

    public static final TRANSFORM vp8_short_fdct4x4 = new TRANSFORM() {
        @Override
        public void call(PositionableIntArrPointer input, FullAccessIntArrPointer output, int pitch) {
            fdct4x4(input, output, pitch);
        }
    };

    public static final TRANSFORM vp8_short_walsh4x4 = new TRANSFORM() {
        @Override
        public void call(PositionableIntArrPointer input, FullAccessIntArrPointer output, int pitch) {
            walsh4x4(input, output, pitch);
        }
    };

    public static void fdct4x4(PositionableIntArrPointer input, final FullAccessIntArrPointer output, int pitch) {
        pitch >>= 1;
        input.savePos();
        output.savePos();

        for (int i = 0; i < 4; ++i) {
            final int in0 = input.get();
            final int in1 = input.getRel(1);
            final int in2 = input.getRel(2);
            final int in3 = input.getRel(3);
            final int a1 = ((in0 + in3) << 3);
            final int b1 = ((in1 + in2) << 3);
            final int c1 = ((in1 - in2) << 3);
            final int d1 = ((in0 - in3) << 3);

            output.set((short) (a1 + b1));
            output.setRel(2, (short) (a1 - b1));
            output.setRel(1, (short) ((c1 * 2217 + d1 * 5352 + 14500) >> 12));
            output.setRel(3, (short) ((d1 * 2217 - c1 * 5352 + 7500) >> 12));

            input.incBy(pitch);
            output.incBy(4);
        }
        input.rewindToSaved();
        output.rewindToSaved();
        for (int i = 0; i < 4; ++i) {
            final int in0 = output.get();
            final int in4 = output.getRel(4);
            final int in8 = output.getRel(8);
            final int in12 = output.getRel(12);
            final int a1 = in0 + in12;
            final int b1 = in4 + in8;
            final int c1 = in4 - in8;
            final int d1 = in0 - in12;

            output.set((short) ((a1 + b1 + 7) >> 4));
            output.setRel(8, (short) ((a1 - b1 + 7) >> 4));
            output.setRel(4, (short) (((c1 * 2217 + d1 * 5352 + 12000) >> 16) + (d1 != 0 ? 1 : 0)));
            output.setRel(12, (short) ((d1 * 2217 - c1 * 5352 + 51000) >> 16));
            output.inc();
        }
        output.rewindToSaved();
    }

    public static void walsh4x4(PositionableIntArrPointer input, final FullAccessIntArrPointer output, int pitch) {
        pitch >>= 1;
        input.savePos();
        output.savePos();

        for (int i = 0; i < 4; ++i) {
            final int in0 = input.get();
            final int in1 = input.getRel(1);
            final int in2 = input.getRel(2);
            final int in3 = input.getRel(3);
            final int a1 = ((in0 + in2) << 2);
            final int d1 = ((in1 + in3) << 2);
            final int c1 = ((in1 - in3) << 2);
            final int b1 = ((in0 - in2) << 2);

            output.setAndInc((short) (a1 + d1 + (a1 != 0 ? 1 : 0)));
            output.setAndInc((short) (b1 + c1));
            output.setAndInc((short) (b1 - c1));
            output.setAndInc((short) (a1 - d1));
            input.incBy(pitch);
        }
        input.rewindToSaved();
        output.rewindToSaved();

        for (int i = 0; i < 4; ++i) {
            final int in0 = output.get();
            final int in4 = output.getRel(4);
            final int in8 = output.getRel(8);
            final int in12 = output.getRel(12);
            final int a1 = in0 + in8;
            final int d1 = in4 + in12;
            final int c1 = in4 - in12;
            final int b1 = in0 - in8;

            int a2 = a1 + d1;
            int b2 = b1 + c1;
            int c2 = b1 - c1;
            int d2 = a1 - d1;

            if (a2 < 0)
                a2++;
            if (b2 < 0)
                b2++;
            if (c2 < 0)
                c2++;
            if (d2 < 0)
                d2++;

            output.set((short) ((a2 + 3) >> 3));
            output.setRel(4, (short) ((b2 + 3) >> 3));
            output.setRel(8, (short) ((c2 + 3) >> 3));
            output.setRel(12, (short) ((d2 + 3) >> 3));

            output.inc();
        }
        output.rewindToSaved();
    }
}
