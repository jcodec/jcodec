package org.jcodec.containers.mp4.boxes

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Creates MP4 file out of a set of samples
 *
 * @author The JCodec project
 */
class DataInfoBox(atom: Header) : NodeBox(atom) {
    val dref: DataRefBox
        get() = findFirst(this, "dref") as DataRefBox

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "dinf"
        }

        @JvmStatic
        fun createDataInfoBox(): DataInfoBox {
            return DataInfoBox(Header(fourcc()))
        }
    }
}