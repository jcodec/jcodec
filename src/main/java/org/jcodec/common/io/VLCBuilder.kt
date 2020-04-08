package org.jcodec.common.io

import org.jcodec.common.IntArrayList
import org.jcodec.common.IntArrayList.Companion.createIntArrayList
import org.jcodec.common.IntIntMap

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * prefix VLC reader builder
 *
 * @author The JCodec project
 */
class VLCBuilder {
    private val forward: IntIntMap
    private val inverse: IntIntMap
    private val codes: IntArrayList
    private val codesSizes: IntArrayList
    operator fun set(`val`: Int, code: String): VLCBuilder {
        setInt(code.toInt(2), code.length, `val`)
        return this
    }

    fun setInt(code: Int, len: Int, `val`: Int): VLCBuilder {
        codes.add(code shl 32 - len)
        codesSizes.add(len)
        forward.put(`val`, codes.size() - 1)
        inverse.put(codes.size() - 1, `val`)
        return this
    }

    fun getVLC(): VLC = vlc

    val vlc: VLC
        get() {
            val self = this
            return object : VLC(codes.toArray(), codesSizes.toArray()) {
                override fun readVLC(_in: BitReader): Int {
                    return self.inverse[super.readVLC(_in)]
                }

                override fun readVLC16(_in: BitReader): Int {
                    return self.inverse[super.readVLC16(_in)]
                }

                override fun writeVLC(out: BitWriter, code: Int) {
                    super.writeVLC(out, self.forward[code])
                }
            }
        }

    companion object {
        @JvmStatic
        fun createVLCBuilder(codes: IntArray, lens: IntArray, vals: IntArray): VLCBuilder {
            val b = VLCBuilder()
            for (i in codes.indices) {
                b.setInt(codes[i], lens[i], vals[i])
            }
            return b
        }
    }

    init {
        forward = IntIntMap()
        inverse = IntIntMap()
        codes = createIntArrayList()
        codesSizes = createIntArrayList()
    }
}