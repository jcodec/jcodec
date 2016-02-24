package org.jcodec.codecs.prores;

import static org.jcodec.common.ArrayUtil.randomByteArray;
import static org.jcodec.common.ArrayUtil.randomIntArray;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jcodec.codecs.prores.ProresEncoder.Profile;
import org.jcodec.common.ArrayUtil;
import org.jcodec.common.dct.SimpleIDCT10Bit;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Picture8Bit;
import org.junit.Assert;
import org.junit.Test;

public class TestProresEncoder {

    public void testSlice1() throws Exception {
        int[] slice = new int[] { 4096, 0, 0, 0, 8, 0, 0, 0, 16, 16, 8, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 8, 16,
                8, 8, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0,
                0, 0, 0, 0, 4048, -48, -24, -8, 0, 8, 0, -8, 8, 8, 8, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -10, 0, 8, 0, 0, -8, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0 };
        int[] qMat = new int[64];
        Arrays.fill(qMat, 4);

        onePlaneTest(ByteBuffer.allocate(1024), slice, 2, qMat);
    }

    public void testSlice2() throws Exception {
        int[] slice = new int[] { 2088, 1248, -208, -328, 144, 80, -88, 16, 232, 184, -48, -56, 48, 48, -8, -8, -64,
                -48, 16, 32, 16, -8, -16, -16, 16, 32, 16, 8, 0, -8, -8, 0, 56, 56, 0, -16, -24, -16, -10, 0, -8, -8,
                -24, -32, -16, 0, 10, 12, 0, -8, -8, 8, 20, 10, 0, 0, 24, 16, 8, 24, 30, 12, -14, -28, 512, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1640, 904, -152, -224, 136,
                72, -72, 0, 96, 80, -8, -16, 8, 8, 0, 0, -56, -48, 8, 24, 0, -8, 0, 0, 24, 16, 8, 0, 0, -16, -16, 0, 0,
                8, 0, 0, 0, 0, -10, -10, -8, -16, -8, -16, -16, -10, -20, 0, 0, 0, 0, 8, 0, -10, -12, -14, 0, 0, -8, 0,
                -10, -12, -14, -14, 504, -8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0 };
        int[] qMat = new int[64];
        Arrays.fill(qMat, 4);

        onePlaneTest(ByteBuffer.allocate(1024), slice, 4, qMat);
    }

    private void onePlaneTest(ByteBuffer out, int[] slice, int blocksPerSlice, int[] qMat) {

        BitWriter writer = new BitWriter(out);
        ProresEncoder encoder = new ProresEncoder(Profile.HQ, false);

        encoder.writeDCCoeffs(writer, qMat, slice, blocksPerSlice);
        encoder.writeACCoeffs(writer, qMat, slice, blocksPerSlice, ProresConsts.progressive_scan, 64);
        writer.flush();

        System.out.println("result");

        out.flip();

        ProresDecoder decoder = new ProresDecoder();
        BitReader bits = BitReader.createBitReader(out);
        int[] result = new int[blocksPerSlice << 6];
        decoder.readDCCoeffs(bits, qMat, result, blocksPerSlice, 64);
        decoder.readACCoeffs(bits, qMat, result, blocksPerSlice, ProresConsts.progressive_scan, 64, 6);

        Assert.assertArrayEquals(slice, result);
    }

    @Test
    public void testWholeThing() throws Exception {
        byte[] Y = randomByteArray(4096, (byte)1, (byte)254);
        byte[] U = randomByteArray(2048, (byte)1, (byte)254);
        byte[] V = randomByteArray(2048, (byte)1, (byte)254);
        Picture8Bit picture = new Picture8Bit(64, 64, new byte[][] { Y, U, V }, ColorSpace.YUV422);

        ByteBuffer buf = ByteBuffer.allocate(64 * 64 * 6);
        new ProresEncoder(Profile.HQ, false).encodeFrame8Bit(picture, buf);

        ProresDecoder decoder = new ProresDecoder();

        Picture8Bit result = decoder.decodeFrame8Bit(buf, new byte[][] { new byte[4096], new byte[2048], new byte[2048] });

        System.out.println("Y");
        assertByteArrayApproximatelyEquals(Y, result.getPlaneData(0), 20);
        System.out.println("U");
        assertByteArrayApproximatelyEquals(U, result.getPlaneData(1), 20);
        System.out.println("V");
        assertByteArrayApproximatelyEquals(V, result.getPlaneData(2), 20);
    }

    @Test
    public void testIdct() {
        int[] rand = randomIntArray(64, 4, 255);
        byte[] rand8Bit = ArrayUtil.toByteArrayShifted(rand);
        
        int[] out = new int[64];
        SimpleIDCT10Bit.fdct10(rand8Bit, 0, out);
        for (int i = 0; i < 64; i++) {
            out[i] >>= 2;
            rand[i] <<= 2;
        }
        
        SimpleIDCT10Bit.idct10(out, 0);
        assertIntArrayApproximatelyEquals(rand, out, 50);

    }
    
    private void assertByteArrayApproximatelyEquals(byte[] rand, byte[] newRand, int threash) {
        int maxDiff = 0;
        for (int i = 0; i < rand.length; i++) {
            int diff = Math.abs(rand[i] - newRand[i]);
            if (diff > maxDiff)
                maxDiff = diff;
        }
        Assert.assertTrue("Maxdiff: " + maxDiff, maxDiff < threash);
    }

    private void assertIntArrayApproximatelyEquals(int[] rand, int[] newRand, int threash) {
        int maxDiff = 0;
        for (int i = 0; i < rand.length; i++) {
            int diff = Math.abs(rand[i] - newRand[i]);
            if (diff > maxDiff)
                maxDiff = diff;
        }
        Assert.assertTrue("Maxdiff: " + maxDiff, maxDiff < threash);
    }

}
