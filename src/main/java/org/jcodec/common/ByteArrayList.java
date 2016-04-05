package org.jcodec.common;
import static js.lang.System.arraycopy;

import js.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ByteArrayList {
    private static final int DEFAULT_GROW_AMOUNT = 2048;
    
    public static ByteArrayList createByteArrayList() {
        return new ByteArrayList(DEFAULT_GROW_AMOUNT);
    }

    private byte[] storage;
    private int _size;
    private int growAmount;


    public ByteArrayList(int growAmount) {
        this.growAmount = growAmount;
        this.storage = new byte[growAmount];
    }

    public byte[] toArray() {
        byte[] result = new byte[_size];
        arraycopy(storage, 0, result, 0, _size);
        return result;
    }

    public void add(byte val) {
        if (_size >= storage.length) {
            byte[] ns = new byte[storage.length + growAmount];
            arraycopy(storage, 0, ns, 0, storage.length);
            storage = ns;
        }
        storage[_size++] = val;
    }
    
    public void push(byte id) {
        this.add(id);
    }
    
    public void pop() {
        if (_size == 0)
            return;
        _size--;
    }

    public void set(int index, byte value) {
        storage[index] = value;
    }

    public byte get(int index) {
        return storage[index];
    }

    public void fill(int start, int end, byte val) {
        if (end > storage.length) {
            byte[] ns = new byte[end + growAmount];
            arraycopy(storage, 0, ns, 0, storage.length);
            storage = ns;
        }
        Arrays.fill(storage, start, end, val);
        _size = Math.max(_size, end);
    }

    public int size() {
        return _size;
    }

    public void addAll(byte[] other) {
        if (_size + other.length >= storage.length) {
            byte[] ns = new byte[_size + growAmount + other.length];
            arraycopy(storage, 0, ns, 0, _size);
            storage = ns;
        }
        arraycopy(other, 0, storage, _size, other.length);
        _size += other.length;
    }
    
    public boolean contains(byte needle) {
        for (int i = 0; i < _size; i++)
            if (storage[i] == needle)
                return true;
        return false;
    }
}
