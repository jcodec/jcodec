package org.jcodec.codecs.common.biari;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Binary ariphmetic encoder
 * 
 * Half-way to MQ Coder
 * 
 * @author Jay Codec
 * 
 */
public class MQEncoder {
    public static final int CARRY_MASK = (1 << 27);

    // 'A' register, only lower 16 bits are used
    private int range;
    // 'C' register, only 28 bits are used
    private int offset;
    // 't' variable
    private int bitsToCode;
    // 'L' variable
    private long bytesOutput;
    // 'B' variable
    private int byteToGo;

    // Stream to output coded bytes
    private OutputStream out;

    public MQEncoder(OutputStream out) {
        range = 0x8000;
        offset = 0;
        bitsToCode = 12;
        this.out = out;
    }

    /**
     * Encodes one symbol either 0 or 1
     * 
     * @param symbol
     * @throws IOException
     */
    public void encode(int symbol, Context cm) throws IOException {

        int rangeLps = MQConst.pLps[cm.getState()];
        if (symbol == cm.getMps()) {
            range -= rangeLps;
            offset += rangeLps;
            if (range < 0x8000) {
                while (range < 0x8000)
                    renormalize();
                cm.setState(MQConst.transitMPS[cm.getState()]);
            }
        } else {
            range = rangeLps;
            while (range < 0x8000)
                renormalize();

            if (MQConst.mpsSwitch[cm.getState()] != 0)
                cm.setMps(1 - cm.getMps());
            cm.setState(MQConst.transitLPS[cm.getState()]);
        }
    }

    public void finish() throws IOException {
        finalizeValue();
        offset <<= bitsToCode;

        int bitsToOutput = 12 - bitsToCode;
        outputByte();
        bitsToOutput -= bitsToCode;
        if (bitsToOutput > 0) {
            offset <<= bitsToCode;
            outputByte();
        }

        out.write(byteToGo);
    }

    private void finalizeValue() {
        int halfBit = offset & 0x8000;
        offset &= 0xffff0000;
        if (halfBit == 0) {
            offset |= 0x8000;
        } else {
            offset += 0x10000;
        }
    }

    private void renormalize() throws IOException {

        offset <<= 1;
        range <<= 1;
        range &= 0xffffL;

        --bitsToCode;

        if (bitsToCode == 0)
            outputByte();
    }

    private void outputByte() throws IOException {
        if (bytesOutput == 0)
            outputByteNoStuffing();
        else {
            if (byteToGo == 0xff)
                outputByteWithStuffing();
            else {
                if ((offset & CARRY_MASK) != 0) {
                    ++byteToGo;
                    offset &= 0x7ffffff;
                    if (byteToGo == 0xff)
                        outputByteWithStuffing();
                    else
                        outputByteNoStuffing();
                } else
                    outputByteNoStuffing();
            }
        }
    }

    private void outputByteWithStuffing() throws IOException {
        bitsToCode = 7;
        if (bytesOutput > 0) {
            out.write(byteToGo);
        }
        byteToGo = (offset >> 20) & 0xff;
        offset &= 0xfffff;

        ++bytesOutput;
    }

    private void outputByteNoStuffing() throws IOException {
        bitsToCode = 8;
        if (bytesOutput > 0) {
            out.write(byteToGo);
        }
        byteToGo = (offset >> 19) & 0xff;

        offset &= 0x7ffff;

        ++bytesOutput;
    }
}