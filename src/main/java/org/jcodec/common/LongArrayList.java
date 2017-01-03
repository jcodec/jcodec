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
public class LongArrayList {

    private static final int DEFAULT_GROW_AMOUNT = 128;

    private long[] storage;
    private int limit;
    private int start;
    private int growAmount;

    public static LongArrayList createLongArrayList() {
        return new LongArrayList(DEFAULT_GROW_AMOUNT);
    }

    public LongArrayList(int growAmount) {
        this.growAmount = growAmount;
        this.storage = new long[growAmount];
    }

    public long[] toArray() {
        long[] result = new long[limit - start];
        arraycopy(storage, start, result, 0, limit - start);
        return result;
    }

    public void add(long val) {
        if (limit > storage.length - 1) {
            long[] ns = new long[storage.length + growAmount - start];
            arraycopy(storage, start, ns, 0, storage.length - start);
            storage = ns;
            limit -= start;
            start = 0;
        }
        storage[limit++] = val;
    }
    
    public void push(long id) {
        this.add(id);
    }
    
    public long pop() {
        if (limit <= start)
            throw new IllegalStateException();
        return storage[limit--];
    }

    public void set(int index, int value) {
        storage[index + start] = value;
    }

    public long get(int index) {
        return storage[index + start];
    }
    
    public long shift() {
        if(start >= limit)
            throw new IllegalStateException();
        return storage[start++];
    }

    public void fill(int from, int to, int val) {
        if (to > storage.length) {
            long[] ns = new long[to + growAmount - start];
            arraycopy(storage, start, ns, 0, storage.length - start);
            storage = ns;
        }
        Arrays.fill(storage, from, to, val);
        limit = Math.max(limit, to);
    }

    public int size() {
        return limit - start;
    }

    public void addAll(long[] other) {
        if (limit + other.length >= storage.length) {
            long[] ns = new long[limit + growAmount + other.length - start];
            arraycopy(storage, start, ns, 0, limit);
            storage = ns;
        }
        arraycopy(other, 0, storage, limit, other.length);
        limit += other.length;
    }

    public void clear() {
        limit = 0;
        start = 0;
    }
    
    public boolean contains(long needle) {
        for (int i = start; i < limit; i++)
            if (storage[i] == needle)
                return true;
        return false;
    }
}
