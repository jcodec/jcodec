package org.jcodec.common.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Bitstream writer
 * 
 * @author The JCodec project
 * 
 */
public class BitstreamWriter implements OutBits {

    private final OutputStream os;
    private int curInt;
    private int curBit;

    public BitstreamWriter(OutputStream out) {
        this.os = out;
    }

    public void flush() throws IOException {
        int toWrite = (curBit + 7) >> 3;
        for (int i = 0; i < toWrite; i++) {
            os.write(curInt >>> 24);
            curInt <<= 8;
        }
    }

    public final void writeNBit(int value, int n) throws IOException {
        if (n > 32)
            throw new IllegalArgumentException("Max 32 bit to write");
        if (32 - curBit >= n) {
            value &= (1 << n) - 1;
            curInt |= value << (32 - curBit - n);
            curBit += n;
            if (curBit == 32) {
                outInt();
                curBit = 0;
                curInt = 0;
            }
        } else {
            int secPart = n - (32 - curBit);
            curInt |= value >>> secPart;
            outInt();
            curInt = value << (32 - secPart);
            curBit = secPart;
        }
    }

    private final void outInt() throws IOException {
        os.write(curInt >>> 24);
        os.write((curInt >> 16) & 0xff);
        os.write((curInt >> 8) & 0xff);
        os.write(curInt & 0xff);
    }

    public void write1Bit(int bit) throws IOException {
        curInt |= bit << (32 - curBit - 1);
        ++curBit;
        if (curBit == 32) {
            outInt();
            curBit = 0;
            curInt = 0;
        }
    }

    public int curBit() {
        return curBit & 0x7;
    }
}