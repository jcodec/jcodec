package org.jcodec.common.io;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jcodec.common.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.System;
import java.nio.ByteBuffer;
import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.syntax.BitStream;
import net.sourceforge.jaad.aac.syntax.IBitStream;
import net.sourceforge.jaad.aac.syntax.NIOBitStream;

public class TestBitReader {
    @Test
    public void testEOF() throws Exception {
        byte[] src = new byte[] { b("10011000") };
        boolean eof = false;
        try {
            BitStream bs = BitStream.createBitStream(src);
            for (int i = 0; i < 10; i++) {
                System.out.println(Integer.toBinaryString(bs.readBits(4)) + " " + bs.getBitsLeft());
            }
        } catch (AACException e) {
            eof = true;
        }
        Assert.assertTrue(eof);
    }

    @Test
    public void testEOF2() throws Exception {
        byte[] src = new byte[] { b("10011000") };
        boolean eof = false;
        try {
            IBitStream bs = new NIOBitStream(BitReader.createBitReader(ByteBuffer.wrap(src)));
            for (int i = 0; i < 10; i++) {
                System.out.println(Integer.toBinaryString(bs.readBits(4)) + " " + bs.getBitsLeft());
            }
        } catch (AACException e) {
            eof = true;
        }
        Assert.assertTrue(eof);
    }

