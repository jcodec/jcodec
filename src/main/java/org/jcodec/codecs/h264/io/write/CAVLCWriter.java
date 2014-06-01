package org.jcodec.codecs.h264.io.write;

import static org.jcodec.common.tools.Debug.print;
import static org.jcodec.common.tools.Debug.println;
import static org.jcodec.common.tools.Debug.trace;

import org.jcodec.common.io.BitWriter;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A class responsible for outputting exp-Golomb values into binary stream
 * 
 * @author Jay Codec
 * 
 */
public class CAVLCWriter {

    private CAVLCWriter() {
    }

    public static void writeU(BitWriter out, int value, int n, String message)  {
        out.writeNBit(value, n);
        trace(message, value);
    }

    public static void writeUE(BitWriter out, int value)  {
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
    
    public static void writeSE(BitWriter out, int value)  {
        writeUE(out, MathUtil.golomb(value));
    }

    public static void writeUE(BitWriter out, int value, String message)  {
        writeUE(out, value);
        trace(message, value);
    }

    public static void writeSE(BitWriter out, int value, String message)  {
        writeUE(out, MathUtil.golomb(value));
        trace(message, value);
    }

    public static void writeBool(BitWriter out, boolean value, String message)  {
        out.write1Bit(value ? 1 : 0);
        trace(message, value ? 1 : 0);
    }
    
    public static void writeBool(BitWriter out, boolean value)  {
        out.write1Bit(value ? 1 : 0);
    }

    public static void writeU(BitWriter out, int value, int n)  {
        out.writeNBit(value, n);
    }

    public static void writeNBit(BitWriter out, long value, int n, String message)  {
        for (int i = 0; i < n; i++) {
            out.write1Bit((int) (value >> (n - i - 1)) & 0x1);
        }
        trace(message, value);
    }

    public static void writeTrailingBits(BitWriter out)  {
        out.write1Bit(1);
        out.flush();
    }

    public static void writeSliceTrailingBits() {
        throw new IllegalStateException("todo");
    }
}