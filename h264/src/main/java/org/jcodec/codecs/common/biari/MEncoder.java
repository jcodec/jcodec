package org.jcodec.codecs.common.biari;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * H264 CABAC M-encoder
 * 
 * @author The JCodec project
 * 
 */
public class MEncoder {
    private ByteBuffer out;

    // Encoder state
    private int range;
    private int offset;

    // Carry propagation related
    private int onesOutstanding;
    private boolean zeroBorrowed;

    // Output related
    private int outReg;
    private int bitsInOutReg;

    private int[][] models;

    public MEncoder(ByteBuffer out, int[][] models) {
        range = 510;

        this.models = models;
        this.out = out;
    }

    /**
     * Encodes one bin in normal mode using supplied context model
     * 
     * @param bin
     * @param cm
     * @throws IOException
     */
    public void encodeBin(int model, int bin) {

        int qs = (range >> 6) & 0x3;
        int rangeLPS = MConst.rangeLPS[qs][models[0][model]];
        range -= rangeLPS;

        if (bin != models[1][model]) {
            offset += range;
            range = rangeLPS;
            if (models[0][model] == 0)
                models[1][model] = 1 - models[1][model];

            models[0][model] = MConst.transitLPS[models[0][model]];
        } else {
            if (models[0][model] < 62)
                models[0][model] ++;
        }

        renormalize();
    }

    /**
     * Codes one bin in bypass mode for symbols with uniform probability
     * distribution
     * 
     * @param bin
     * @throws IOException
     */
    public void encodeBinBypass(int bin) {
        offset <<= 1;
        if (bin == 1) {
            offset += range;
        }

        if ((offset & 0x400) != 0) {
            flushOutstanding(1);
            offset &= 0x3ff;
        } else if ((offset & 0x200) != 0) {
            offset &= 0x1ff;
            ++onesOutstanding;
        } else {
            flushOutstanding(0);
        }
    }

    /**
     * Codes termination flag. Range for LPS is preset to be 2
     * 
     * @param bin
     * @throws IOException
     */
    public void encodeBinFinal(int bin) {
        range -= 2;
        if (bin == 0) {
            renormalize();
        } else {
            offset += range;
            range = 2;
            renormalize();
        }
    }

    public void finishEncoding() {
        flushOutstanding((offset >> 9) & 0x1);
        putBit((offset >> 8) & 0x1);
        stuffBits();
    }

    private void renormalize() {
        while (range < 256) {
            if (offset < 256) {
                flushOutstanding(0);
            } else if (offset < 512) {
                offset &= 0xff;
                ++onesOutstanding;
            } else {
                offset &= 0x1ff;
                flushOutstanding(1);
            }

            range <<= 1;
            offset <<= 1;
        }
    }

    private void flushOutstanding(int hasCarry) {
        if (zeroBorrowed)
            putBit(hasCarry);

        int trailingBit = 1 - hasCarry;
        for (; onesOutstanding > 0; onesOutstanding--)
            putBit(trailingBit);

        zeroBorrowed = true;
    }

    private void putBit(int bit) {
        outReg = (outReg << 1) | bit;
        ++bitsInOutReg;

        if (bitsInOutReg == 8) {
            out.put((byte) outReg);
            outReg = 0;
            bitsInOutReg = 0;
        }
    }

    private void stuffBits() {
        if (bitsInOutReg == 0) {
            out.put((byte) 128);
        } else {
            outReg = (outReg << 1) | 1;
            outReg <<= (8 - (bitsInOutReg + 1));
            out.put((byte) outReg);
            outReg = 0;
            bitsInOutReg = 0;
        }
    }
}