package org.jcodec.common.io

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Bitstream writer
 *
 * @author The JCodec project
 */
class BitWriter(val buffer: ByteBuffer) {
    private var curInt = 0
    private var _curBit = 0
    private var initPos: Int
    fun fork(): BitWriter {
        val fork = BitWriter(buffer.duplicate())
        fork._curBit = _curBit
        fork.curInt = curInt
        fork.initPos = initPos
        return fork
    }

    fun flush() {
        val toWrite = _curBit + 7 shr 3
        for (i in 0 until toWrite) {
            buffer.put((curInt ushr 24).toByte())
            curInt = curInt shl 8
        }
    }

    private fun putInt(i: Int) {
        buffer.put((i ushr 24).toByte())
        buffer.put((i shr 16).toByte())
        buffer.put((i shr 8).toByte())
        buffer.put(i.toByte())
    }

    fun writeNBit(value: Int, n: Int) {
        var value = value
        require(n <= 32) { "Max 32 bit to write" }
        if (n == 0) return
        value = value and (-1 ushr 32 - n)
        if (32 - _curBit >= n) {
            curInt = curInt or (value shl 32 - _curBit - n)
            _curBit += n
            if (_curBit == 32) {
                putInt(curInt)
                _curBit = 0
                curInt = 0
            }
        } else {
            val secPart = n - (32 - _curBit)
            curInt = curInt or (value ushr secPart)
            putInt(curInt)
            curInt = value shl 32 - secPart
            _curBit = secPart
        }
    }

    fun write1Bit(bit: Int) {
        curInt = curInt or (bit shl 32 - _curBit - 1)
        ++_curBit
        if (_curBit == 32) {
            putInt(curInt)
            _curBit = 0
            curInt = 0
        }
    }

    fun curBit(): Int {
        return _curBit and 0x7
    }

    fun position(): Int {
        return (buffer.position() - initPos shl 3) + _curBit
    }

    init {
        initPos = buffer.position()
    }
}