package org.jcodec.common;

import static java.lang.System.arraycopy;

import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class LongArrayList {

    private static final int DEFAULT_GROW_AMOUNT = 128;

    private long[] storage;
    private int _size;
    private int growAmount;

    public static LongArrayList createLongArrayList() {
        return new LongArrayList(DEFAULT_GROW_AMOUNT);
    }

    public LongArrayList(int growAmount) {
        this.growAmount = growAmount;
        this.storage = new long[growAmount];
    }

    public long[] toArray() {
        long[] result = new long[_size];
        arraycopy(storage, 0, result, 0, _size);
        return result;
    }

    public void add(long val) {
        if (_size >= storage.length) {
            long[] ns = new long[storage.length + growAmount];
            arraycopy(storage, 0, ns, 0, storage.length);
            storage = ns;
        }
        storage[_size++] = val;
    }
    
    public void push(long id) {
        this.add(id);
    }
    
    public void pop() {
        if (_size == 0)
            return;
        _size--;
    }

    public void set(int index, int value) {
        storage[index] = value;
    }

    public long get(int index) {
        return storage[index];
    }

    public void fill(int start, int end, int val) {
        if (end > storage.length) {
            long[] ns = new long[end + growAmount];
            arraycopy(storage, 0, ns, 0, storage.length);
            storage = ns;
        }
        Arrays.fill(storage, start, end, val);
        _size = Math.max(_size, end);
    }

    public int size() {
        return _size;
    }

    public void addAll(long[] other) {
        if (_size + other.length >= storage.length) {
            long[] ns = new long[_size + growAmount + other.length];
            arraycopy(storage, 0, ns, 0, _size);
            storage = ns;
        }
        arraycopy(other, 0, storage, _size, other.length);
        _size += other.length;
    }

    public void clear() {
        _size = 0;
    }
    
    public boolean contains(long needle) {
        for (int i = 0; i < _size; i++)
            if (storage[i] == needle)
                return true;
        return false;
    }
}
