package org.jcodec.containers.mxf.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class IndexEntries {
    private byte[] displayOff;
    private byte[] flags;
    private long[] fileOff;
    private byte[] keyFrameOff;

    public IndexEntries(byte[] displayOff, byte[] keyFrameOff, byte[] flags, long[] fileOff) {
        this.displayOff = displayOff;
        this.keyFrameOff = keyFrameOff;
        this.flags = flags;
        this.fileOff = fileOff;
    }

    public byte[] getDisplayOff() {
        return displayOff;
    }

    public byte[] getFlags() {
        return flags;
    }

    public long[] getFileOff() {
        return fileOff;
    }

    public byte[] getKeyFrameOff() {
        return keyFrameOff;
    }

    public static IndexEntries read(ByteBuffer bb) {
        bb.order(ByteOrder.BIG_ENDIAN);
        int n = bb.getInt();
        int len = bb.getInt();

        int[] temporalOff = new int[n];
        byte[] flags = new byte[n];
        long[] fileOff = new long[n];
        byte[] keyFrameOff = new byte[n];

        for (int i = 0; i < n; i++) {
            temporalOff[i] = i + bb.get();
            keyFrameOff[i] = bb.get();
            flags[i] = bb.get();
            fileOff[i] = bb.getLong();
            NIOUtils.skip(bb, len - 11);
        }
        byte[] displayOff = new byte[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (temporalOff[j] == i) {
                    displayOff[i] = (byte) (j - i);
                    break;
                }
            }
        }

        return new IndexEntries(displayOff, keyFrameOff, flags, fileOff);
    }
}