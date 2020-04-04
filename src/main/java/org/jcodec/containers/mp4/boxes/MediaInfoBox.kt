package org.jcodec.containers.mp4.boxes

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Creates MP4 file out of a set of samples
 *
 * @author The JCodec project
 */
class MediaInfoBox(atom: Header) : NodeBox(atom) {
    val dinf: DataInfoBox
        get() = findFirst(this, "dinf") as DataInfoBox

    val stbl: NodeBox
        get() = findFirst(this, "stbl") as NodeBox

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "minf"
        }

        @JvmStatic
        fun createMediaInfoBox(): MediaInfoBox {
            return MediaInfoBox(Header(fourcc()))
        }
    }
}