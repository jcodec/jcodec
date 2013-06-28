package org.jcodec.containers.mxf.read;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jcodec.common.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class IndexEntries {
    private byte[] tOff;
    private byte[] flags;
    private long[] off;

    public IndexEntries(byte[] tOff, byte[] flags, long[] off) {
        this.tOff = tOff;
        this.flags = flags;
        this.off = off;
    }

    public byte[] gettOff() {
        return tOff;
    }

    public byte[] getFlags() {
        return flags;
    }

    public long[] getOff() {
        return off;
    }

    public static IndexEntries read(ByteBuffer bb) {
        bb.order(ByteOrder.BIG_ENDIAN);
        int n = bb.getInt();
        int len = bb.getInt();

        byte[] tOff = new byte[n];
        byte[] flags = new byte[n];
        long[] off = new long[n];

        for (int i = 0; i < n; i++) {
            tOff[i] = bb.get();
            bb.get();
            flags[i] = bb.get();
            off[i] = bb.getLong();
            NIOUtils.skip(bb, len - 11);
        }

        return new IndexEntries(tOff, flags, off);
    }
}
