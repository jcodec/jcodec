package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class VideoMediaHeaderBox(header: Header) : FullBox(header) {
    var graphicsMode = 0
    var rOpColor = 0
    var gOpColor = 0
    var bOpColor = 0
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        graphicsMode = input.short.toInt()
        rOpColor = input.short.toInt()
        gOpColor = input.short.toInt()
        bOpColor = input.short.toInt()
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putShort(graphicsMode.toShort())
        out.putShort(rOpColor.toShort())
        out.putShort(gOpColor.toShort())
        out.putShort(bOpColor.toShort())
    }

    override fun estimateSize(): Int {
        return 20
    }

    fun getrOpColor(): Int {
        return rOpColor
    }

    fun getgOpColor(): Int {
        return gOpColor
    }

    fun getbOpColor(): Int {
        return bOpColor
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "vmhd"
        }

        @JvmStatic
        fun createVideoMediaHeaderBox(graphicsMode: Int, rOpColor: Int, gOpColor: Int,
                                      bOpColor: Int): VideoMediaHeaderBox {
            val vmhd = VideoMediaHeaderBox(Header(fourcc()))
            vmhd.graphicsMode = graphicsMode
            vmhd.rOpColor = rOpColor
            vmhd.gOpColor = gOpColor
            vmhd.bOpColor = bOpColor
            return vmhd
        }
    }
}