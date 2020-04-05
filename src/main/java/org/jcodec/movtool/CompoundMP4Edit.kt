package org.jcodec.movtool

import org.jcodec.containers.mp4.boxes.MovieBox
import org.jcodec.containers.mp4.boxes.MovieFragmentBox

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Like MP4Edit
 *
 * @author The JCodec project
 */
class CompoundMP4Edit(private val edits: List<MP4Edit?>) : MP4Edit {
    override fun applyToFragment(mov: MovieBox?, fragmentBox: Array<MovieFragmentBox?>?) {
        for (command in edits) {
            command!!.applyToFragment(mov, fragmentBox)
        }
    }

    override fun apply(mov: MovieBox) {
        for (command in edits) {
            command!!.apply(mov)
        }
    }

}