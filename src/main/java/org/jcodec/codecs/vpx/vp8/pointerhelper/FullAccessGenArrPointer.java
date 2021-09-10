package org.jcodec.codecs.vpx.vp8.pointerhelper;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */public class FullAccessGenArrPointer<T> {
    ArrayList<T> arr;
    int pos;
    private int savedPos;

    public FullAccessGenArrPointer(int size) {
        arr = new ArrayList<T>(size);
        for (int i = 0; i < size; i++)
            arr.add(null);
    }

    public FullAccessGenArrPointer(final T[] data, final int pos) {
        this.arr = new ArrayList<T>();
        this.arr.addAll(Arrays.asList(data));
        this.pos = pos;
        savedPos = pos;
    }

    public FullAccessGenArrPointer(final FullAccessGenArrPointer<T> other) {
        this.arr = other.arr;
        this.pos = other.pos;
        this.savedPos = other.savedPos;
    }

    public T get() {
        return arr.get(pos);
    }

    public T getRel(int r) {
        return arr.get(pos + r);
    }

    public int size() {
        return arr.size();
    }

    public void dec() {
        pos--;
    }

    public T getAndInc() {
        return arr.get(pos++);
    }

    public void inc() {
        pos++;
    }

    public void incBy(int r) {
        pos += r;
    }

    public void rewind() {
        pos = 0;
    }

    public void rewindToSaved() {
        pos = savedPos;
    }

    public void savePos() {
        savedPos = pos;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int p) {
        pos = p;
    }

    public T set(T v) {
        arr.set(pos, v);
        return v;
    }

    public T setAndInc(T v) {
        arr.set(pos++, v);
        return v;
    }

    public T setRel(int r, T v) {
        arr.set(pos + r, v);
        return v;
    }

    public void memset(int rel, T v, int len) {
        final int start = pos + rel;
        final int stop = start + len;
        for (int i = start; i < stop; i++) {
            arr.set(i, v);
        }
    }

    public FullAccessGenArrPointer<T> shallowCopy() {
        return new FullAccessGenArrPointer<T>(this);
    }

    public FullAccessGenArrPointer<T> shallowCopyWithPosInc(int by) {
        FullAccessGenArrPointer<T> ret = shallowCopy();
        ret.incBy(by);
        return ret;
    }

    public int pointerDiff(FullAccessGenArrPointer<T> other) {
        return other.pos - this.pos;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FullAccessGenArrPointer) {
            FullAccessGenArrPointer<T> other = ((FullAccessGenArrPointer<T>) obj);
            return other.arr == this.arr && other.pos == this.pos;
        }
        return false;
    }

}
