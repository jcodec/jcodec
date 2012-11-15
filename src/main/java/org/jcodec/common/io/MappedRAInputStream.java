package org.jcodec.common.io;

import java.io.IOException;
import java.util.Arrays;

public class MappedRAInputStream extends RAInputStream {

    private RAInputStream src;
    private int[] blocks;
    private int[] inv;
    private byte[][] buffers;
    private long[] access;
    private int[] bytesLeft;
    private long ts;
    private long fileSize;
    private int blockSize;
    private int blkInd;
    private int blkOff;
    private int blockMask;

    public MappedRAInputStream(RAInputStream src) throws IOException {
        this(src, 16, 22);
    }

    public MappedRAInputStream(RAInputStream src, int bufSize, int blockSize) throws IOException {
        this.src = src;
        this.blockSize = blockSize;
        this.blockMask = (1 << blockSize) - 1;
        this.fileSize = src.length();
        blocks = new int[(int) ((fileSize + blockMask) >> blockSize)];
        buffers = new byte[bufSize][1 << blockSize];
        access = new long[bufSize];
        inv = new int[bufSize];
        bytesLeft = new int[bufSize];
        Arrays.fill(inv, -1);
        Arrays.fill(blocks, -1);
    }

    @Override
    public void seek(long where) throws IOException {
        blkInd = (int) (where >> blockSize);
        blkOff = (int) (where & blockMask);
    }

    @Override
    public long getPos() throws IOException {
        return (blkInd << blockSize) + blkOff;
    }

    @Override
    public long length() throws IOException {
        return fileSize;
    }

    @Override
    public int read() throws IOException {
        if (blocks[blkInd] == -1)
            fetchBlock(blkInd);

        if (blkInd >= blocks.length)
            return -1;

        byte[] buffer = buffers[blocks[blkInd]];
        int res = buffer[blkOff++] & 0xff;
        access[blocks[blkInd]] = ts++;

        if (blkOff == bytesLeft[blocks[blkInd]]) {
            blkInd++;
            blkOff = 0;
        }
        return res;
    }

    private void fetchBlock(int ind) throws IOException {
//        System.out.print(ind + " -- ");
        int min = 0;
        for (int i = 1; i < access.length; i++)
            if (access[i] < access[min])
                min = i;

        long seek = (long) ind << blockSize;
        if (seek != src.getPos()) {
            src.seek(seek);
        }
        byte[] buffer = buffers[min];
        int read = 0;
//        long in = System.currentTimeMillis();
        while (read < buffer.length) {
            int rr = src.read(buffer, read, Math.min(buffer.length - read, 0x2000));
            if (rr == -1)
                break;
            read += rr;
        }
//        System.out.println(System.currentTimeMillis() - in);
        bytesLeft[min] = read;

        if (inv[min] != -1) {
            blocks[inv[min]] = -1;
        }

        blocks[ind] = min;
        inv[min] = ind;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (blkInd >= blocks.length)
            return -1;
        int total = 0;
        while (len > 0 && blkInd < blocks.length) {
            if (blocks[blkInd] == -1)
                fetchBlock(blkInd);
            byte[] buffer = buffers[blocks[blkInd]];
            int toRead = Math.min(len, bytesLeft[blocks[blkInd]] - blkOff);
            System.arraycopy(buffer, blkOff, b, off, toRead);

            total += toRead;
            off += toRead;
            len -= toRead;
            blkOff += toRead;
            access[blocks[blkInd]] = ts++;
            if (blkOff == bytesLeft[blocks[blkInd]]) {
                blkInd++;
                blkOff = 0;
            }
        }
        return total;
    }
}
