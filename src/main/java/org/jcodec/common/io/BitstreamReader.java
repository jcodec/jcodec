package org.jcodec.common.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class BitstreamReader implements InBits {
    public static int BUF_SIZE = 1024;
    public static int READ_THRESH = 16;

    protected InputStream is;
    protected int curBit;
    protected int nextByte;
    protected int curInt;

    protected byte[] buf = new byte[BUF_SIZE];
    protected int available;
    protected int target;

    public BitstreamReader(InputStream is) throws IOException {
        this.is = is;
        nextInt();
    }

    public int read1Bit() throws IOException {

        int ret = curInt >>> 31;
        curInt <<= 1;
        ++curBit;
        if (curBit == 32) {
            nextInt();
            curBit = 0;
        }

        return ret;
    }

    private final void nextInt() throws IOException {
        tryRead();
        curInt = ((buf[nextByte++] & 0xff) << 24) | ((buf[nextByte++] & 0xff) << 16) | ((buf[nextByte++] & 0xff) << 8)
                | (buf[nextByte++] & 0xff);
    }

    private final void tryRead() throws IOException {
        if (available - nextByte >= READ_THRESH || available != target)
            return;
        target = buf.length;
        int i;
        for (i = 0; i < available - nextByte; i++)
            buf[i] = buf[nextByte + i];
        buf[i] = buf[i + 1] = buf[i + 2] = buf[i + 3] = 0;
        int read;
        while ((i < target) && (read = is.read(buf, i, target - i)) != -1)
            i += read;
        available = i;
        nextByte = 0;
    }

    public int readNBit(int n) throws IOException {
        if (n > 32)
            throw new IllegalArgumentException("Can not read more then 32 bit");

        curBit += n;

        int ret;
        if (curBit > 31) {
            ret = curInt >>> (curBit - n);
            curBit -= 32;
            nextInt();
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

//    private void printBits() {
//        String string = Long.toString(curInt & 0xffffffffL, 2);
//        System.out.print("int: ");
//        for (int i = 0; i < 32 - string.length(); i++)
//            System.out.print("0");
//        System.out.println(string);
//    }

    public boolean moreData() throws IOException {
        int curByte = nextByte + (curBit >> 3) - 4;
        if (curByte < available - 1)
            return true;
        else if (curByte == available - 1) {
            return checkNBit(8 - (curBit & 0x7)) != 0;
        } else
            return false;
    }

    public final boolean isByteAligned() {
        return (curBit & 0x7) == 0;
    }

    public void close() throws IOException {
        is.close();
    }

    public int skip(int bits) throws IOException {
        curBit += bits;

        if (curBit > 31) {
            curBit -= 32;
            int dwSkip = (curBit >> 5);
            curBit &= 0x1f;
            tryRead();
            nextByte += dwSkip << 2;
            nextInt();
            curInt <<= curBit;
        } else {
            curInt <<= bits;
        }

        return bits;
    }

    public int align() throws IOException {
        return (curBit & 0x7) > 0 ? skip(8 - (curBit & 0x7)) : 0;
    }

    public int checkNBit(int n) throws IOException {
        if (n > 24)
            throw new IllegalArgumentException("Can not check more then 24 bit");
        
        tryRead();
        while (curBit + n > 32 && nextByte < buf.length) {
            curBit -= 8;
            curInt |= (buf[nextByte++] & 0xff) << curBit;
        }
        return curInt >>> (32 - n);
    }

    public int curBit() {
        return curBit & 0x7;
    }

    public boolean lastByte() throws IOException {
        return (nextByte + (curBit >> 3) - 4) == (available - 1);
    }
}