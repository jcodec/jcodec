package org.jcodec.common.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A dummy implementation of H264 RBSP reading
 * 
 * @author The JCodec project
 * 
 */
public class DummyBitstreamReader {
    private InputStream is;
    private int curByte;
    private int nextByte;
    private int secondByte;
    int nBit;
    protected static int bitsRead;

    public DummyBitstreamReader(InputStream is) throws IOException {
        this.is = is;
        curByte = is.read();
        nextByte = is.read();
        secondByte = is.read();
        // println("\n-> " + curByte);
    }

    int cnt = 0;

    /*
     * (non-Javadoc)
     * 
     * @see ua.org.jplayer.javcodec.h264.RBSPInputStream#read1Bit()
     */
    public int read1Bit() throws IOException {
        return read1BitInt();
    }

    public int read1BitInt() throws IOException {

        if (nBit == 8) {
            advance();
            if (curByte == -1) {
                return -1;
            }
        }
        int res = (curByte >> (7 - nBit)) & 1;
        nBit++;

//        debugBits.append(res == 0 ? '0' : '1');
        ++bitsRead;

        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ua.org.jplayer.javcodec.h264.RBSPInputStream#readNBit(int)
     */
    public int readNBit(int n) throws IOException {
        if (n > 32)
            throw new IllegalArgumentException("Can not read more then 32 bit");

        int val = 0;

        for (int i = 0; i < n; i++) {
            val <<= 1;
            val |= read1BitInt();
        }

        return val;
    }

    private final void advance1() throws IOException {
        curByte = nextByte;
        nextByte = secondByte;
        secondByte = is.read();
    }

    private final void advance() throws IOException {
        advance1();
        nBit = 0;
        // println("\n-> " + curByte);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ua.org.jplayer.javcodec.h264.RBSPInputStream#readByte()
     */
    public int readByte() throws IOException {
        if (nBit > 0) {
            advance();
        }

        int res = curByte;

        advance();

        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ua.org.jplayer.javcodec.h264.RBSPInputStream#moreRBSPData()
     */
    public boolean moreRBSPData() throws IOException {
        if (nBit == 8) {
            advance();
        }
        int tail = 1 << (8 - nBit - 1);
        int mask = ((tail << 1) - 1);
        boolean hasTail = (curByte & mask) == tail;

        return !(curByte == -1 || (nextByte == -1 && hasTail));
    }

    public long getBitPosition() {
        return (bitsRead * 8 + (nBit % 8));
    }

    public boolean moreData() throws IOException {
        if (nBit == 8)
            advance();
        if (curByte == -1)
            return false;
        if (nextByte == -1 || (nextByte == 0 && secondByte == -1)) {
            int mask = (1 << (8 - nBit)) - 1;
            return (curByte & mask) != 0;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ua.org.jplayer.javcodec.h264.RBSPInputStream#readRemainingByte()
     */
    public long readRemainingByte() throws IOException {
        return readNBit(8 - nBit);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ua.org.jplayer.javcodec.h264.RBSPInputStream#next_bits(int)
     */
    public int peakNextBits(int n) throws IOException {
        if (n > 8)
            throw new IllegalArgumentException("N should be less then 8");
        if (nBit == 8) {
            advance();
            if (curByte == -1) {
                return -1;
            }
        }
        int[] bits = new int[16 - nBit];

        int cnt = 0;
        for (int i = nBit; i < 8; i++) {
            bits[cnt++] = (curByte >> (7 - i)) & 0x1;
        }

        for (int i = 0; i < 8; i++) {
            bits[cnt++] = (nextByte >> (7 - i)) & 0x1;
        }

        int result = 0;
        for (int i = 0; i < n; i++) {
            result <<= 1;
            result |= bits[i];
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ua.org.jplayer.javcodec.h264.RBSPInputStream#byte_aligned()
     */
    public boolean isByteAligned() {
        return (nBit % 8) == 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ua.org.jplayer.javcodec.h264.RBSPInputStream#close()
     */
    public void close() throws IOException {
        is.close();
    }

    public int getCurBit() {
        return nBit;
    }

    public boolean moreData(int bits) throws IOException {
        throw new UnsupportedOperationException();
    }

    public final int skip(int bits) throws IOException {
        nBit += bits;
        int was = nBit;
        while (nBit >= 8 && curByte != -1) {
            advance1();
            nBit -= 8;
        }
        return was - nBit;
    }

    public int align() throws IOException {
        int n = (8 - nBit) & 0x7;
        skip((8 - nBit) & 0x7);
        return n;
    }

    public int checkNBit(int n) throws IOException {
        return peakNextBits(n);
    }

    public int curBit() {
        return nBit;
    }

    public boolean lastByte() throws IOException {
        return nextByte == -1 && secondByte == -1;
    }
}