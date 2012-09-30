package org.jcodec.codecs.common.biari;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * H264 CABAC M-encoder
 * 
 * @author Jay Codec
 * 
 */
public class MEncoder {
    private OutputStream out;

    // Encoder state
    private int range;
    private int offset;

    // Carry propagation related
    private int onesOutstanding;
    private boolean zeroBorrowed;

    // Output related
    private int outReg;
    private int bitsInOutReg;

    public MEncoder(OutputStream out) {
        range = 510;

        this.out = out;
    }

    /**
     * Encodes one bin in normal mode using supplied context model
     * 
     * @param bin
     * @param cm
     * @throws IOException
     */
    public void encodeBin(int bin, Context cm) throws IOException {

        int qs = (range >> 6) & 0x3;
        int rangeLPS = MConst.rangeLPS[qs][cm.getState()];
        range -= rangeLPS;

        if (bin != cm.getMps()) {
            offset += range;
            range = rangeLPS;
            if (cm.getState() == 0)
                cm.setMps(1 - cm.getMps());

            cm.setState(MConst.transitLPS[cm.getState()]);
        } else {
            if (cm.getState() < 62)
                cm.setState(cm.getState() + 1);
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
    public void encodeBinBypass(int bin) throws IOException {
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
    public void encodeBinFinal(int bin) throws IOException {
        range -= 2;
        if (bin == 0) {
            renormalize();
        } else {
            offset += range;
            range = 2;
            renormalize();
        }
    }

    public void finishEncoding() throws IOException {
        flushOutstanding((offset >> 9) & 0x1);
        putBit((offset >> 8) & 0x1);
        stuffBits();
    }

    private void renormalize() throws IOException {
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

    private void flushOutstanding(int hasCarry) throws IOException {
        if (zeroBorrowed)
            putBit(hasCarry);

        int trailingBit = 1 - hasCarry;
        for (; onesOutstanding > 0; onesOutstanding--)
            putBit(trailingBit);

        zeroBorrowed = true;
    }

    private void putBit(int bit) throws IOException {
        outReg = (outReg << 1) | bit;
        ++bitsInOutReg;

        if (bitsInOutReg == 8) {
            out.write(outReg);
            outReg = 0;
            bitsInOutReg = 0;
        }
    }

    private void stuffBits() throws IOException {
        if (bitsInOutReg == 0) {
            out.write(128);
        } else {
            outReg = (outReg << 1) | 1;
            outReg <<= (8 - (bitsInOutReg + 1));
            out.write(outReg);
            outReg = 0;
            bitsInOutReg = 0;
        }
    }
}