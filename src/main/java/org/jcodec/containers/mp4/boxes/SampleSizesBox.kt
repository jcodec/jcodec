package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author The JCodec project
 */
class SampleSizesBox(atom: Header) : FullBox(atom) {
    var defaultSize = 0
        private set
    private var count = 0
    private var sizes: IntArray = IntArray(0)
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        defaultSize = input.int
        count = input.int
        if (defaultSize == 0) {
            sizes = IntArray(count)
            for (i in 0 until count) {
                sizes[i] = input.int
            }
        }
    }

    fun getSizes(): IntArray {
        return sizes
    }

    fun getCount(): Int {
        return count
    }

    fun setCount(count: Int) {
        this.count = count
    }

    public override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(defaultSize)
        if (defaultSize == 0) {
            out.putInt(count)
            for (i in sizes.indices) {
                val size = sizes[i].toLong()
                out.putInt(size.toInt())
            }
        } else {
            out.putInt(count)
        }
    }

    override fun estimateSize(): Int {
        return (if (defaultSize == 0) sizes.size * 4 else 0) + 20
    }

    fun setSizes(sizes: IntArray) {
        this.sizes = sizes
        count = sizes.size
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "stsz"
        }

        @JvmStatic
        fun createSampleSizesBox(defaultSize: Int, count: Int): SampleSizesBox {
            val stsz = SampleSizesBox(Header(fourcc()))
            stsz.defaultSize = defaultSize
            stsz.count = count
            return stsz
        }

        @JvmStatic
        fun createSampleSizesBox2(sizes: IntArray): SampleSizesBox {
            val stsz = SampleSizesBox(Header(fourcc()))
            stsz.sizes = sizes
            stsz.count = sizes.size
            return stsz
        }
    }
}