package org.jcodec.codecs.vpx;

import java.util.Arrays;

import org.jcodec.codecs.vpx.vp8.DCT;
import org.jcodec.codecs.vpx.vp8.IDCTllm;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;
import org.junit.Assert;
import org.junit.Test;

public class VP8DCTTest {

    @Test
    public void testDCT() {
        short[] input = { 87, -38, 1, 0, 87, -38, 1, 0, 87, -38, 1, 0, 87, 116, 155, 154 };
        short[] output = { 331, 85, 171, 155, -301, 132, 101, 55, 231, -101, -77, -42, -125, 55, 42, 23 };
        DCT.fdct4x4(new PositionableIntArrPointer(input, 0), FullAccessIntArrPointer.toPointer(input), 8);

        Assert.assertArrayEquals(output, input);
    }

    @Test
    public void testWalsh() {
        short[] input = { -696, 625, 104, 856, 856, 163, 625, 656, -896, 856, -128, 856, 856, 336, 856, -896 };
        short[] output = { 2515, -414, -922, -937, 674, -878, 1077, -473, -473, 615, -878, 1136, -937, -1384, -414,
                -3871 };
        DCT.walsh4x4(new PositionableIntArrPointer(input, 0), FullAccessIntArrPointer.toPointer(input), 8);

        Assert.assertArrayEquals(output, input);
    }

    @Test
    public void testIDCT() {
        short[] input = { 331, 85, 171, 155, -301, 132, 101, 55, 231, -101, -77, -42, -125, 55, 42, 23 };
        short[] output = { 87, -38, 1, 0, 87, -38, 1, 0, 87, -38, 1, 0, 87, 116, 155, 154 };
        short[] pred = new short[16];
        Arrays.fill(pred, 0, 14, (short)128);
        IDCTllm.vp8_short_idct4x4llm(new PositionableIntArrPointer(input, 0), new PositionableIntArrPointer(pred, 0), 4,
                FullAccessIntArrPointer.toPointer(input), 4);
        for (int i = 0; i < input.length - 2; i++)
            input[i] -= 128;

        Assert.assertArrayEquals(output, input);
    }

    @Test
    public void testIWalsh() {
        short[] input = { 2515, -414, -922, -937, 674, -878, 1077, -473, -473, 615, -878, 1136, -937, -1384, -414,
                -3871 };
        short[] output = { -696, 625, 104, 856, 856, 163, 625, 655, -896, 856, -128, 856, 856, 336, 856, -896 };
        short[] realOut = new short[256];

        IDCTllm.vp8_short_inv_walsh4x4(new PositionableIntArrPointer(input, 0),
                FullAccessIntArrPointer.toPointer(realOut));
        for (int i = 0; i < input.length; i++) {
            input[i] = realOut[i * 16];
        }
        Assert.assertArrayEquals(output, input);
    }

}
