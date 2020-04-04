package org.jcodec.containers.mp4.boxes

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class ProductionApertureBox(atom: Header) : ClearApertureBox(atom) {
    companion object {
        const val PROF = "prof"
        @JvmStatic
        fun createProductionApertureBox(width: Int, height: Int): ProductionApertureBox {
            val prof = ProductionApertureBox(Header(PROF))
            prof.width = width.toFloat()
            prof.height = height.toFloat()
            return prof
        }
    }
}