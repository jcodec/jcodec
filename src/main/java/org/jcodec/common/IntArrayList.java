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
    private int size;
    private int growAmount;

    public IntArrayList() {
        this(DEFAULT_GROW_AMOUNT);
    }

    public IntArrayList(int growAmount) {
        this.growAmount = growAmount;
        this.storage = new int[growAmount];
    }

    public int[] toArray() {
        int[] result = new int[size];
        arraycopy(storage, 0, result, 0, size);
        return result;
    }

    public void add(int val) {
        if (size >= storage.length) {
            int[] ns = new int[storage.length + growAmount];
            arraycopy(storage, 0, ns, 0, storage.length);
            storage = ns;
        }
        storage[size++] = val;
    }

    public void push(int id) {
        this.add(id);
    }

    public void pop() {
        if (size == 0)
            return;
        size--;
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
        size = Math.max(size, end);
    }

    public int size() {
        return size;
    }

    public void addAll(int[] other) {
        if (size + other.length >= storage.length) {
            int[] ns = new int[size + growAmount + other.length];
            arraycopy(storage, 0, ns, 0, size);
            storage = ns;
        }
        arraycopy(other, 0, storage, size, other.length);
        size += other.length;
    }

    public void clear() {
        size = 0;
    }

    public boolean contains(int needle) {
        for (int i = 0; i < size; i++)
            if (storage[i] == needle)
                return true;
        return false;
    }
}