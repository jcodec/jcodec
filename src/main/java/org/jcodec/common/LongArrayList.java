package org.jcodec.common;

import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class LongArrayList {

    private static final int GROW_AMOUNT = 128;
    private long[] storage;
    private int size;

    public LongArrayList() {
        this.storage = new long[GROW_AMOUNT];
    }

    public long[] toArray() {
        long[] result = new long[size];
        System.arraycopy(storage, 0, result, 0, size);
        return result;
    }

    public void add(long val) {
        if (size >= storage.length) {
            long[] ns = new long[storage.length + GROW_AMOUNT];
            System.arraycopy(storage, 0, ns, 0, storage.length);
            storage = ns;
        }
        storage[size++] = val;
    }

    public void set(int index, int value) {
        storage[index] = value;
    }

    public long get(int index) {
        return storage[index];
    }

    public void fill(int start, int end, int val) {
        if (end > storage.length) {
            long[] ns = new long[end + GROW_AMOUNT];
            System.arraycopy(storage, 0, ns, 0, storage.length);
            storage = ns;
        }
        Arrays.fill(storage, start, end, val);
        size = Math.max(size, end);
    }

    public int size() {
        return size;
    }

    public void addAll(long[] other) {
        if (size + other.length >= storage.length) {
            long[] ns = new long[size + GROW_AMOUNT + other.length];
            System.arraycopy(storage, 0, ns, 0, size);
            storage = ns;
        }
        System.arraycopy(other, 0, storage, size, other.length);
        size += other.length;
    }

}
