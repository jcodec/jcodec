package org.jcodec.common.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class LogInBits implements InBits {

    private InBits in;
    private PrintStream out;
    private int calls;
    private int curBit;

    public LogInBits(InBits in, OutputStream out) {
        this.in = in;
        this.out = new PrintStream(out);
    }

    private void printNL() {
        if((calls ++ % 30) == 0) {
            out.println();
            out.print(String.format("%08x: ", curBit));
        }
    }

    private void printCb() {
        if((curBit & 0x7) == 0)
            out.print("|");
    }
    
    public int read1Bit() throws IOException {
        printNL();
        printCb();
        int read1Bit = in.read1Bit();
        out.print("r1=" + read1Bit + ",");
        curBit += 1;
        return read1Bit;
    }

    public int readNBit(int n) throws IOException {
        printNL();
        printCb();
        int readNBit = in.readNBit(n);
        out.print("r" + n + "=" + readNBit + ",");
        curBit += n;
        return readNBit;
    }

    public int checkNBit(int n) throws IOException {
        printNL();
        int checkNBit = in.checkNBit(n);
        out.print("c" + n + "=" + checkNBit + ",");
        return checkNBit;
    }

    public boolean moreData() throws IOException {
        printNL();
        boolean moreData = in.moreData();
        out.print("md=" + moreData + ",");
        return moreData;
    }

    public int skip(int n) throws IOException {
        printNL();
        printCb();
        int skip = in.skip(n);
        out.print("s" + n + "=" + skip + ",");
        curBit += skip;
        return skip;
    }

    public int align() throws IOException {
        printNL();
        printCb();
        int align = in.align();
        out.print("a=" + align + ",");
        curBit += align;
        return align;
    }

    public int curBit() {
        printNL();
        int align = in.curBit();
        out.print("cb=" + align + ",");
        return align;
    }

    public boolean lastByte() throws IOException {
        printNL();
        boolean lastByte = in.lastByte();
        out.print("lb=" + lastByte + ",");
        return lastByte;
    }
}