package org.jcodec.codecs.h264;
import static org.junit.Assert.assertArrayEquals;

import org.jcodec.codecs.common.biari.MDecoder;
import org.jcodec.codecs.common.biari.MEncoder;
import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.jcodec.codecs.h264.io.CABAC;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.CAVLCUtil;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.util.BinUtil;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.VLC;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ResidualBlockTest {

    @Test
    public void testLuma1CAVLC() throws Exception {

        // 0000100 5,3
        String code = "0000100 01110010111101101";
        int[] coeffs = { 0, 3, 0, 1, -1, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 };

        residualCAVLC(code, H264Const.CoeffToken[0], coeffs);
    }

    @Test
    public void testLuma2CAVLC() throws Exception {
        // 0000000110 5,1
        String code = "0000000110 10001001000010111001100";
        int[] coeffs = { -2, 4, 3, -3, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        residualCAVLC(code, H264Const.CoeffToken[0], coeffs);
    }

    @Test
    public void testLuma3CAVLC() throws Exception {
        // 00011 3,3
        String code = "00011 10001110010";
        int[] coeffs = { 0, 0, 0, 1, 0, 1, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0 };

        residualCAVLC(code, H264Const.CoeffToken[0], coeffs);
    }

    @Test
    public void testChromaAC3CAVLC() throws Exception {
        // 000000100 7,3
        String code = "000000100 000 1 010 0011 10 101 00";

        int[] coeffs = { 1, -3, 2, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0 };

        residualCAVLC(code, H264Const.CoeffToken[0], coeffs);
    }

    private int[] zigzag(int[] src, int[] tab) {
        int[] result = new int[src.length];
        for (int i = 0; i < src.length; i++)
            result[tab[i]] = src[i];
        return result;
    }

    private void residualCAVLC(String code, VLC coeffTokenTab, int[] expected) throws IOException {
        BitReader reader = BitReader.createBitReader(ByteBuffer.wrap(BinUtil.binaryStringToBytes(code)));

        CAVLC cavlc = new CAVLC(new SeqParameterSet(), new PictureParameterSet(), 4, 4);
        int[] actual = new int[expected.length];
        CAVLCUtil.readCoeffs(reader, coeffTokenTab, H264Const.totalZeros16, actual, 0, 16, CoeffTransformer.zigzag4x4);

        assertArrayEquals(zigzag(expected, CoeffTransformer.zigzag4x4), actual);
    }

    @Test
    public void testLumaCABAC() {
        int bits[] = { 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 1,
                1, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 0, 1 };
        int m[] = { 134, 195, 135, 196, 136, 197, 137, 198, 138, 199, 139, 200, 140, 201, 141, 202, 142, 203, 143, 144,
                145, 206, 146, 207, 248, -1, 249, -1, 250, -1, 251, -1, 251, 252, -1, 247, 253, 253, -1, 247, 254, -1,
                247, -1, 247, -1, 247, 255, 255, 255, -1, 247, 256, -1 };
        int coeffs[] = { -2, -1, 1, 0, 4, 2, -1, 0, -3, -1, 1, 0, 2, 1, 0, 0 };
        int[] actual = new int[16];
        MDecoder decoder = new MockMDecoder(bits, m);

        new CABAC(1).readCoeffs(decoder, CABAC.BlockType.LUMA_16, actual, 0, 16, new int[] { 0, 4, 1, 2, 5, 8, 12, 9,
                6, 3, 7, 10, 13, 14, 11, 15 }, H264Const.identityMapping16, H264Const.identityMapping16);

        assertArrayEquals(coeffs, actual);

        MEncoder encoder = new MockMEncoder(bits, m);
        new CABAC(1).writeCoeffs(encoder, CABAC.BlockType.LUMA_16, actual, 0, 16, new int[] { 0, 4, 1, 2, 5, 8, 12, 9,
                6, 3, 7, 10, 13, 14, 11, 15 });
    }

    @Test
    public void testChromaDCCABAC() {
        int bits[] = { 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 0, 1 };

        int m[] = { 149, 210, 150, 211, 151, 212, 258, 262, 262, -1, 257, 263, 263, 263, 263, 263, 263, -1, 257, 264,
                -1, 257, 265, 265, 265, 265, -1 };

        int coeffs[] = { -5, 2, 7, -3 };
        int[] actual = new int[4];
        MDecoder decoder = new MockMDecoder(bits, m);

        new CABAC(1).readCoeffs(decoder, CABAC.BlockType.CHROMA_DC, actual, 0, 4, new int[] { 0, 1, 2, 3 }, H264Const.identityMapping16, H264Const.identityMapping16);

        assertArrayEquals(coeffs, actual);

        MEncoder encoder = new MockMEncoder(bits, m);
        new CABAC(1).writeCoeffs(encoder, CABAC.BlockType.CHROMA_DC, actual, 0, 4, new int[] { 0, 1, 2, 3 });
    }

    @Test
    public void testAC15CABAC() {
        int bits[] = { 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1,
                0, 0 };

        int m[] = { 152, 213, 153, 154, 155, 216, 156, 157, 158, 219, 159, 220, 160, 161, 162, 223, 163, 224, 164, 225,
                267, -1, 268, -1, 269, -1, 270, -1, 270, -1, 270, -1, 270, -1 };

        int coeffs[] = { 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 1, 0, 0, -1, -1, 0 };
        int[] actual = new int[16];
        MDecoder decoder = new MockMDecoder(bits, m);

        new CABAC(1).readCoeffs(decoder, CABAC.BlockType.CHROMA_AC, actual, 1, 15, new int[] { 0, 4, 1, 2, 5, 8, 12, 9,
                6, 3, 7, 10, 13, 14, 11, 15 }, H264Const.identityMapping16, H264Const.identityMapping16);

        assertArrayEquals(coeffs, actual);

        MEncoder encoder = new MockMEncoder(bits, m);
        new CABAC(1).writeCoeffs(encoder, CABAC.BlockType.CHROMA_AC, actual, 1, 15, new int[] { 0, 4, 1, 2, 5, 8, 12,
                9, 6, 3, 7, 10, 13, 14, 11, 15 });
    }

    @Test
    public void testLumaDCCabac() {
        int[] coeffs = new int[] { 46, -10, -5, -27, -18, -4, 7, 9, 19, -3, -37, 14, -17, -15, -43, -28 };

        int bits[] = { 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1,
                1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0 };

        int m[] = { 105, 166, 106, 167, 107, 168, 108, 169, 109, 170, 110, 171, 111, 172, 112, 173, 113, 174, 114, 175,
                115, 176, 116, 177, 117, 178, 118, 179, 119, 180, 228, 232, 232, 232, 232, 232, 232, 232, 232, 232,
                232, 232, 232, 232, -1, -1, -1, -1, -1, -1, -1, -1, 227, 233, 233, 233, 233, 233, 233, 233, 233, 233,
                233, 233, 233, 233, -1, 227, 234, 234, 234, 234, 234, 234, 234, 234, 234, 234, 234, 234, 234, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, 227, 235, 235, 235, 235, 235, 235, 235, 235, 235, 235, 235, 235, 235,
                -1, -1, 227, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, 227, 236, 236, 236, 236, 236, 236, 236, 236, -1, 227, 236, 236, 236, 236, 236, 236,
                236, 236, 236, 236, 236, 236, 236, -1, -1, -1, -1, -1, -1, -1, -1, 227, 236, 236, 236, 236, 236, 236,
                -1, 227, 236, 236, -1, 227, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, -1, -1,
                -1, -1, 227, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, -1, -1, -1, -1, -1, -1,
                227, 236, 236, 236, -1, 227, 236, 236, 236, 236, -1, 227, 236, 236, 236, 236, 236, 236, 236, 236, 236,
                -1, 227, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, -1, -1, -1, -1, -1, -1, 227,
                236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1 };

        int[] actual = new int[16];
        MDecoder decoder = new MockMDecoder(bits, m);

        new CABAC(1).readCoeffs(decoder, CABAC.BlockType.LUMA_16_DC, actual, 0, 16, new int[] { 0, 4, 1, 2, 5, 8, 12,
                9, 6, 3, 7, 10, 13, 14, 11, 15 }, H264Const.identityMapping16, H264Const.identityMapping16);

        assertArrayEquals(coeffs, actual);

        MEncoder encoder = new MockMEncoder(bits, m);
        new CABAC(1).writeCoeffs(encoder, CABAC.BlockType.LUMA_16_DC, actual, 0, 16, new int[] { 0, 4, 1, 2, 5, 8, 12,
                9, 6, 3, 7, 10, 13, 14, 11, 15 });
    }
}