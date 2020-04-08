package org.jcodec.common.io;
import java.lang.IllegalArgumentException;
import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Bitstream writer
 * 
 * @author The JCodec project
 * 
 */
public class BitWriter {

    private final ByteBuffer buf;
    private int curInt;
    private int _curBit;
    private int initPos;

    public BitWriter(ByteBuffer buf) {
        this.buf = buf;
        initPos = buf.position();
    }

    public BitWriter fork() {
        BitWriter fork = new BitWriter(buf.duplicate());
        fork._curBit = this._curBit;
        fork.curInt = this.curInt;
        fork.initPos = this.initPos;
        return fork;
    }
    
    public void writeOther(BitWriter bw) {
        if (_curBit >= 8) {
            int shift = 32 - _curBit;
            for (int i = initPos; i < bw.buf.position(); i++) {
                buf.put((byte)(curInt >> 24));
                curInt <<= 8;
                curInt |= (bw.buf.get(i) & 0xff) << shift;
            }
        } else {
            int shift = 24 - _curBit;
            for (int i = initPos; i < bw.buf.position(); i++) {
                curInt |= (bw.buf.get(i) & 0xff) << shift;
                buf.put((byte)(curInt >> 24));
                curInt <<= 8;
            }
        }
        writeNBit(bw.curInt >> (32 - bw._curBit), bw._curBit);
    }

    public void flush() {
        int toWrite = (_curBit + 7) >> 3;
        for (int i = 0; i < toWrite; i++) {
            buf.put((byte) (curInt >>> 24));
            curInt <<= 8;
        }
    }

    private final void putInt(int i) {
        buf.put((byte) (i >>> 24));
        buf.put((byte) (i >> 16));
        buf.put((byte) (i >> 8));
        buf.put((byte) i);
    }

    public final void writeNBit(int value, int n) {
        if (n > 32)
            throw new IllegalArgumentException("Max 32 bit to write");
        if (n == 0)
            return;
        value &= -1 >>> (32 - n);
        if (32 - _curBit >= n) {
            curInt |= value << (32 - _curBit - n);
            _curBit += n;
            if (_curBit == 32) {
                putInt(curInt);
                _curBit = 0;
                curInt = 0;
            }
        } else {
            int secPart = n - (32 - _curBit);
            curInt |= value >>> secPart;
            putInt(curInt);
            curInt = value << (32 - secPart);
            _curBit = secPart;
        }
    }

    public void write1Bit(int bit) {
        curInt |= bit << (32 - _curBit - 1);
        ++_curBit;
        if (_curBit == 32) {
            putInt(curInt);
            _curBit = 0;
            curInt = 0;
        }
    }

    public int curBit() {
        return _curBit & 0x7;
    }

    public int position() {
        return ((buf.position() - initPos) << 3) + _curBit;
    }

    public ByteBuffer getBuffer() {
        return buf;
    }
}