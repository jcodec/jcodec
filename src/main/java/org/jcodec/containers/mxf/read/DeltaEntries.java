package org.jcodec.containers.mxf.read;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jcodec.common.NIOUtils;

public class DeltaEntries {

    private byte[] posTabIdx;
    private byte[] slice;
    private int[] elementData;

    public DeltaEntries(byte[] posTabIdx, byte[] slice, int[] elementDelta) {
        this.posTabIdx = posTabIdx;
        this.slice = slice;
        this.elementData = elementDelta;
    }

    public static DeltaEntries read(ByteBuffer bb) {
        bb.order(ByteOrder.BIG_ENDIAN);
        int n = bb.getInt();
        int len = bb.getInt();
        byte[] posTabIdx = new byte[n];
        byte[] slice = new byte[n];
        int[] elementDelta = new int[n];
        for (int i = 0; i < n; i++) {
            posTabIdx[i] = bb.get();
            slice[i] = bb.get();
            elementDelta[i] = bb.getInt();
            NIOUtils.skip(bb, len - 6);
        }
        return new DeltaEntries(posTabIdx, slice, elementDelta);
    }

}
