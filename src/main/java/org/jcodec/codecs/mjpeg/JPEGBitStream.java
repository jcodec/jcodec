package org.jcodec.codecs.mjpeg;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.VLC;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Class for convenient reading JPEG entropy coded bit stream
 * 
 * @author Jay Codec
 * 
 */
public class JPEGBitStream {
    private VLC huffYDC;
    private VLC huffCDC;
    private VLC huffYAC;
    private VLC huffCAC;
    private BitReader in;

    /**
     * @deprecated
     * @param coded
     * @throws IOException
     */
    public JPEGBitStream(CodedImage coded) {
        this(coded, ByteBuffer.wrap(coded.getData()));
    }

    public JPEGBitStream(CodedImage coded, ByteBuffer b) {
        this.in = new BitReader(b);
        this.huffYDC = coded.getYdc();
        this.huffCDC = coded.getCdc();
        this.huffYAC = coded.getYac();
        this.huffCAC = coded.getCac();
    }

    private final int prevLumDC[] = new int[1];
    private final int prevCbDC[] = new int[1];
    private final int prevCrDC[] = new int[1];

    public MCU readBlock(MCU block) throws IOException {
        block.clear();
        for (int i = 0; i < block.lum.data.length; i++) {
            readDCValue(block.lum.data[i], prevLumDC, huffYDC);
            readACValues(block.lum.data[i], huffYAC);
        }
        for (int i = 0; i < block.cb.data.length; i++) {
            readDCValue(block.cb.data[i], prevCbDC, huffCDC);
            readACValues(block.cb.data[i], huffCAC);
        }
        for (int i = 0; i < block.cr.data.length; i++) {
            readDCValue(block.cr.data[i], prevCrDC, huffCDC);
            readACValues(block.cr.data[i], huffCAC);
        }
        return block;
    }

    public void readDCValue(int[] target, int[] prevDC, VLC table) {
        int code = table.readVLC(in);
        if (code != 0) {
            target[0] = toValue(in.readNBit(code), code);
            target[0] += prevDC[0];
            prevDC[0] = target[0];
        } else {
            target[0] = prevDC[0];
        }
    }

    public void readACValues(int[] target, VLC table) throws IOException {
        int code;
        int curOff = 1;
        do {
            code = table.readVLC(in);
            if (code == 0xF0) {
                curOff += 16;
            } else if (code > 0) {
                int rle = code >> 4;
                curOff += rle;
                int len = code & 0xf;
                target[curOff] = toValue(in.readNBit(len), len);
                curOff++;
            }
        } while (code != 0 && curOff < 64);
    }

    public final int toValue(int raw, int length) {
        return (length >= 1 && raw < (1 << length - 1)) ? -(1 << length) + 1 + raw : raw;
    }
}