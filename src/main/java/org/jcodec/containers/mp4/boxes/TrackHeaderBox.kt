package org.jcodec.containers.mp4.boxes

import org.jcodec.containers.mp4.TimeUtil
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class TrackHeaderBox(header: Header) : FullBox(header) {
    fun getNo(): Int = trackId
    var trackId = 0

    private var duration: Long = 0
    private var width = 0f
    private var height = 0f
    var created: Long = 0
        private set
    var modified: Long = 0
        private set
    var volume = 0f
        private set
    var layer: Short = 0
        private set
    var altGroup: Long = 0
        private set
    var matrix: IntArray? = null
        private set

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        if (version.toInt() == 0) {
            created = TimeUtil.fromMovTime(input.int) // Creation time
            modified = TimeUtil.fromMovTime(input.int) // Modification time
        } else {
            created = TimeUtil.fromMovTime(input.long.toInt())
            modified = TimeUtil.fromMovTime(input.long.toInt())
        }
        trackId = input.int
        input.int
        duration = if (version.toInt() == 0) {
            input.int.toLong()
        } else {
            input.long
        }
        input.int // Reserved
        input.int
        layer = input.short
        altGroup = input.short.toLong()
        volume = readVolume(input)
        input.short
        readMatrix(input)
        width = input.int / 65536f
        height = input.int / 65536f
    }

    private fun readMatrix(input: ByteBuffer) {
        matrix = IntArray(9)
        for (i in 0..8) matrix!![i] = input.int
    }

    private fun readVolume(input: ByteBuffer): Float {
        return (input.short / 256.0).toFloat()
    }

    fun getDuration(): Long {
        return duration
    }

    fun getWidth(): Float {
        return width
    }

    fun getHeight(): Float {
        return height
    }

    public override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(TimeUtil.toMovTime(created))
        out.putInt(TimeUtil.toMovTime(modified))
        out.putInt(trackId)
        out.putInt(0)
        out.putInt(duration.toInt())
        out.putInt(0)
        out.putInt(0)
        out.putShort(layer)
        out.putShort(altGroup.toShort())
        writeVolume(out)
        out.putShort(0.toShort())
        writeMatrix(out)
        out.putInt((width * 65536).toInt())
        out.putInt((height * 65536).toInt())
    }

    override fun estimateSize(): Int {
        return 92
    }

    private fun writeMatrix(out: ByteBuffer) {
        for (i in 0 until Math.min(9, matrix!!.size)) out.putInt(matrix!![i])
        for (i in Math.min(9, matrix!!.size)..8) out.putInt(0)
    }

    private fun writeVolume(out: ByteBuffer) {
        out.putShort((volume * 256.0).toShort())
    }

    fun setWidth(width: Float) {
        this.width = width
    }

    fun setHeight(height: Float) {
        this.height = height
    }

    fun setDuration(duration: Long) {
        this.duration = duration
    }

    val isOrientation0: Boolean
        get() = matrix != null && matrix!![0] == 65536 && matrix!![4] == 65536

    val isOrientation90: Boolean
        get() = matrix != null && matrix!![1] == 65536 && matrix!![3] == -65536

    val isOrientation180: Boolean
        get() = matrix != null && matrix!![0] == -65536 && matrix!![4] == -65536

    val isOrientation270: Boolean
        get() = matrix != null && matrix!![1] == -65536 && matrix!![3] == 65536

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "tkhd"
        }

        @JvmStatic
        fun createTrackHeaderBox(trackId: Int, duration: Long, width: Float, height: Float,
                                 created: Long, modified: Long, volume: Float, layer: Short, altGroup: Long, matrix: IntArray?): TrackHeaderBox {
            val box = TrackHeaderBox(Header(fourcc()))
            box.trackId = trackId
            box.duration = duration
            box.width = width
            box.height = height
            box.created = created
            box.modified = modified
            box.volume = volume
            box.layer = layer
            box.altGroup = altGroup
            box.matrix = matrix
            return box
        }
    }
}