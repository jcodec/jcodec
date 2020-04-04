package org.jcodec.containers.mp4.boxes

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class MediaBox(atom: Header) : NodeBox(atom) {
    val minf: MediaInfoBox
        get() = findFirst(this, "minf") as MediaInfoBox

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "mdia"
        }

        @JvmStatic
        fun createMediaBox(): MediaBox {
            return MediaBox(Header(fourcc()))
        }
    }
}