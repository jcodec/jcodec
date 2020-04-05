package org.jcodec.movtool

import org.jcodec.containers.mp4.boxes.MovieBox
import org.jcodec.containers.mp4.boxes.MovieFragmentBox

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
interface MP4Edit {
    /**
     * Operation performed on a movie header and fragments
     *
     * @param mov
     */
    fun applyToFragment(mov: MovieBox?, fragmentBox: Array<MovieFragmentBox?>?)

    /**
     * Operation performed on a movie header
     *
     * @param mov
     */
    fun apply(mov: MovieBox)
}