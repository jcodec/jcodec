package org.jcodec.codecs.mpeg4.es

import org.jcodec.common.JCodecUtil2
import org.jcodec.common.Preconditions
import org.jcodec.common.UsedViaReflection
import org.jcodec.common.and
import org.jcodec.common.io.NIOUtils
import java.nio.ByteBuffer
import java.util.*

object DescriptorParser {
    private const val ES_TAG = 0x03
    private const val DC_TAG = 0x04
    private const val DS_TAG = 0x05
    private const val SL_TAG = 0x06
    fun read(input: ByteBuffer): Descriptor? {
        if (input.remaining() < 2) return null
        val tag: Int = input.get() and 0xff
        val size = JCodecUtil2.readBER32(input)
        val byteBuffer = NIOUtils.read(input, size)
        return when (tag) {
            ES_TAG -> parseES(byteBuffer)
            SL_TAG -> parseSL(byteBuffer)
            DC_TAG -> parseDecoderConfig(byteBuffer)
            DS_TAG -> parseDecoderSpecific(byteBuffer)
            else -> throw RuntimeException("unknown tag $tag")
        }
    }

    @UsedViaReflection
    private fun parseNodeDesc(input: ByteBuffer): NodeDescriptor {
        val children: MutableCollection<Descriptor> = ArrayList()
        var d: Descriptor?
        do {
            d = read(input)
            if (d != null) children.add(d)
        } while (d != null)
        return NodeDescriptor(0, children)
    }

    private fun parseES(input: ByteBuffer): ES {
        val trackId = input.short.toInt()
        input.get()
        val node = parseNodeDesc(input)
        return ES(trackId, node.children)
    }

    private fun parseSL(input: ByteBuffer): SL {
        Preconditions.checkState(0x2 == input.get() and 0xff)
        return SL()
    }

    private fun parseDecoderSpecific(input: ByteBuffer): DecoderSpecific {
        val data = NIOUtils.readBuf(input)
        return DecoderSpecific(data)
    }

    private fun parseDecoderConfig(input: ByteBuffer): DecoderConfig {
        val objectType: Int = input.get() and 0xff
        input.get()
        val bufSize: Int = input.get() and 0xff shl 16 or (input.short and 0xffff)
        val maxBitrate = input.int
        val avgBitrate = input.int
        val node = parseNodeDesc(input)
        return DecoderConfig(objectType, bufSize, maxBitrate, avgBitrate, node.children)
    }
}