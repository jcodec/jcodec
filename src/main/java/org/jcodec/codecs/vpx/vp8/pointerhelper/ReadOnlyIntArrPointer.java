package org.jcodec.codecs.vpx.vp8.pointerhelper;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */public class ReadOnlyIntArrPointer implements Comparable<ReadOnlyIntArrPointer> {
    protected final short[] arr;
    protected int pos; // allows changes in subclasses

    public ReadOnlyIntArrPointer(final short[] data, final int pos) {
        if (pos >= data.length || pos < 0) {
            throw new ExceptionInInitializerError(
                    "Tried to initialise a pointer with position outside the backing array");
        }
        this.arr = data;
        this.pos = pos;
    }

    public ReadOnlyIntArrPointer(final ReadOnlyIntArrPointer other) {
        this.arr = other.arr;
        this.pos = other.pos;
    }

    public short get() {
        return arr[pos];
    }

    public short getRel(int r) {
        return arr[pos + r];
    }

    public short getAbs(int p) {
        return arr[p];
    }

    public short getAbs(Enum<?> e) {
        return arr[e.ordinal()];
    }

    public int size() {
        return arr.length;
    }

    public int getRemaining() {
        return arr.length - pos;
    }

    public int getPos() {
        return pos;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ReadOnlyIntArrPointer) {
            ReadOnlyIntArrPointer other = ((ReadOnlyIntArrPointer) obj);
            return other.arr == this.arr && other.pos == this.pos;
        }
        return false;
    }

    public int pointerDiff(ReadOnlyIntArrPointer other) {
        if (other.arr != this.arr) {
            throw new RuntimeException("Tried to calculate a pointer difference on data not backed by the same array");
        }
        return other.pos - this.pos;
    }

    public void memcopyout(int rel, short[] target, int trel, int amount) {
        System.arraycopy(arr, pos + rel, target, trel, amount);
    }

    @Override
    public int compareTo(ReadOnlyIntArrPointer arg0) {
        final boolean sameArray = arg0.arr == arr;
        int compResult = sameArray ? 0 : Integer.compare(arg0.arr.length, arr.length);
        if (compResult == 0 && !sameArray) {
            for (int i = 0; i < arr.length && compResult == 0; i++) {
                compResult = Short.compare(arr[i], arg0.arr[i]);
            }
        }
        return compResult;
    }

}
