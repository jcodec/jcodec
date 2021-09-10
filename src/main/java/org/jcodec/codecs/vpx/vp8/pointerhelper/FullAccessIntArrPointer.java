package org.jcodec.codecs.vpx.vp8.pointerhelper;

import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */public class FullAccessIntArrPointer extends PositionableIntArrPointer {

    public FullAccessIntArrPointer(int size) {
        super(new short[size], 0);
    }

    private FullAccessIntArrPointer(short[] a, int p) {
        super(a, p);
    }

    public short set(short v) {
        return arr[pos] = v;
    }

    public short setAndInc(short v) {
        return arr[pos++] = v;
    }

    public short setRel(int r, short v) {
        return arr[pos + r] = v;
    }

    public short setAbs(int l, short v) {
        return arr[l] = v;
    }

    public short setAbs(Enum<?> e, short v) {
        return arr[e.ordinal()] = v;
    }

    public void memcopyin(int rel, ReadOnlyIntArrPointer other, int otRel, int len) {
        System.arraycopy(other.arr, other.pos + otRel, this.arr, pos + rel, len);
    }

    public void memcopyin(int rel, short[] other, int otRel, int len) {
        System.arraycopy(other, otRel, this.arr, pos + rel, len);
    }

    public void memset(int rel, short v, int len) {
        final int start = pos + rel;
        Arrays.fill(arr, start, start + len, v);
    }

    public FullAccessIntArrPointer shallowCopy() {
        return new FullAccessIntArrPointer(this.arr, this.pos);
    }

    public FullAccessIntArrPointer shallowCopyWithPosInc(int by) {
        return new FullAccessIntArrPointer(this.arr, this.pos + by);
    }

    public FullAccessIntArrPointer deepCopy() {
        return new FullAccessIntArrPointer(Arrays.copyOf(this.arr, this.arr.length), pos);
    }

    public ReadOnlyIntArrPointer readOnly() {
        return new ReadOnlyIntArrPointer(this);
    }

    public PositionableIntArrPointer positionableOnly() {
        return new PositionableIntArrPointer(this);
    }

    public static FullAccessIntArrPointer toPointer(short[] data) {
        return new FullAccessIntArrPointer(data, 0);
    }
}
