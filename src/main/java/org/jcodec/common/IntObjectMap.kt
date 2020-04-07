package org.jcodec.common

import org.jcodec.platform.Platform

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class IntObjectMap<T> {
    private var storage: Array<Any?>
    private var _size = 0
    fun put(key: Int, `val`: T) {
        if (storage.size <= key) {
            val ns = arrayOfNulls<Any>(key + GROW_BY)
            System.arraycopy(storage, 0, ns, 0, storage.size)
            storage = ns
        }
        if (storage[key] == null) _size++
        storage[key] = `val`
    }

    operator fun get(key: Int): T? {
        return if (key >= storage.size) null else storage[key] as T?
    }

    fun keys(): IntArray {
        val result = IntArray(_size)
        var i = 0
        var r = 0
        while (i < storage.size) {
            if (storage[i] != null) result[r++] = i
            i++
        }
        return result
    }

    fun clear() {
        for (i in storage.indices) storage[i] = null
        _size = 0
    }

    fun size(): Int {
        return _size
    }

    fun remove(key: Int) {
        if (storage[key] != null) _size--
        storage[key] = null
    }

    fun values(runtime: Array<T>?): Array<T?> {
        val result = java.lang.reflect.Array.newInstance(Platform.arrayComponentType(runtime), _size) as Array<T?>
        var i = 0
        var r = 0
        while (i < storage.size) {
            if (storage[i] != null) result[r++] = storage[i] as T?
            i++
        }
        return result
    }

    companion object {
        private const val GROW_BY = 128
    }

    init {
        storage = arrayOfNulls(GROW_BY)
    }
}