    @Test
    public void testReader1() throws IOException {
        BitReader _in = reader(new byte[] { b("10011000"), b("00011100"), b("11001101"), b("01010101"), b("11101001"),
                b("00101110"), b("00000111"), b("00011101"), b("10100100"), b("01101110"), b("10000101"),
                b("00001100"), b("01010111"), b("01000000") });
        assertFalse(_in.lastByte());
        assertEquals(i("1"), _in.read1Bit());
        assertFalse(_in.lastByte());
        assertEquals(i("00110000001110011001101"), _in.checkNBit(23));
        assertEquals(i("001"), _in.readNBit(3));
        assertTrue(_in.moreData());
        assertFalse(_in.lastByte());
        assertEquals(i("1000"), _in.readNBit(4));
        assertTrue(_in.isByteAligned());
        assertEquals(0, _in.curBit());
        assertEquals(7, _in.skip(7));
        assertEquals(7, _in.curBit());
        assertFalse(_in.lastByte());
        assertEquals(i("011"), _in.readNBit(3));
        assertEquals(i("0011010101010"), _in.checkNBit(13));
        assertEquals(i("0011"), _in.readNBit(4));
        assertEquals(35, _in.skip(35));
        assertTrue(_in.moreData());
        assertFalse(_in.lastByte());
        assertEquals(i("0011"), _in.checkNBit(4));
        assertEquals(i("001110110"), _in.readNBit(9));
        assertFalse(_in.lastByte());
        assertEquals(i("1"), _in.readNBit(1));
        assertFalse(_in.lastByte());
        assertEquals(i("00100"), _in.readNBit(5));
        assertTrue(_in.moreData());
        assertFalse(_in.lastByte());
        assertEquals(i("011"), _in.readNBit(3));
        assertFalse(_in.lastByte());
        assertEquals(i("01110"), _in.checkNBit(5));
        assertEquals(i("01110100"), _in.readNBit(8));
        assertFalse(_in.lastByte());
        assertEquals(i("001010000110001"), _in.readNBit(15));
        assertTrue(_in.moreData());
        assertFalse(_in.lastByte());
        assertEquals(i("010111010"), _in.readNBit(9));
        assertTrue(_in.lastByte());
        assertFalse(_in.moreData());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.readNBit(1));
        assertEquals(i("00"), _in.readNBit(2));
    }

    private BitReader reader(byte[] src) throws IOException {
        return BitReader.createBitReader(ByteBuffer.wrap(src));
    }

    @Test
    public void testReader2() throws IOException {

        BitReader _in = reader(new byte[] { b("10100111"), b("10101110"), b("10010111"), b("01000010"), b("10100101"),
                b("11000001"), b("11100001"), b("01010101"), b("00111100"), b("10100011"), b("01010000"),
                b("01010100"), b("00101010"), b("01010100"), b("10101001"), b("00001010"), b("10000011"),
                b("11000000"), b("00010101"), b("11010100"), b("10111110"), b("10100100"), b("10001010"),
                b("01010001"), b("00100000"), b("00111110"), b("00000101"), b("00100100") });
        assertTrue(_in.moreData());
        assertFalse(_in.lastByte());
        assertEquals(i("101001111010"), _in.readNBit(12));
        assertTrue(_in.moreData());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("110100101110100001"), _in.readNBit(18));
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(6, _in.skip(6));
        assertEquals(i("110"), _in.readNBit(3));
        assertEquals(i("0000111"), _in.readNBit(7));
        assertTrue(_in.moreData());
        assertEquals(i("10000"), _in.checkNBit(5));
        assertEquals(i("10000101010"), _in.readNBit(11));
        assertEquals(i("10100"), _in.readNBit(5));
        assertEquals(15, _in.skip(15));
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1000001010"), _in.readNBit(10));
        assertEquals(47, _in.skip(47));
        assertEquals(i("00000001010111010100101111101010"), _in.readNBit(32));
        assertTrue(_in.moreData());
        assertEquals(i("01001000101"), _in.readNBit(11));
        assertTrue(_in.moreData());
        assertEquals(20, _in.skip(20));
        assertTrue(_in.moreData());
        assertEquals(i("11110000001010010010"), _in.readNBit(20));
        assertFalse(_in.moreData());
        assertTrue(_in.lastByte());

    }

    @Test
    public void testReader3() throws IOException {
        BitReader _in = reader(new byte[] { b("10100111"), b("10101110"), b("10010111"), b("01000010"), b("10100101"),
                b("11000001"), b("11100001"), b("01010101"), b("00111100"), b("10100011"), b("01010000") });

        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());
        assertEquals(i("0"), _in.read1Bit());
        assertEquals(i("1"), _in.read1Bit());

    }

    public void testReader4() throws Exception {
        BitReader _in = reader(new byte[] { b("00000000"), b("00000000"), b("00000000"), b("00000000"), b("00000000"),
                b("00000000"), b("00000000"), b("00000000"), b("10001100"), b("00011100"), b("00110100"),
                b("00010000"), b("11111111") });
        _in.skip(63);
        assertEquals(i("010"), _in.readNBit(3));
        assertEquals(i("001"), _in.readNBit(3));
        assertEquals(i("100"), _in.readNBit(3));
        assertEquals(i("000"), _in.readNBit(3));
        assertEquals(i("111"), _in.readNBit(3));
        assertEquals(i("000"), _in.readNBit(3));
        assertEquals(i("011"), _in.readNBit(3));
        assertEquals(i("010"), _in.readNBit(3));
        assertEquals(i("000"), _in.readNBit(3));
        assertEquals(i("010"), _in.readNBit(3));
        assertEquals(i("000"), _in.readNBit(3));

    }

    public void testReader5() throws Exception {
        BitReader _in = BitReader.createBitReader(NIOUtils.fetchFromFile(new File(
                "src/test/resources/h264/bitstream/data.dat")));
        DummyBitstreamReader in1 = new DummyBitstreamReader(new BufferedInputStream(new FileInputStream(
                "src/test/resources/h264/bitstream/data.dat")));
        String readFileToString = Platform.stringFromBytes(NIOUtils.toArray(NIOUtils.fetchFromFile(new File("src/test/resources/h264/bitstream/reads.csv"))));

        String[] split = StringUtils.splitS(readFileToString, ",");
        for (String string : split) {
            String trim = string.trim();
            if (StringUtils.isEmpty(trim))
                continue;
            if ("r1".equals(trim))
                assertEquals(in1.read1Bit(), _in.read1Bit());
            else if ("a".equals(trim))
                assertEquals(in1.align(), _in.align());
            else if ("md".equals(trim))
                assertEquals(in1.moreData(), _in.moreData());
            else if ("lb".equals(trim))
                assertEquals(in1.lastByte(), _in.lastByte());
            else if ("cub".equals(trim))
                assertEquals(in1.curBit(), _in.curBit());
            else if (trim.startsWith("s")) {
                int i = Integer.parseInt(trim.substring(1));
                in1.skip(i);
                _in.skip(i);
            } else if (trim.startsWith("c")) {
                int i = Integer.parseInt(trim.substring(1));
                assertEquals(in1.checkNBit(i), _in.checkNBit(i));
            } else {
                int i = Integer.parseInt(trim);
                assertEquals(in1.readNBit(i), _in.readNBit(i));
            }
        }
    }

    public void testReader6() throws IOException {

        BitReader _in = reader(new byte[] { b("01010100"), b("00011001"), b("10000100"), b("10001111"), b("11101011"),
                b("10010100"), b("01101010"), b("01011111"), b("01110101") });

        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(20, _in.readNBit(6));
        assertEquals(0, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(4, _in.readNBit(3));
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
        assertEquals(0, _in.read1Bit());
        assertEquals(1, _in.read1Bit());
    }

    @Test
    public void testRandom1Bit() throws IOException {

        byte[] data = randomData(10000);

        BitReader in1 = reader(data);
        DummyBitstreamReader in2 = new DummyBitstreamReader(new ByteArrayInputStream(data));

        for (int i = 0; i < 80000; i++) {
            int exp = in2.read1Bit();
            assertEquals(exp, in1.read1Bit());
        }
    }

    @Test
    public void testRandomNBit() throws IOException {
        for (int bits = 1; bits <= 32; bits++) {
            byte[] data = randomData(bits * 10000);

            BitReader in1 = reader(data);
            DummyBitstreamReader in2 = new DummyBitstreamReader(new ByteArrayInputStream(data));

            for (int i = 0; i < 80000; i++) {
                int exp = in2.readNBit(bits);
                int exp1 = in1.readNBit(bits);
                assertEquals(exp, exp1);
            }
        }
    }

    @Test
    public void testCheckSkip() throws IOException {
        byte[] data = randomData(2048);
        BitReader reader = reader(data);
        reader.skip(1000 << 3);
        reader.skip(31);
        for (int i = 0; i < 40; i++) {
            reader.checkNBit(5);
            reader.skip(8);
        }
    }

    private byte[] randomData(int n) {
        byte[] data = new byte[n];
        for (int i = 0; i < n; i++) {
            int ni = 0;
            ni |= (Math.random() < 0.5f ? 0 : 1);
            for (int j = 0; j < 7; j++) {
                ni <<= 1;
                ni |= (Math.random() < 0.5f ? 0 : 1);
            }
            data[i] = (byte) ni;
        }
        return data;
    }

    private byte b(String str) {
        return (byte) Short.parseShort(str, 2);
    }

    private int i(String str) {
        return (int) Long.parseLong(str, 2);
    }
}