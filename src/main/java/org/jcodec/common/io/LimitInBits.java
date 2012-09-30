package org.jcodec.common.io;

import static java.lang.Math.min;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class LimitInBits implements InBits {

    private int limit;
    private InBits bits;

    public LimitInBits(int limit, InBits bits) {
        this.limit = limit;
        this.bits = bits;
    }

    public int read1Bit() throws IOException {
        if (limit-- > 0)
            return bits.read1Bit();
        return -1;
    }

    public int readNBit(int n) throws IOException {
        int toRead = min(n, limit);
        if (toRead > 0)
            return readNBit(toRead);
        return -1;
    }

    public boolean moreData() throws IOException {
        return limit > 0;
    }

    public int skip(int bits) throws IOException {
        int oldLimit = limit;
        limit -= bits;
        limit = limit < 0 ? 0 : limit;
        this.bits.skip(oldLimit - limit);
        return oldLimit - limit;
    }

    public int align() throws IOException {
        int align = this.bits.align();
        limit -= align;
        return align;
    }

    public int checkNBit(int n) throws IOException {
        int nn = n < limit ? n : limit;
        return checkNBit(nn) << (n - nn);
    }

    public int curBit() {
        return bits.curBit();
    }

    public boolean lastByte() throws IOException {
        return limit >= 8 ? bits.lastByte() : true;
    }
}
