package org.jcodec.codecs.prores;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.jcodec.codecs.prores.ProresEncoder.Profile;
import org.jcodec.common.dct.DCTRef;
import org.jcodec.common.dct.SimpleIDCT10Bit;
import org.jcodec.common.io.BitstreamReader;
import org.jcodec.common.io.BitstreamWriter;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
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

        onePlaneTest(slice, 2, qMat);
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

        onePlaneTest(slice, 4, qMat);
    }

    private void onePlaneTest(int[] slice, int blocksPerSlice, int[] qMat) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        BitstreamWriter writer = new BitstreamWriter(out);
        ProresEncoder encoder = new ProresEncoder(Profile.HQ);

        encoder.writeDCCoeffs(writer, qMat, slice, blocksPerSlice);
        encoder.writeACCoeffs(writer, qMat, slice, blocksPerSlice, ProresConsts.progressive_scan, 64);
        writer.flush();

        System.out.println("result");

        byte[] byteArray = out.toByteArray();

        ByteArrayInputStream input = new ByteArrayInputStream(byteArray);

        ProresDecoder decoder = new ProresDecoder();
        BitstreamReader bits = new BitstreamReader(input);
        int[] result = new int[blocksPerSlice << 6];
        decoder.readDCCoeffs(bits, qMat, result, blocksPerSlice);
        decoder.readACCoeffs(bits, qMat, result, blocksPerSlice, ProresConsts.progressive_scan, 64);

        Assert.assertArrayEquals(slice, result);
    }

    @Test
    public void testWholeThing() throws Exception {
        int[] Y = randomArray(4096, 4, 1019);
        int[] U = randomArray(2048, 4, 1019);
        int[] V = randomArray(2048, 4, 1019);
        Picture picture = new Picture(64, 64, new int[][] {Y, U, V}, ColorSpace.YUV422_10);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ProresEncoder(Profile.HQ).encodeFrame(out, picture);

        ProresDecoder decoder = new ProresDecoder();

        Buffer buffer = new Buffer(out.toByteArray());
        Picture result = decoder.decodeFrame(buffer, new int[][] { new int[4096], new int[2048], new int[2048] });

        System.out.println("Y");
        diffArray(Y, result.getPlaneData(0));
        System.out.println("U");
        diffArray(U, result.getPlaneData(1));
        System.out.println("V");
        diffArray(V, result.getPlaneData(2));

        // Assert.assertArrayEquals(Y, result.getY());
        // Assert.assertArrayEquals(U, result.getCb());
        // Assert.assertArrayEquals(V, result.getCr());
    }

    private int[] randomArray(int size, int off, int max) {
        int width = max - off;
        int[] result = new int[size];
        for (int i = 0; i < size; i++)
            result[i] = (int) ((Math.random() * width) % width) + off;
        return result;
    }

    public void testIdct() {
        int[] rand = randomArray(64, 4, 1019);
        int[] newRand = new int[rand.length];
        System.arraycopy(rand, 0, newRand, 0, rand.length);
        DCTRef.fdct(rand, 0);
        for (int i = 0; i < 64; i++)
            rand[i] >>= 2;
        SimpleIDCT10Bit.idct10(rand, 0);
        diffArray(rand, newRand);

    }

    private void diffArray(int[] rand, int[] newRand) {
        for (int i = 0; i < rand.length; i++)
            System.out.println((Math.abs(rand[i] - newRand[i])));
    }

}
