package org.jcodec.codecs.vpx;

import org.jcodec.codecs.vpx.vp8.BoolEncoder;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.junit.Assert;
import org.junit.Test;

public class BooleanEncoderTest {

    @Test
    public void testOne() {
        short[] expected = { 0x0a, 0x8f, 0xef, 0x00, 0x14, 0xb1, 0xb0, 0x22, 0xfa, 0x00, 0x01, 0x86, 0x95, 0xdb, 0x47,
                0xbb, 0x7a, 0xde, 0xcd, 0x01, 0x0e, 0x01, 0xa8, 0xda, 0x14, 0xa8, 0xc9, 0x01, 0x72, 0x00, 0x48, 0x00,
                0xc0, 0x00, 0xc6, 0xff, 0xa8, 0x8d, 0x13, 0x9b, 0x8c, 0xe4, 0xc3, 0xb8, 0x1a, 0x00 };

        int[] probs = { 107, 50, 100, 102, 116, 25, 42, 98, 35, 70, 61, 80, 46, 65, 121, 117, 81, 91, 18, 77, 2, 31, 17,
                102, 20, 51, 16, 13, 127, 27, 65, 107, 78, 37, 81, 67, 63, 124, 37, 98, 67, 98, 51, 114, 36, 45, 103,
                117, 8, 121, 67, 11, 24, 84, 113, 44, 8, 2, 58, 8, 30, 124, 115, 108, 34, 69, 48, 97, 65, 85, 68, 5, 56,
                119, 119, 92, 36, 94, 81, 45, 88, 21, 56, 112, 106, 42, 29, 114, 44, 87, 122, 75, 84, 109, 56, 118, 50,
                104, 87, 116, 61, 27, 121, 117, 18, 112, 82, 55, 79, 35, 100, 39, 57, 28, 24, 35, 71, 53, 21, 116, 13,
                16, 63, 97, 126, 119, 87, 49, 95, 47, 37, 29, 74, 31, 19, 93, 16, 101, 21, 95, 9, 121, 6, 66, 22, 30,
                102, 93, 84, 123, 81, 97, 11, 17, 66, 10, 8, 26, 59, 104, 73, 96, 6, 20, 127, 26, 113, 16, 127, 6, 111,
                9, 0, 118, 76, 23, 20, 50, 116, 104, 45, 70, 74, 57, 87, 12, 67, 96, 38, 127, 73, 112, 95, 80, 4, 95,
                106, 118, 111, 106, 125, 95, 115, 125, 85, 63, 20, 106, 113, 9, 83, 31, 80, 29, 89, 40, 42, 29, 9, 81,
                28, 83, 65, 124, 35, 69, 92, 14, 60, 75, 120, 57, 43, 108, 55, 0, 44, 76, 106, 29, 86, 61, 61, 39, 91,
                23, 79, 5, 52, 89 };
        int[] bits = { 0, 0, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1,
                0, 0, 1, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 0,
                0, 1, 0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 0,
                0, 0, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0,
                0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1,
                0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1,
                1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0,
                0, 1, 0, 0, 1, 0, 1, 1 };

        BoolEncoder bh = new BoolEncoder();
        FullAccessIntArrPointer fia = new FullAccessIntArrPointer(512);
        bh.vp8_start_encode(fia, fia.shallowCopyWithPosInc(511).readOnly());
        for (int i = 0; i < probs.length; i++)
            bh.vp8_encode_bool(bits[i] == 1, probs[i]);
        bh.vp8_stop_encode();
        byte[] result = new byte[bh.getPos()];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) fia.getRel(i);
        }
        Assert.assertArrayEquals(asByteArray(expected), result);
    }

    byte[] asByteArray(short[] src) {
        byte[] result = new byte[src.length];
        for (int i = 0; i < src.length; i++) {
            result[i] = (byte) src[i];
        }
        return result;
    }
}
