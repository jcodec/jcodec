package org.jcodec.common;

import java.util.Arrays;

import static java.lang.System.arraycopy;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class IntArrayList {
    private static final int DEFAULT_GROW_AMOUNT = 128;
 
    private int[] storage;
    private int _size;
    private int growAmount;

    public static IntArrayList createIntArrayList() {
        return new IntArrayList(DEFAULT_GROW_AMOUNT);
    }

    public IntArrayList(int growAmount) {
        this.growAmount = growAmount;
        this.storage = new int[growAmount];
    }

    public int[] toArray() {
        int[] result = new int[_size];
        arraycopy(storage, 0, result, 0, _size);
        return result;
    }

    public void add(int val) {
        if (_size >= storage.length) {
            int[] ns = new int[storage.length + growAmount];
            arraycopy(storage, 0, ns, 0, storage.length);
            storage = ns;
        }
        storage[_size++] = val;
    }

    public void push(int id) {
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

    public int get(int index) {
        return storage[index];
    }

    public void fill(int start, int end, int val) {
        if (end > storage.length) {
            int[] ns = new int[end + growAmount];
            arraycopy(storage, 0, ns, 0, storage.length);
            storage = ns;
        }
        Arrays.fill(storage, start, end, val);
        _size = Math.max(_size, end);
    }

    public int size() {
        return _size;
    }

    public void addAll(int[] other) {
        if (_size + other.length >= storage.length) {
            int[] ns = new int[_size + growAmount + other.length];
            arraycopy(storage, 0, ns, 0, _size);
            storage = ns;
        }
        arraycopy(other, 0, storage, _size, other.length);
        _size += other.length;
    }

    public void clear() {
        _size = 0;
    }

    public boolean contains(int needle) {
        for (int i = 0; i < _size; i++)
            if (storage[i] == needle)
                return true;
        return false;
    }
}