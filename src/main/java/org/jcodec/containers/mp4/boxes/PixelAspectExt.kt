package org.jcodec.containers.mp4.boxes

import org.jcodec.common.model.Rational
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * Pixel aspect ratio video sample entry extension
 *
 * @author The JCodec project
 */
class PixelAspectExt(header: Header) : Box(header) {
    private var hSpacing = 0
    private var vSpacing = 0
    override fun parse(buf: ByteBuffer) {
        hSpacing = buf.int
        vSpacing = buf.int
    }

    override fun doWrite(out: ByteBuffer) {
        out.putInt(hSpacing)
        out.putInt(vSpacing)
    }

    override fun estimateSize(): Int {
        return 16
    }

    fun gethSpacing(): Int {
        return hSpacing
    }

    fun getvSpacing(): Int {
        return vSpacing
    }

    val rational: Rational
        get() = Rational(hSpacing, vSpacing)

    companion object {
        @JvmStatic
        fun createPixelAspectExt(par: Rational): PixelAspectExt {
            val pasp = PixelAspectExt(Header(fourcc()))
            pasp.hSpacing = par.getNum()
            pasp.vSpacing = par.getDen()
            return pasp
        }

        @JvmStatic
        fun fourcc(): String {
            return "pasp"
        }
    }
}