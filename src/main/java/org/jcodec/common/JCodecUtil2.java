package org.jcodec.common;

import org.jcodec.common.tools.MathUtil;

import java.nio.ByteBuffer;

public class JCodecUtil2 {
    public static void writeBER32(ByteBuffer buffer, int value) {
        buffer.put((byte) ((value >> 21) | 0x80));
        buffer.put((byte) ((value >> 14) | 0x80));
        buffer.put((byte) ((value >> 7) | 0x80));
        buffer.put((byte) (value & 0x7F));
    }

    public static int readBER32(ByteBuffer input) {
        int size = 0;
        for (int i = 0; i < 4; i++) {
            byte b = input.get();
            size = (size << 7) | (b & 0x7f);
            if (((b & 0xff) >> 7) == 0)
                break;
        }
        return size;
    }

    public static void writeBER32Var(ByteBuffer bb, int value) {
        for (int i = 0, bits = MathUtil.log2(value); i < 4 && bits > 0; i++) {
            bits -= 7;
            int out = value >> bits;
            if(bits > 0)
                out |= 0x80;
            bb.put((byte)out);
        }
    }

    public static byte[] asciiString(String fourcc) {
        char[] ch = fourcc.toCharArray();
        byte[] result = new byte[ch.length];
        for (int i = 0; i < ch.length; i++) {
            result[i] = (byte) ch[i];
        }
        return result;
    }

    public static int[] getAsIntArray(ByteBuffer yuv, int size) {
        byte[] b = new byte[size];
        int[] result = new int[size];
        yuv.get(b);
        for (int i = 0; i < b.length; i++) {
            result[i] = b[i] & 0xff;
        }
        return result;
    }

    public static String removeExtension(String name) {
        if(name == null)
            return null;
        return name.replaceAll("\\.[^\\.]+$", "");
    }
}
