package org.jcodec.containers.mp4.boxes

import org.jcodec.common.JCodecUtil2
import org.jcodec.common.io.NIOUtils
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A handler description box
 *
 * @author The JCodec project
 */
class HandlerBox(atom: Header) : FullBox(atom) {
    var componentType: String? = null
        private set
    var componentSubType: String? = null
        private set
    var componentManufacturer: String? = null
        private set
    var componentFlags = 0
        private set
    var componentFlagsMask = 0
        private set
    private var componentName: String? = null
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        componentType = NIOUtils.readString(input, 4)
        componentSubType = NIOUtils.readString(input, 4)
        componentManufacturer = NIOUtils.readString(input, 4)
        componentFlags = input.int
        componentFlagsMask = input.int
        componentName = NIOUtils.readString(input, input.remaining())
    }

    public override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.put(fourcc(componentType))
        out.put(fourcc(componentSubType))
        out.put(fourcc(componentManufacturer))
        out.putInt(componentFlags)
        out.putInt(componentFlagsMask)
        if (componentName != null) {
            out.put(fourcc(componentName))
        }
    }

    fun fourcc(fourcc: String?): ByteArray {
        val dst = ByteArray(4)
        if (fourcc != null) {
            val tmp = JCodecUtil2.asciiString(fourcc)
            for (i in 0 until Math.min(tmp.size, 4)) dst[i] = tmp[i]
        }
        return dst
    }

    override fun estimateSize(): Int {
        return 32 + if (componentName != null) 4 else 0
    }

    companion object {
        const val FOURCC = "hdlr"
        fun fourcc(): String {
            return FOURCC
        }

        @JvmStatic
        fun createHandlerBox(componentType: String?, componentSubType: String?,
                             componentManufacturer: String?, componentFlags: Int, componentFlagsMask: Int): HandlerBox {
            val hdlr = HandlerBox(Header(fourcc()))
            hdlr.componentType = componentType
            hdlr.componentSubType = componentSubType
            hdlr.componentManufacturer = componentManufacturer
            hdlr.componentFlags = componentFlags
            hdlr.componentFlagsMask = componentFlagsMask
            hdlr.componentName = ""
            return hdlr
        }
    }
}