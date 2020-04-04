package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class GenericMediaInfoBox(atom: Header) : FullBox(atom) {
    private var graphicsMode: Short = 0
    private var rOpColor: Short = 0
    private var gOpColor: Short = 0
    private var bOpColor: Short = 0
    private var balance: Short = 0
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        graphicsMode = input.short
        rOpColor = input.short
        gOpColor = input.short
        bOpColor = input.short
        balance = input.short
        input.short
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putShort(graphicsMode)
        out.putShort(rOpColor)
        out.putShort(gOpColor)
        out.putShort(bOpColor)
        out.putShort(balance)
        out.putShort(0.toShort())
    }

    override fun estimateSize(): Int {
        return 24
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "gmin"
        }

        @JvmStatic
        fun createGenericMediaInfoBox(): GenericMediaInfoBox {
            return GenericMediaInfoBox(Header(fourcc()))
        }
    }
}