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
public class ByteArrayList {
    private static final int DEFAULT_GROW_AMOUNT = 2048;

    private byte[] storage;
    private int _start;
    private int _size;
    private List<byte[]> chunks;
    private int growAmount;

    public static ByteArrayList createByteArrayList() {
        return new ByteArrayList(DEFAULT_GROW_AMOUNT);
    }

    public ByteArrayList(int growAmount) {
        this.chunks = new ArrayList<byte[]>();
        this.growAmount = growAmount;
        this.storage = new byte[growAmount];
    }

    public byte[] toArray() {
        byte[] result = new byte[_size + chunks.size() * growAmount - _start];
        int off = 0;
        for (int i = 0; i < chunks.size(); i++) {
            byte[] chunk = chunks.get(i);
            int aoff = i == 0 ? _start : 0;
            arraycopy(chunk, aoff, result, off, growAmount - aoff);
            off += growAmount;
        }
        int aoff = chunks.size() == 0 ? _start : 0;
        arraycopy(storage, aoff, result, off, _size - aoff);
        return result;
    }

    public void add(byte val) {
        if (_size >= storage.length) {
            chunks.add(storage);
            storage = new byte[growAmount];
            _size = 0;
        }
        storage[_size++] = val;
    }

    public void push(byte id) {
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

    public void set(int index, byte value) {
        index += _start;
        int chunk = index / growAmount;
        int off = index % growAmount;

        if (chunk < chunks.size())
            chunks.get(chunk)[off] = value;
        else
            storage[off] = value;
    }

    public byte get(int index) {
        index += _start;
        int chunk = index / growAmount;
        int off = index % growAmount;
        return chunk < chunks.size() ? chunks.get(chunk)[off] : storage[off];
    }
    
    public byte shift() {
        if (chunks.size() == 0 && _start >= _size) {
            throw new IllegalStateException();
        }
        byte ret = get(0);
        ++_start;
        if (chunks.size() != 0 && _start >= growAmount) {
            chunks.remove(0);
            _start = 0;
        } 
        return ret;
    }

    public void fill(int start, int end, byte val) {
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
                    storage = new byte[growAmount];
                }
            } else {
                chunks.add(storage);
                _size = 0;
                storage = new byte[growAmount];
            }
        }
    }

    public int size() {
        return chunks.size() * growAmount + _size - _start;
    }

    public void addAll(byte[] other) {
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
                storage = new byte[growAmount];
                _size = 0;
            }
        }
    }

    public void clear() {
        chunks.clear();
        _size = 0;
        _start = 0;
    }

    public boolean contains(byte needle) {
        for (int c = 0; c < chunks.size(); c++) {
            byte[] chunk = chunks.get(c);
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
