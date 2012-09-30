package org.jcodec.codecs.h264.io.read;

import static org.jcodec.common.tools.Debug.trace;

import java.io.IOException;

import org.jcodec.codecs.h264.io.BTree;
import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class CAVLCReader {

    private CAVLCReader() {

    }

    public static int readNBit(InBits bits, int n, String message) throws IOException {
        int val = bits.readNBit(n);

        trace(message, String.valueOf(val));

        return val;
    }

    private static int readUE(InBits bits) throws IOException {
        int cnt = 0;
        while (bits.read1Bit() == 0 && cnt < 31)
            cnt++;

        int res = 0;
        if (cnt > 0) {
            long val = bits.readNBit(cnt);

            res = (int) ((1 << cnt) - 1 + val);
        }

        return res;
    }

    public static int readUE(InBits bits, String message) throws IOException {
        int res = readUE(bits);

        trace(message, String.valueOf(res));

        return res;
    }

    public static int readSE(InBits bits, String message) throws IOException {
        int val = readUE(bits);

        int sign = ((val & 0x1) << 1) - 1;
        val = ((val >> 1) + (val & 0x1)) * sign;

        trace(message, String.valueOf(val));

        return val;
    }

    public static boolean readBool(InBits bits, String message) throws IOException {

        boolean res = bits.read1Bit() == 0 ? false : true;

        trace(message, res ? "1" : "0");

        return res;
    }

    public static int readU(InBits bits, int i, String string) throws IOException {
        return (int) readNBit(bits, i, string);
    }

    public static boolean readAE(InBits bits) {
        throw new UnsupportedOperationException("Stan");
    }

    public static int readTE(InBits bits, int max) throws IOException {
        if (max > 1)
            return readUE(bits);
        return ~bits.read1Bit() & 0x1;
    }

    public static int readAEI(InBits bits) {
        throw new UnsupportedOperationException("Stan");
    }

    public static int readME(InBits bits, String string) throws IOException {
        return readUE(bits, string);
    }

    public static Object readCE(InBits bits, BTree bt, String message) throws IOException {
        while (true) {
            int bit = bits.read1Bit();
            bt = bt.down(bit);
            if (bt == null) {
                throw new RuntimeException("Illegal code");
            }
            Object i = bt.getValue();
            if (i != null) {
                trace(message, i.toString());
                return i;
            }
        }
    }

    public static int readZeroBitCount(InBits bits, String message) throws IOException {
        int count = 0;
        while (bits.read1Bit() == 0 && count < 32)
            count++;

        trace(message, String.valueOf(count));

        return count;
    }

    public static void readTrailingBits(InBits bits) throws IOException {
        bits.read1Bit();
        bits.align();
    }

    public static boolean moreRBSPData(InBits bits) throws IOException {
        if (!bits.moreData())
            return false;
        int bitsRem = 8 - bits.curBit();
        if (bits.lastByte() && bits.checkNBit(bitsRem) == (1 << (bitsRem - 1)))
            return false;

        return true;
    }
}