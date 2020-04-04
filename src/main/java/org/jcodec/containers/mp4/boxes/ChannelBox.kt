package org.jcodec.containers.mp4.boxes

import org.jcodec.common.model.Label
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class ChannelBox(atom: Header) : FullBox(atom) {
    var channelLayout = 0
    var channelBitmap = 0
        private set
    var descriptions: Array<ChannelDescription?> = emptyArray();

    class ChannelDescription(channelLabel: Int, channelFlags: Int, coordinates: FloatArray) {
        val channelLabel: Int
        val channelFlags: Int
        var coordinates: FloatArray= FloatArray(3)

        val label: Label
            get() = Label.getByVal(channelLabel)

        init {
            this.channelLabel = channelLabel
            this.channelFlags = channelFlags
            this.coordinates = coordinates
        }
    }

    override fun parse(input: ByteBuffer) {
        super.parse(input)
        channelLayout = input.int
        channelBitmap = input.int
        val numDescriptions = input.int
        descriptions = arrayOfNulls(numDescriptions)
        for (i in 0 until numDescriptions) {
            descriptions[i] = ChannelDescription(input.int, input.int, floatArrayOf(
                    java.lang.Float.intBitsToFloat(input.int), java.lang.Float.intBitsToFloat(input.int),
                    java.lang.Float.intBitsToFloat(input.int)))
        }
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putInt(channelLayout)
        out.putInt(channelBitmap)
        out.putInt(descriptions.size)
        for (i in descriptions.indices) {
            val channelDescription = descriptions[i]
            out.putInt(channelDescription!!.channelLabel)
            out.putInt(channelDescription.channelFlags)
            out.putFloat(channelDescription.coordinates[0])
            out.putFloat(channelDescription.coordinates[1])
            out.putFloat(channelDescription.coordinates[2])
        }
    }

    override fun estimateSize(): Int {
        return 12 + 12 + descriptions.size * 20
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "chan"
        }

        fun createChannelBox(): ChannelBox {
            return ChannelBox(Header(fourcc()))
        }
    }
}