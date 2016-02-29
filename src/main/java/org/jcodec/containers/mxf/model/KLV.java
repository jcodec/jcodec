package org.jcodec.containers.mxf.model;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.io.SeekableByteChannel;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class KLV {
    public final long offset;
    public final long dataOffset;

    public final UL key;
    public final long len;

    ByteBuffer value;

    public KLV(UL k, long len, long offset, long dataOffset) {
        this.key = k;
        this.len = len;
        this.offset = offset;
        this.dataOffset = dataOffset;
    }

    @Override
    public String toString() {
        return "KLV [offset=" + offset + ", dataOffset=" + dataOffset + ", key=" + key + ", len=" + len + ", value="
                + value + "]";
    }

    public static KLV readKL(SeekableByteChannel ch) throws IOException {
        long offset = ch.position();
        if (offset >= ch.size() - 1)
            return null;

        byte[] key = new byte[16];
        ch.read(ByteBuffer.wrap(key));

        long len = BER.decodeLength(ch);
        long dataOffset = ch.position();
        return new KLV(new UL(key), len, offset, dataOffset);
    }

    /**
     * @return byte count of BER encoded "length" field
     */
    public int getLenByteCount() {
        int berlen = (int) (dataOffset - offset - 16);
        return berlen <= 0 ? 4 : berlen;
    }

    public static boolean matches(byte[] key1, byte[] key2, int len) {
        for (int i = 0; i < len; i++)
            if (key1[i] != key2[i])
                return false;
        return true;
    }

    public static KLV readKLFromBuffer(ByteBuffer buffer, long baseOffset) {
        if (buffer.remaining() < 17)
            return null;

        long offset = baseOffset + buffer.position();

        UL ul = UL.read(buffer);

        long len = BER.decodeLengthBuf(buffer);
        return new KLV(ul, len, offset, baseOffset + buffer.position());
    }
}
