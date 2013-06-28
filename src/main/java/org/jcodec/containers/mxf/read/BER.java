package org.jcodec.containers.mxf.read;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;

public class BER {
    public static final byte ASN_LONG_LEN = (byte) 0x80;

    public static final long decodeLength(SeekableByteChannel is) throws IOException {
        long length = 0;
        int lengthbyte = NIOUtils.readByte(is) & 0xff;

        if ((lengthbyte & ASN_LONG_LEN) > 0) {
            lengthbyte &= ~ASN_LONG_LEN;
            if (lengthbyte == 0)
                throw new IOException("Indefinite lengths are not supported");
            if (lengthbyte > 8)
                throw new IOException("Data length > 4 bytes are not supported!");
            byte[] bb = NIOUtils.readNByte(is, lengthbyte);

            for (int i = 0; i < lengthbyte; i++)
                length = (length << 8) | (bb[i] & 0xff);

            if (length < 0)
                throw new IOException("mxflib does not support data lengths > 2^63");
        } else {
            length = lengthbyte & 0xFF;
        }
        return length;
    }

    public static long decodeLength(ByteBuffer buffer) {
        long length = 0;
        int lengthbyte = buffer.get() & 0xff;

        if ((lengthbyte & ASN_LONG_LEN) > 0) {
            lengthbyte &= ~ASN_LONG_LEN;
            if (lengthbyte == 0)
                throw new RuntimeException("Indefinite lengths are not supported");

            if (lengthbyte > 8)
                throw new RuntimeException("Data length > 8 bytes are not supported!");

            for (int i = 0; i < lengthbyte; i++)
                length = (length << 8) | (buffer.get() & 0xff);

            if (length < 0)
                throw new RuntimeException("mxflib does not support data lengths > 2^63");
        } else {
            length = lengthbyte & 0xff;
        }
        return length;
    }
}