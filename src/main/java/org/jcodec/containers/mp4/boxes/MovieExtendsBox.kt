package org.jcodec.containers.mp4.boxes

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class MovieExtendsBox(atom: Header) : NodeBox(atom) {
    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "mvex"
        }

        fun createMovieExtendsBox(): MovieExtendsBox {
            return MovieExtendsBox(Header(fourcc()))
        }
    }
}