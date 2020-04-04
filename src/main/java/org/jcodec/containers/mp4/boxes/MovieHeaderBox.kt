package org.jcodec.containers.mp4.boxes

import org.jcodec.common.io.NIOUtils
import org.jcodec.containers.mp4.TimeUtil
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A movie header box
 *
 * @author The JCodec project
 */
class MovieHeaderBox(header: Header) : FullBox(header) {
    private var timescale = 0
    private var duration: Long = 0
    var rate = 0f
        private set
    var volume = 0f
        private set
    var created: Long = 0
        private set
    var modified: Long = 0
        private set
    var matrix: IntArray = IntArray(0)
        private set
    private var nextTrackId = 0
    fun getTimescale(): Int {
        return timescale
    }

    fun getDuration(): Long {
        return duration
    }

    fun getNextTrackId(): Int {
        return nextTrackId
    }

    fun setTimescale(newTs: Int) {
        timescale = newTs
    }

    fun setDuration(duration: Long) {
        this.duration = duration
    }

    fun setNextTrackId(nextTrackId: Int) {
        this.nextTrackId = nextTrackId
    }

    private fun readMatrix(input: ByteBuffer): IntArray {
        val matrix = IntArray(9)
        for (i in 0..8) matrix[i] = input.int
        return matrix
    }

    private fun readVolume(input: ByteBuffer): Float {
        return input.short.toFloat() / 256f
    }

    private fun readRate(input: ByteBuffer): Float {
        return input.int.toFloat() / 65536f
    }

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        if (version.toInt() == 0) {
            created = TimeUtil.fromMovTime(input.int)
            modified = TimeUtil.fromMovTime(input.int)
            timescale = input.int
            duration = input.int.toLong()
        } else if (version.toInt() == 1) {
            created = TimeUtil.fromMovTime(input.long.toInt())
            modified = TimeUtil.fromMovTime(input.long.toInt())
            timescale = input.int
            duration = input.long
        } else {
            throw RuntimeException("Unsupported version")
        }
        rate = readRate(input)
        volume = readVolume(input)
        NIOUtils.skip(input, 10)
        matrix = readMatrix(input)
        NIOUtils.skip(input, 24)
        nextTrackId = input.int
    }

    public override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(TimeUtil.toMovTime(created))
        out.putInt(TimeUtil.toMovTime(modified))
        out.putInt(timescale)
        out.putInt(duration.toInt())
        writeFixed1616(out, rate)
        writeFixed88(out, volume)
        out.put(ByteArray(10))
        writeMatrix(out)
        out.put(ByteArray(24))
        out.putInt(nextTrackId)
    }

    override fun estimateSize(): Int {
        return 144
    }

    private fun writeMatrix(out: ByteBuffer) {
        for (i in 0 until Math.min(9, matrix.size)) out.putInt(matrix[i])
        for (i in Math.min(9, matrix.size)..8) out.putInt(0)
    }

    private fun writeFixed88(out: ByteBuffer, volume: Float) {
        out.putShort((volume * 256.0).toShort())
    }

    private fun writeFixed1616(out: ByteBuffer, rate: Float) {
        out.putInt((rate * 65536.0).toInt())
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "mvhd"
        }

        @JvmStatic
        fun createMovieHeaderBox(timescale: Int, duration: Long, rate: Float, volume: Float,
                                 created: Long, modified: Long, matrix: IntArray, nextTrackId: Int): MovieHeaderBox {
            val mvhd = MovieHeaderBox(Header(fourcc()))
            mvhd.timescale = timescale
            mvhd.duration = duration
            mvhd.rate = rate
            mvhd.volume = volume
            mvhd.created = created
            mvhd.modified = modified
            mvhd.matrix = matrix
            mvhd.nextTrackId = nextTrackId
            return mvhd
        }
    }
}