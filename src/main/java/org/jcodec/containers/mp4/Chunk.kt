package org.jcodec.containers.mp4

import org.jcodec.common.io.NIOUtils
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class Chunk(var offset: Long, var startTv: Long, var sampleCount: Int, val sampleSize: Int, var sampleSizes: IntArray, var sampleDur: Int,
            var sampleDurs: IntArray?, val entry: Int) {
    var data: ByteBuffer? = null

    val duration: Int
        get() {
            if (sampleDur != UNEQUAL_DUR) return sampleDur * sampleCount
            var sum = 0
            for (j in sampleDurs!!.indices) {
                val i = sampleDurs!![j]
                sum += i
            }
            return sum
        }

    val size: Long
        get() {
            if (sampleSize != UNEQUAL_SIZES) return (sampleSize * sampleCount).toLong()
            var sum: Long = 0
            for (j in sampleSizes.indices) {
                val i = sampleSizes[j]
                sum += i.toLong()
            }
            return sum
        }

    fun dropFrontSamples(drop: Int) {
        if (sampleSize == UNEQUAL_SIZES) {
            for (i in 0 until drop) {
                offset += sampleSizes[i]
                if (data != null) NIOUtils.skip(data, sampleSizes[i])
            }
            sampleSizes = Arrays.copyOfRange(sampleSizes, drop, sampleSizes.size)
        } else {
            offset += sampleSize * drop.toLong()
            NIOUtils.skip(data, sampleSize * drop)
        }
        if (sampleDur == UNEQUAL_DUR) {
            sampleDurs = Arrays.copyOfRange(sampleDurs, drop, sampleDurs!!.size)
        }
        sampleCount -= drop
    }

    fun dropTailSamples(drop: Int) {
        if (sampleSize == UNEQUAL_SIZES) {
            sampleSizes = Arrays.copyOf(sampleSizes, sampleSizes.size - drop)
        }
        if (sampleDur == UNEQUAL_DUR) {
            sampleDurs = Arrays.copyOf(sampleDurs, sampleDurs!!.size - drop)
        }
        sampleCount -= drop
    }

    fun trimFront(cutDur: Long) {
        var cutDur = cutDur
        startTv += cutDur
        if (sampleCount > 1) {
            var drop = 0
            for (s in 0 until sampleCount) {
                val dur = (if (sampleDur == UNEQUAL_DUR) sampleDurs!![s] else sampleDur).toLong()
                if (dur > cutDur) break
                drop++
                cutDur -= dur
            }
            dropFrontSamples(drop)
        }
        if (sampleDur == UNEQUAL_DUR) sampleDurs!![0] -= cutDur.toInt() else if (sampleCount == 1) sampleDur -= cutDur.toInt()
    }

    fun trimTail(cutDur: Long) {
        var cutDur = cutDur
        if (sampleCount > 1) {
            var drop = 0
            for (s in 0 until sampleCount) {
                val dur = (if (sampleDur == UNEQUAL_DUR) sampleDurs!![sampleCount - s - 1] else sampleDur).toLong()
                if (dur > cutDur) break
                drop++
                cutDur -= dur
            }
            dropTailSamples(drop)
        }
        if (sampleDur == UNEQUAL_DUR) sampleDurs!![sampleDurs!!.size - 1] -= cutDur.toInt() else if (sampleCount == 1) sampleDur -= cutDur.toInt()
    }

    companion object {
        const val UNEQUAL_DUR = -1
        const val UNEQUAL_SIZES = -1

        @JvmStatic
        fun createFrom(other: Chunk): Chunk {
            return Chunk(other.offset, other.startTv, other.sampleCount, other.sampleSize,
                    other.sampleSizes, other.sampleDur, other.sampleDurs, other.entry)
        }
    }

}