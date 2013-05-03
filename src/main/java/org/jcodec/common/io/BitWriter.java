package org.jcodec.common.io;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Bitstream writer
 * 
 * @author The JCodec project
 * 
 */
public class BitWriter {

    private final ByteBuffer os;
    private int curInt;
    private int curBit;

    public BitWriter(ByteBuffer out) {
        os = out;
    }

    public void flush() {
        int toWrite = (curBit + 7) >> 3;
        for (int i = 0; i < toWrite; i++) {
            os.put((byte) (curInt >>> 24));
            curInt <<= 8;
        }
    }

    private final void putInt(int i) {
        os.put((byte) (i >>> 24));
        os.put((byte) (i >> 16));
        os.put((byte) (i >> 8));
        os.put((byte) i);
    }

    public final void writeNBit(int value, int n) {
        if (n > 32)
            throw new IllegalArgumentException("Max 32 bit to write");
        if (n == 0)
            return;
        value &= -1 >>> (32 - n);
        if (32 - curBit >= n) {
            curInt |= value << (32 - curBit - n);
            curBit += n;
            if (curBit == 32) {
                putInt(curInt);
                curBit = 0;
                curInt = 0;
            }
        } else {
            int secPart = n - (32 - curBit);
            curInt |= value >>> secPart;
            putInt(curInt);
            curInt = value << (32 - secPart);
            curBit = secPart;
        }
    }

    public void write1Bit(int bit) {
        curInt |= bit << (32 - curBit - 1);
        ++curBit;
        if (curBit == 32) {
            putInt(curInt);
            curBit = 0;
            curInt = 0;
        }
    }

    public int curBit() {
        return curBit & 0x7;
    }
}