package org.jcodec.containers.mxf.model;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * An UL class that wraps UL bytes, introduced to implement custom comparison
 * rules
 * 
 * @author The JCodec project
 * 
 */
public class UL {
    private byte[] bytes;

    public UL(byte... arguments) {
        this.bytes = arguments;
    }

    public UL(int... arguments) {
        this.bytes = new byte[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            this.bytes[i] = (byte) arguments[i];
        }
    }

    @Override
    public int hashCode() {
        return ((bytes[4] & 0xff) << 24) | ((bytes[5] & 0xff) << 16) | ((bytes[6] & 0xff) << 8) | (bytes[7] & 0xff);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UL))
            return false;

        byte[] other = ((UL) obj).bytes;

        for (int i = 4; i < Math.min(bytes.length, other.length); i++)
            if (bytes[i] != other[i])
                return false;

        return true;
    }

    public boolean maskEquals(UL o, int mask) {
        if(o == null)
            return false;
        byte[] other = ((UL) o).bytes;
        mask >>= 4;
        for (int i = 4; i < Math.min(bytes.length, other.length); i++, mask >>= 1)
            if ((mask & 0x1) == 1 && bytes[i] != other[i])
                return false;

        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("06:0E:2B:34:");
        for (int i = 4; i < bytes.length; i++) {
            sb.append(hex((bytes[i] >> 4) & 0xf));
            sb.append(hex(bytes[i] & 0xf));
            if (i < bytes.length - 1)
                sb.append(":");
        }
        return sb.toString();
    }

    private char hex(int i) {
        return (char) (i < 10 ? '0' + i : 'A' + (i - 10));
    }

    public int get(int i) {
        return bytes[i];
    }

    public static UL read(ByteBuffer _bb) {
        byte[] umid = new byte[16];
        _bb.get(umid);
        return new UL(umid);
    }
}
