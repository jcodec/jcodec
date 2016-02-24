package org.jcodec.codecs.h264.decode;

import static org.jcodec.common.tools.Debug.trace;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.io.BitReader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class CAVLCReader {

    private CAVLCReader() {

    }

    public static int readNBit(BitReader bits, int n, String message)  {
        int val = bits.readNBit(n);

        trace(message, val);

        return val;
    }

    public static int readUE(BitReader bits)  {
        int cnt = 0;
        while (bits.read1Bit() == 0 && cnt < 32)
            cnt++;

        int res = 0;
        if (cnt > 0) {
            long val = bits.readNBit(cnt);

            res = (int) ((1 << cnt) - 1 + val);
        }

        return res;
    }

    public static int readUEtrace(BitReader bits, String message)  {
        int res = readUE(bits);

        trace(message, res);

        return res;
    }

    public static int readSE(BitReader bits, String message)  {
        int val = readUE(bits);

        val = H264Utils.golomb2Signed(val);

        trace(message, val);

        return val;
    }

    public static boolean readBool(BitReader bits, String message)  {

        boolean res = bits.read1Bit() == 0 ? false : true;

        trace(message, res ? 1 : 0);

        return res;
    }

    public static int readU(BitReader bits, int i, String string)  {
        return (int) readNBit(bits, i, string);
    }

    public static int readTE(BitReader bits, int max)  {
        if (max > 1)
            return readUE(bits);
        return ~bits.read1Bit() & 0x1;
    }

    public static int readME(BitReader bits, String string)  {
        return readUEtrace(bits, string);
    }

    public static int readZeroBitCount(BitReader bits, String message)  {
        int count = 0;
        while (bits.read1Bit() == 0 && count < 32)
            count++;

        trace(message, String.valueOf(count));

        return count;
    }

    public static boolean moreRBSPData(BitReader bits)  {
        return !(bits.remaining() < 32 && bits.checkNBit(1) == 1 && (bits.checkNBit(24) << 9) == 0);
    }
}