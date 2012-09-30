package org.jcodec.codecs.h264.annexb;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.codecs.h264.annexb.AnnexBDemuxer.NALUnitSource;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Input stream that automatically unwraps NAL units and allows for NAL unit
 * navigation
 * 
 * @author Jay Codec
 * 
 */
public class NALUnitReader implements NALUnitSource {
    private final InputStream src;
    private int next0;
    private int next1;
    private int next2;
    private int prev0;
    private int prev1;
    private int curByte;

    private BufferedInputStream proxy = new BufferedInputStream(new InputStream() {
        @Override
        public int read() throws IOException {
            return readByte();
        }
    });

    public NALUnitReader(InputStream src) throws IOException {
        this.src = src;
        fillBuffer();
    }

    private int readByte() throws IOException {
        boolean marker = curByte == 0 && next0 == 0 && ((next1 == 0 && next2 == 1) || next1 == 1);
        if (marker)
            return -1;

        if (curByte == 3 && prev0 == 0 && prev1 == 0) {
            advance();
        }

        int b = curByte;

        advance();

        return b;
    }

    private void advance() throws IOException {

        prev1 = prev0;
        prev0 = curByte;
        curByte = next0;
        next0 = next1;
        next1 = next2;
        next2 = src.read();

    }

    private void fillBuffer() throws IOException {
        this.curByte = src.read();
        this.next0 = src.read();
        this.next1 = src.read();
        this.next2 = src.read();
        this.prev0 = -1;
        this.prev1 = -1;
    }

    public InputStream nextNALUnit() throws IOException {
        boolean valid = false;
        int headerLen = 0;
        if (curByte == 0 && next0 == 0) {
            if (next1 == 1) {
                valid = true;
                headerLen = 3;
            } else if (next1 == 0 && next2 == 1) {
                valid = true;
                headerLen = 4;
            }
        }
        if (!valid)
            return null;

        for (int i = 0; i < headerLen; i++) {
            advance();
        }

        return proxy;
    }
}