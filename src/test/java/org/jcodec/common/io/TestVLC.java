package org.jcodec.common.io;
import org.junit.Assert;
import org.junit.Test;

import js.io.IOException;
import js.nio.ByteBuffer;

public class TestVLC {
    static String[] codes = new String[] { "101111110001", "1000000", "101111100101", "1000001", "101111110100",
            "1000010", "101111101011", "1000101", "1001000", "1001001", "101111101010", "1011000", "1001100",
            "1001101", "101111100100", "1001110", "101111110101", "1001111", "11", "1010001", "101111100110",
            "1010010", "1010011", "1011111111101", "1010101", "0", "101111111010", "1010110", "1010111",
            "101111111000", "101111100111", "1000011", "1011001", "101111100010", "1011111111111", "101111111101",
            "1011101", "101111100000", "1011111111110", "101111100001", "1000100", "101111100011", "1000111",
            "101111101100", "1011011", "101111101000", "1010000", "101111101001", "101111101101", "1000110",
            "101111101110", "101111101111", "1001011", "101111110000", "101111110011", "1001010", "1011010",
            "101111110110", "101111110111", "1010100", "101111111001", "101111111011", "1011100", "101111111100",
            "1011111111100", "1011110", "101111110010" };

    @Test
    public void testVLC1() throws IOException {

        VLC vlc = VLC.createVLC(codes);

        for (int i = 0; i < codes.length; i++) {
            ByteBuffer buf = ByteBuffer.allocate(1024);
            BitWriter out = new BitWriter(buf);
            vlc.writeVLC(out, i);
            out.flush();

            buf.flip();
            BitReader _in = BitReader.createBitReader(buf);
            int readVLC = vlc.readVLC(_in);

            Assert.assertEquals(readVLC, i);
        }
    }

    @Test
    public void testVLC2() throws IOException {

        VLC vlc = VLC.createVLC(codes);
        ByteBuffer buf = ByteBuffer.allocate(1024);
        
        BitWriter out = new BitWriter(buf);
        for (int i = 0; i < codes.length; i++) {
            vlc.writeVLC(out, i);
        }
        out.flush();
        buf.flip();
        
        BitReader _in = BitReader.createBitReader(buf);
        for (int i = 0; i < codes.length; i++) {
            Assert.assertEquals(i, vlc.readVLC(_in));
        }
    }

    @Test
    public void testVLC3() throws IOException {

        int decoded[] = new int[] { 51, 58, 24, 48, 45, 1, 34, 33, 63, 15, 16, 24, 28, 31, 33, 7, 54, 28, 2, 30, 1, 42,
                11, 7, 64, 39, 41, 50, 65, 10, 34, 36, 23, 40, 17, 28, 38, 57, 59, 25, 60, 8, 32, 20, 19, 53, 23, 26,
                37, 14, 9, 56, 60, 57, 36, 29, 62, 63, 35, 24, 44, 3, 23, 62, 38, 49, 15, 56, 24, 48, 50, 45, 65, 1,
                12, 1, 10, 6, 51, 10, 21, 41, 57, 34, 27, 65, 31, 64, 17, 64, 45, 46, 66, 31, 16, 18, 28, 66, 21, 51 };

        VLC vlc = VLC.createVLC(codes);

        // vlc.printTable();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        BitWriter bout = new BitWriter(buf);
        for (int i : decoded) {
            vlc.writeVLC(bout, i);
        }
        buf.flip();
        
        BitReader bis = BitReader.createBitReader(buf);
        int[] actual = new int[decoded.length];
        for (int i = 0; i < decoded.length; i++) {
            actual[i] = vlc.readVLC(bis);
        }

        Assert.assertArrayEquals(decoded, actual);
    }
}
