package org.jcodec.common;

import static java.lang.System.arraycopy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private int _start;
    private int _size;
    private List<long[]> chunks;
    private int growAmount;

    public static LongArrayList createLongArrayList() {
        return new LongArrayList(DEFAULT_GROW_AMOUNT);
    }

    public LongArrayList(int growAmount) {
        this.chunks = new ArrayList<long[]>();
        this.growAmount = growAmount;
        this.storage = new long[growAmount];
    }

    public long[] toArray() {
        long[] result = new long[_size + chunks.size() * growAmount - _start];
        int off = 0;
        for (int i = 0; i < chunks.size(); i++) {
            long[] chunk = chunks.get(i);
            int aoff = i == 0 ? _start : 0;
            arraycopy(chunk, aoff, result, off, growAmount - aoff);
            off += growAmount;
        }
        int aoff = chunks.size() == 0 ? _start : 0;
        arraycopy(storage, aoff, result, off, _size - aoff);
        return result;
    }

    public void add(long val) {
        if (_size >= storage.length) {
            chunks.add(storage);
            storage = new long[growAmount];
            _size = 0;
        }
        storage[_size++] = val;
    }

    public void push(long id) {
        this.add(id);
    }

    public void pop() {
        if (_size == 0) {
            if (chunks.size() == 0)
                return;
            storage = chunks.remove(chunks.size() - 1);
            _size = growAmount;
        }
        if (chunks.size() == 0 && _size == _start)
            return;
        _size--;
    }

    public void set(int index, long value) {
        index += _start;
        int chunk = index / growAmount;
        int off = index % growAmount;

        if (chunk < chunks.size())
            chunks.get(chunk)[off] = value;
        else
            storage[off] = value;
    }

    public long get(int index) {
        index += _start;
        int chunk = index / growAmount;
        int off = index % growAmount;
        return chunk < chunks.size() ? chunks.get(chunk)[off] : storage[off];
    }
    
    public long shift() {
        if (chunks.size() == 0 && _start >= _size) {
            throw new IllegalStateException();
        }
        long ret = get(0);
        ++_start;
        if (chunks.size() != 0 && _start >= growAmount) {
            chunks.remove(0);
            _start = 0;
        } 
        return ret;
    }

    public void fill(int start, int end, long val) {
        start += _start;
        end += _start;
        while (start < end) {
            int chunk = start / growAmount;
            int off = start % growAmount;
            if (chunk < chunks.size()) {
                int toFill = Math.min(end - start, growAmount - off);
                Arrays.fill(chunks.get(chunk), off, off + toFill, val);
                start += toFill;
            } else if (chunk == chunks.size()) {
                int toFill = Math.min(end - start, growAmount - off);
                Arrays.fill(storage, off, off + toFill, val);
                _size = Math.max(_size, off + toFill);
                start += toFill;
                if (_size == growAmount) {
                    chunks.add(storage);
                    _size = 0;
                    storage = new long[growAmount];
                }
            } else {
                chunks.add(storage);
                _size = 0;
                storage = new long[growAmount];
            }
        }
    }

    public int size() {
        return chunks.size() * growAmount + _size - _start;
    }

    public void addAll(long[] other) {
        int otherOff = 0;
        while (otherOff < other.length) {
            int copyAmount = Math.min(other.length - otherOff, growAmount - _size);
            if (copyAmount < 32) {
                for (int i = 0; i < copyAmount; i++)
                    storage[_size++] = other[otherOff++];
            } else {
                arraycopy(other, otherOff, storage, _size, copyAmount);
                _size += copyAmount;
                otherOff += copyAmount;
            }
            if (otherOff < other.length) {
                chunks.add(storage);
                storage = new long[growAmount];
                _size = 0;
            }
        }
    }

    public void clear() {
        chunks.clear();
        _size = 0;
        _start = 0;
    }

    public boolean contains(long needle) {
        for (int c = 0; c < chunks.size(); c++) {
            long[] chunk = chunks.get(c);
            int coff = c == 0 ? _start : 0;
            for (int i = coff; i < growAmount; i++) {
                if (chunk[i] == needle)
                    return true;
            }
        }
        int coff = chunks.size() == 0 ? _start : 0;
        for (int i = coff; i < _size; i++)
            if (storage[i] == needle)
                return true;
        return false;
    }
}
