package org.jcodec.common

import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class LongArrayList(growAmount: Int) {
    private var storage: LongArray
    private var _start = 0
    private var _size = 0
    private val chunks: MutableList<LongArray>
    private val growAmount: Int
    fun toArray(): LongArray {
        val result = LongArray(_size + chunks.size * growAmount - _start)
        var off = 0
        for (i in chunks.indices) {
            val chunk = chunks[i]
            val aoff = if (i == 0) _start else 0
            System.arraycopy(chunk, aoff, result, off, growAmount - aoff)
            off += growAmount
        }
        val aoff = if (chunks.size == 0) _start else 0
        System.arraycopy(storage, aoff, result, off, _size - aoff)
        return result
    }

    fun add(`val`: Long) {
        if (_size >= storage.size) {
            chunks.add(storage)
            storage = LongArray(growAmount)
            _size = 0
        }
        storage[_size++] = `val`
    }

    fun push(id: Long) {
        add(id)
    }

    fun pop() {
        if (_size == 0) {
            if (chunks.size == 0) return
            storage = chunks.removeAt(chunks.size - 1)
            _size = growAmount
        }
        if (chunks.size == 0 && _size == _start) return
        _size--
    }

    operator fun set(index: Int, value: Long) {
        var index = index
        index += _start
        val chunk = index / growAmount
        val off = index % growAmount
        if (chunk < chunks.size) chunks[chunk][off] = value else storage[off] = value
    }

    operator fun get(index: Int): Long {
        var index = index
        index += _start
        val chunk = index / growAmount
        val off = index % growAmount
        return if (chunk < chunks.size) chunks[chunk][off] else storage[off]
    }

    fun shift(): Long {
        check(!(chunks.size == 0 && _start >= _size))
        val ret = get(0)
        ++_start
        if (chunks.size != 0 && _start >= growAmount) {
            chunks.removeAt(0)
            _start = 0
        }
        return ret
    }

    fun fill(start: Int, end: Int, `val`: Long) {
        var start = start
        var end = end
        start += _start
        end += _start
        while (start < end) {
            val chunk = start / growAmount
            val off = start % growAmount
            if (chunk < chunks.size) {
                val toFill = Math.min(end - start, growAmount - off)
                Arrays.fill(chunks[chunk], off, off + toFill, `val`)
                start += toFill
            } else if (chunk == chunks.size) {
                val toFill = Math.min(end - start, growAmount - off)
                Arrays.fill(storage, off, off + toFill, `val`)
                _size = Math.max(_size, off + toFill)
                start += toFill
                if (_size == growAmount) {
                    chunks.add(storage)
                    _size = 0
                    storage = LongArray(growAmount)
                }
            } else {
                chunks.add(storage)
                _size = 0
                storage = LongArray(growAmount)
            }
        }
    }

    fun size(): Int {
        return chunks.size * growAmount + _size - _start
    }

    fun addAll(other: LongArray) {
        var otherOff = 0
        while (otherOff < other.size) {
            val copyAmount = Math.min(other.size - otherOff, growAmount - _size)
            if (copyAmount < 32) {
                for (i in 0 until copyAmount) storage[_size++] = other[otherOff++]
            } else {
                System.arraycopy(other, otherOff, storage, _size, copyAmount)
                _size += copyAmount
                otherOff += copyAmount
            }
            if (otherOff < other.size) {
                chunks.add(storage)
                storage = LongArray(growAmount)
                _size = 0
            }
        }
    }

    fun clear() {
        chunks.clear()
        _size = 0
        _start = 0
    }

    operator fun contains(needle: Long): Boolean {
        for (c in chunks.indices) {
            val chunk = chunks[c]
            val coff = if (c == 0) _start else 0
            for (i in coff until growAmount) {
                if (chunk[i] == needle) return true
            }
        }
        val coff = if (chunks.size == 0) _start else 0
        for (i in coff until _size) if (storage[i] == needle) return true
        return false
    }

    companion object {
        private const val DEFAULT_GROW_AMOUNT = 128
        @JvmStatic
        fun createLongArrayList(): LongArrayList {
            return LongArrayList(DEFAULT_GROW_AMOUNT)
        }
    }

    init {
        chunks = ArrayList()
        this.growAmount = growAmount
        storage = LongArray(growAmount)
    }
}