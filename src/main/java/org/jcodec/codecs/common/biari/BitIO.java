package org.jcodec.codecs.common.biari;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * All the IO routines related to bits
 * 
 * @author Jay Codec
 * 
 */
public class BitIO {

    public static interface InputBits {
        public int getBit() throws IOException;

    }

    public static interface OutputBits {
        public void putBit(int symbol) throws IOException;

        public void flush() throws IOException;
    }

    public static InputBits inputFromStream(InputStream is) {
        return new StreamInputBits(is);
    }

    public static OutputBits outputFromStream(OutputStream out) {
        return new StreamOutputBits(out);
    }

    public static InputBits inputFromArray(byte[] bytes) {
        return new StreamInputBits(new ByteArrayInputStream(bytes));
    }

    public static OutputBits outputFromArray(final byte[] bytes) {
        return new StreamOutputBits(new OutputStream() {
            int ptr;

            public void write(int b) throws IOException {
                if (ptr >= bytes.length)
                    throw new IOException("Buffer is full");
                bytes[ptr++] = (byte) b;
            }
        });
    }

    public static byte[] compressBits(int[] decompressed) {
        byte[] compressed = new byte[(decompressed.length >> 3) + 1];
        OutputBits out = outputFromArray(compressed);
        try {
            for (int bit : decompressed) {
                out.putBit(bit);
            }
        } catch (IOException e) {
        }

        return compressed;
    }

    public static int[] decompressBits(byte[] compressed) {
        int[] decompressed = new int[compressed.length << 3];
        InputBits inputFromArray = inputFromArray(compressed);
        int read;
        try {
            for (int i = 0; (read = inputFromArray.getBit()) != -1; i++) {
                decompressed[i] = read;
            }
        } catch (IOException e) {
        }
        return decompressed;
    }

    public static class StreamInputBits implements InputBits {
        private InputStream in;
        private int cur;
        private int bit;

        public StreamInputBits(InputStream in) {
            this.in = in;
            this.bit = 8;
        }

        public int getBit() throws IOException {
            if (bit > 7) {
                cur = in.read();
                if (cur == -1)
                    return -1;
                bit = 0;
            }
            return (cur >> (7 - bit++)) & 0x1;
        }
    }

    public static class StreamOutputBits implements OutputBits {
        private OutputStream out;
        private int cur;
        private int bit;

        public StreamOutputBits(OutputStream out) {
            this.out = out;
        }

        public void putBit(int symbol) throws IOException {
            if (bit > 7) {
                out.write(cur);
                cur = 0;
                bit = 0;
            }
            cur |= (symbol & 0x1) << (7 - bit++);
        }

        public void flush() throws IOException {
            if (bit > 0)
                out.write(cur);
        }
    }
}