package org.jcodec.codecs.h264.io.write;

import static org.jcodec.common.tools.Debug.print;
import static org.jcodec.common.tools.Debug.println;

import java.io.IOException;

import org.jcodec.common.io.OutBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A class responsible for outputting exp-Golumb values into binary stream
 * 
 * @author Jay Codec
 * 
 */
public class CAVLCWriter {

    private CAVLCWriter() {
    }

    public static void writeU(OutBits out, int value, int n, String string) throws IOException {
        print(string + "\t");
        out.writeNBit(value, n);
        println("\t" + value);
    }

    public static void writeUE(OutBits out, int value) throws IOException {
        int bits = 0;
        int cumul = 0;
        for (int i = 0; i < 15; i++) {
            if (value < cumul + (1 << i)) {
                bits = i;
                break;
            }
            cumul += (1 << i);
        }
        out.writeNBit(0, bits);
        out.write1Bit(1);
        out.writeNBit(value - cumul, bits);
    }

    public static void writeUE(OutBits out, int value, String string) throws IOException {
        print(string + "\t");
        writeUE(out, value);
        println("\t" + value);
    }

    public static void writeSE(OutBits out, int value, String string) throws IOException {
        print(string + "\t");
        int sign = (value >> 31);
        int mod = (value ^ sign) - sign;
        writeUE(out, (mod << 1) - (sign + 1));
        println("\t" + value);
    }

    public static void writeBool(OutBits out, boolean value, String string) throws IOException {
        print(string + "\t");
        out.write1Bit(value ? 1 : 0);
        println("\t" + value);
    }

    public static void writeU(OutBits out, int i, int n) throws IOException {
        out.writeNBit(i, n);
    }

    public static void writeNBit(OutBits out, long value, int n, String string) throws IOException {
        print(string + "\t");
        for (int i = 0; i < n; i++) {
            out.write1Bit((int) (value >> (n - i - 1)) & 0x1);
        }
        println("\t" + value);
    }

    public static void writeTrailingBits(OutBits out) throws IOException {
        out.write1Bit(1);
        out.flush();
    }

    public static void writeSliceTrailingBits() {
        throw new IllegalStateException("todo");
    }
}