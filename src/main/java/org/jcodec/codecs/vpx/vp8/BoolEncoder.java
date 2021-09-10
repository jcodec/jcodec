package org.jcodec.codecs.vpx.vp8;

import org.jcodec.codecs.vpx.VPXConst;
import org.jcodec.codecs.vpx.vp8.enums.CodecError;
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
public class BoolEncoder {

    public static final int[] vp8_prob_cost = { 2047, 2047, 1791, 1641, 1535, 1452, 1385, 1328, 1279, 1235, 1196, 1161,
            1129, 1099, 1072, 1046, 1023, 1000, 979, 959, 940, 922, 905, 889, 873, 858, 843, 829, 816, 803, 790, 778,
            767, 755, 744, 733, 723, 713, 703, 693, 684, 675, 666, 657, 649, 641, 633, 625, 617, 609, 602, 594, 587,
            580, 573, 567, 560, 553, 547, 541, 534, 528, 522, 516, 511, 505, 499, 494, 488, 483, 477, 472, 467, 462,
            457, 452, 447, 442, 437, 433, 428, 424, 419, 415, 410, 406, 401, 397, 393, 389, 385, 381, 377, 373, 369,
            365, 361, 357, 353, 349, 346, 342, 338, 335, 331, 328, 324, 321, 317, 314, 311, 307, 304, 301, 297, 294,
            291, 288, 285, 281, 278, 275, 272, 269, 266, 263, 260, 257, 255, 252, 249, 246, 243, 240, 238, 235, 232,
            229, 227, 224, 221, 219, 216, 214, 211, 208, 206, 203, 201, 198, 196, 194, 191, 189, 186, 184, 181, 179,
            177, 174, 172, 170, 168, 165, 163, 161, 159, 156, 154, 152, 150, 148, 145, 143, 141, 139, 137, 135, 133,
            131, 129, 127, 125, 123, 121, 119, 117, 115, 113, 111, 109, 107, 105, 103, 101, 99, 97, 95, 93, 92, 90, 88,
            86, 84, 82, 81, 79, 77, 75, 73, 72, 70, 68, 66, 65, 63, 61, 60, 58, 56, 55, 53, 51, 50, 48, 46, 45, 43, 41,
            40, 38, 37, 35, 33, 32, 30, 29, 27, 25, 24, 22, 21, 19, 18, 16, 15, 13, 12, 10, 9, 7, 6, 4, 3, 1, 1 };

    private int count;
    private int range;
    private long lowvalue;
    private int pos;

    private FullAccessIntArrPointer buffer;
    private ReadOnlyIntArrPointer buffer_end;

    public static final int vp8_prob_half = 128;

    public void vp8_encode_bool(final boolean bit, final int probability) {
        encodeBitInRangeLow(bit, 1 + (((range - 1) * probability) >> 8));
        int shift = VPXConst.vp8Norm[range];
        updateRangeCount(shift);
        if (count >= 0) {
            final int offset = shift - count;
            adjustBuffer(offset);
            shift = writeCurrEncodedByte(offset);
        }
        lowvalue <<= shift;
    }

    public void vp8_encode_extra(int e) {
        encodeBitInRangeLow((e & 1) > 0, (range + 1) >> 1);
        updateRangeCount(1);
        adjustBuffer(1);
        if (count == 0) {
            writeCurrEncodedByte(1);
        } else {
            lowvalue <<= 1;
        }
    }

    private void updateRangeCount(final int shift) {
        range <<= shift;
        count += shift;
    }

    private void encodeBitInRangeLow(final boolean bit, final int split) {
        if (bit) {
            lowvalue += split;
            range -= split;
        } else {
            range = split;
        }
    }

    private int writeCurrEncodedByte(final int offset) {
        final int shift = count;
        buffer.incBy(pos);
        validate_buffer(buffer, 1, buffer_end);
        buffer.incBy(-pos);
        buffer.setRel(pos++, (short) ((lowvalue >> (24 - offset) & 0xff)));
        lowvalue <<= offset;
        lowvalue &= 0xffffff;
        count -= 8;
        return shift;
    }

    private void adjustBuffer(final int offset) {
        if (((lowvalue << (offset - 1)) & 0x80000000) != 0) {
            int x = pos - 1;

            while (x >= 0 && buffer.getRel(x) == 0xff) {
                buffer.setRel(x, (short) 0);
                x--;
            }

            buffer.setRel(x, (short) (buffer.getRel(x) + 1));
        }
    }

    public static void validate_buffer(final ReadOnlyIntArrPointer start, int shift, final ReadOnlyIntArrPointer end) {
        final PositionableIntArrPointer mid = PositionableIntArrPointer.makePositionableAndInc(start, shift);
        if (start.pointerDiff(mid) < 0 || mid.pointerDiff(end) < 0) {
            VP8Exception.vp8_internal_error(CodecError.CORRUPT_FRAME,
                    "Truncated packet or corrupt partition ");
        }
    }

    public void vp8_encode_value(final int data, final int bits) {
        for (int bit = bits - 1; bit >= 0; bit--) {
            vp8_encode_bool((1 & (data >> bit)) == 1, BoolEncoder.vp8_prob_half);
        }
    }

    public void vp8_start_encode(final FullAccessIntArrPointer source, final ReadOnlyIntArrPointer end) {
        lowvalue = 0;
        range = 255;
        count = -24;
        buffer = source.shallowCopy();
        buffer_end = end;
        pos = 0;
    }

    public void vp8_stop_encode() {
        for (int i = 0; i < 32; ++i)
            vp8_encode_bool(false, BoolEncoder.vp8_prob_half);
    }

    public int getPos() {
        return pos;
    }

    public void vp8_write_bit(boolean bit) {
        vp8_encode_bool(bit, vp8_prob_half);
    }

}
