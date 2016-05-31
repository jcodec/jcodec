package net.sourceforge.jaad.aac.syntax;
import org.jcodec.common.io.BitReader;

import java.nio.ByteBuffer;
import net.sourceforge.jaad.aac.AACException;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * @author in-somnia
 */
public class NIOBitStream implements IBitStream {

    private BitReader br;

    public NIOBitStream(BitReader br) {
        this.br = br;
    }

    @Override
    public void destroy() {
        reset();
        br = null;
    }

    @Override
    public void setData(byte[] data) {
        br = BitReader.createBitReader(ByteBuffer.wrap(data));
    }

    @Override
    public void byteAlign() throws AACException {
        br.align();
    }

    @Override
    public void reset() {
        throw new RuntimeException("todo");
    }

    @Override
    public int getPosition() {
        return br.position();
    }

    @Override
    public int getBitsLeft() {
        return br.remaining();
    }

    @Override
    public int readBits(int n) throws AACException {
        if (br.remaining() >= n) {
            return br.readNBit(n);
        }
        throw AACException.endOfStream();
    }

    @Override
    public int readBit() throws AACException {
        if (br.remaining() >= 1) {
            return br.read1Bit();
        }
        throw AACException.endOfStream();
    }

    @Override
    public boolean readBool() throws AACException {
        int read1Bit = readBit();
        return read1Bit != 0;
    }

    @Override
    public int peekBits(int n) throws AACException {
        int checkNBit = br.checkNBit(n);
        return checkNBit;
    }

    @Override
    public int peekBit() throws AACException {
        int curBit = br.curBit();
        return curBit;
    }

    @Override
    public void skipBits(int n) throws AACException {
        br.skip(n);
    }

    @Override
    public void skipBit() throws AACException {
        skipBits(1);
    }

    @Override
    public int maskBits(int n) {
        int i;
        if (n == 32)
            i = -1;
        else
            i = (1 << n) - 1;
        return i;
    }

}
