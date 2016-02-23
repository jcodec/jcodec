package org.jcodec.codecs.mjpeg;

import java.nio.ByteBuffer;

import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.VLC;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Class for convenient reading JPEG entropy coded bit stream
 * 
 * @author The JCodec project
 * 
 */
public class JPEGBitStream {
    private VLC[] huff;
    private BitReader _in;
    private int[] dcPredictor;
    private int lumaLen;
    
    

    public JPEGBitStream(ByteBuffer b, VLC[] huff, int lumaLen) {
        this.dcPredictor = new int[3];
        this._in = new BitReader(b);
        this.huff = huff;
        this.lumaLen = lumaLen;
    }

    public void readMCU(int[][] buf) {
        int blk = 0;
        for (int i = 0; i < lumaLen; i++, blk++) {
            dcPredictor[0] = buf[blk][0] = readDCValue(dcPredictor[0], huff[0]);
            readACValues(buf[blk], huff[2]);
        }

        dcPredictor[1] = buf[blk][0] = readDCValue(dcPredictor[1], huff[1]);
        readACValues(buf[blk], huff[3]);
        ++blk;

        dcPredictor[2] = buf[blk][0] = readDCValue(dcPredictor[2], huff[1]);
        readACValues(buf[blk], huff[3]);
        ++blk;
    }

    public int readDCValue(int prevDC, VLC table) {
        int code = table.readVLC(_in);
        return code != 0 ? toValue(_in.readNBit(code), code) + prevDC : prevDC;
    }

    public void readACValues(int[] target, VLC table) {
        int code;
        int curOff = 1;
        do {
            code = table.readVLC(_in);
            if (code == 0xF0) {
                curOff += 16;
            } else if (code > 0) {
                int rle = code >> 4;
                curOff += rle;
                int len = code & 0xf;
                target[curOff] = toValue(_in.readNBit(len), len);
                curOff++;
            }
        } while (code != 0 && curOff < 64);
    }

    public final int toValue(int raw, int length) {
        return (length >= 1 && raw < (1 << length - 1)) ? -(1 << length) + 1 + raw : raw;
    }
}