package org.jcodec.common.io;

import java.io.InputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class BitstreamReaderBB implements InBits {
    protected InputStream is;
    protected int curBit;
    protected int curInt;
    private Buffer bb;

    public BitstreamReaderBB(Buffer bb) {
        this.bb = bb;
        curInt = bb.nextIntLE();
    }

    public int read1Bit() {

        int ret = curInt >>> 31;
        curInt <<= 1;
        ++curBit;
        if (curBit == 32) {
            curInt = bb.nextIntLE();
            curBit = 0;
        }

        return ret;
    }

    public int readNBit(int n) {
        if (n > 32)
            throw new IllegalArgumentException("Can not read more then 32 bit");

        curBit += n;

        int ret;
        if (curBit > 31) {
            ret = curInt >>> (curBit - n);
            curBit -= 32;
            curInt = bb.nextIntLE();
            if (curBit != 0) {
                ret = (ret << curBit) | (curInt >>> (32 - curBit));
                curInt <<= curBit;
            }
        } else {
            ret = curInt >>> (32 - n);
            curInt <<= n;
        }

        return ret;
    }

    // private void printBits() {
    // String string = Long.toString(curInt & 0xffffffffL, 2);
    // System.out.print("int: ");
    // for (int i = 0; i < 32 - string.length(); i++)
    // System.out.print("0");
    // System.out.println(string);
    // }

    public boolean moreData() {
        int remaining = bb.remaining() + 4 - ((curBit + 7) >> 3);
        return remaining > 1 || (remaining == 1 && curInt != 0);
    }

    public int remaining() {
        return (bb.remaining() << 3) + 32 - curBit;
    }

    public final boolean isByteAligned() {
        return (curBit & 0x7) == 0;
    }

    public int skip(int bits) {
        curBit += bits;

        if (curBit > 31) {
            curBit -= 32;
            int dwSkip = (curBit >> 5);
            curBit &= 0x1f;
            bb.skip(dwSkip << 2);
            curInt = bb.nextIntLE();
            curInt <<= curBit;
        } else {
            curInt <<= bits;
        }

        return bits;
    }

    public int align() {
        return (curBit & 0x7) > 0 ? skip(8 - (curBit & 0x7)) : 0;
    }

    public int checkNBit(int n) {
        if (n > 24)
            throw new IllegalArgumentException("Can not check more then 24 bit");

        while (curBit + n > 32) {
            curBit -= 8;
            curInt |= bb.nextIgnore() << curBit;
        }
        return curInt >>> (32 - n);
    }

    public int curBit() {
        return curBit & 0x7;
    }

    public boolean lastByte() {
        return bb.remaining() + 4 - ((curBit + 7) >> 3) <= 1;
    }
}