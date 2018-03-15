package org.jcodec.common;
import org.jcodec.platform.Platform;

import static java.lang.System.arraycopy;

import java.lang.reflect.Array;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class IntObjectMap<T> {
    private static final int GROW_BY = 128;
    private Object[] storage;
    private int _size;

    public IntObjectMap() {
        this.storage = new Object[GROW_BY];
    }

    public void put(int key, T val) {
        if (storage.length <= key) {
            Object[] ns = new Object[key + GROW_BY];
            arraycopy(storage, 0, ns, 0, storage.length);
            storage = ns;
        }
        if (storage[key] == null)
            _size++;
        storage[key] = val;
    }

    @SuppressWarnings("unchecked")
    public T get(int key) {
        return key >= storage.length ? null : (T) storage[key];
    }

    public int[] keys() {
        int[] result = new int[_size];
        for (int i = 0, r = 0; i < storage.length; i++) {
            if (storage[i] != null)
                result[r++] = i;
        }
        return result;
    }

    public void clear() {
        for (int i = 0; i < storage.length; i++)
            storage[i] = null;
        _size = 0;
    }

    public int size() {
        return _size;
    }

    public void remove(int key) {
        if (storage[key] != null)
            _size--;
        storage[key] = null;
    }

    @SuppressWarnings("unchecked")
    public T[] values(T[] runtime) {
        T[] result = (T[]) Array.newInstance(Platform.arrayComponentType(runtime), _size);
        for (int i = 0, r = 0; i < storage.length; i++) {
            if (storage[i] != null)
                result[r++] = (T) storage[i];
        }
        return result;
    }
}