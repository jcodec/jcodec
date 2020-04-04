package org.jcodec.containers.mp4.boxes

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class EncodedPixelBox(atom: Header) : ClearApertureBox(atom) {
    companion object {
        const val ENOF = "enof"
        @JvmStatic
        fun createEncodedPixelBox(width: Int, height: Int): EncodedPixelBox {
            val enof = EncodedPixelBox(Header(ENOF))
            enof.width = width.toFloat()
            enof.height = height.toFloat()
            return enof
        }
    }
}