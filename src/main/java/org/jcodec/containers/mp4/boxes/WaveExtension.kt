package org.jcodec.containers.mp4.boxes

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Wave extension to audio sample entry
 *
 * @author The JCodec project
 */
class WaveExtension(atom: Header) : NodeBox(atom) {
    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "wave"
        }
    }
}