package org.jcodec.containers.mxf.model;
import org.jcodec.common.StringUtils;
import org.jcodec.platform.Platform;

import static org.jcodec.common.Preconditions.checkNotNull;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * An UL class that wraps UL bytes, introduced to implement custom comparison
 * rules
 * 
 * <p>
 * SMPTE 298-2009
 * </p>
 * 4.2 SMPTE-Administered Universal Label A fixed-length (16-byte) universal
 * label, defined by this standard and administered by SMPTE.
 * 
 * @author The JCodec project
 * 
 */
public class UL {
    private final byte[] bytes;

    public UL(byte[] bytes) {
        checkNotNull(bytes);
        this.bytes = bytes;
    }

    public static UL newULFromInts(int[] args) {
        byte[] bytes = new byte[args.length];
        for (int i = 0; i < args.length; i++) {
            bytes[i] = (byte) args[i];
        }

        return new UL(bytes);
    }
    
    public static UL newUL(String ul) {
        checkNotNull(ul);
        String[] split = StringUtils.splitS(ul, ".");
        byte b[] = new byte[split.length];
        for (int i = 0; i < split.length; i++) {
            int parseInt = Integer.parseInt(split[i], 16);
            b[i] = (byte) parseInt;
        }
        return new UL(b);
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
        byte[] other = o.bytes;
        mask >>= 4;
        for (int i = 4; i < Math.min(bytes.length, other.length); i++, mask >>= 1)
            if ((mask & 0x1) == 1 && bytes[i] != other[i])
                return false;

        return true;
    }

    private final static char[] hex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    @Override
    public String toString() {
        if (bytes.length == 0) return "";
        char[] str = new char[bytes.length * 3 - 1];
        int i = 0;
        int j = 0;
        for (i = 0; i < bytes.length - 1; i++) {
            str[j++] = hex[(bytes[i] >> 4) & 0xf];
            str[j++] = hex[bytes[i] & 0xf];
            str[j++] = '.';
        }
        str[j++] = hex[(bytes[i] >> 4) & 0xf];
        str[j++] = hex[bytes[i] & 0xf];
        return Platform.stringFromChars(str);
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
