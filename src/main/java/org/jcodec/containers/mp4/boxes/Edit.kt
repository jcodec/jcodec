package org.jcodec.containers.mp4.boxes

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class Edit(var duration: Long, var mediaTime: Long, val rate: Float) {

    fun shift(shift: Long) {
        mediaTime += shift
    }

    fun stretch(l: Long) {
        duration += l
    }

    companion object {
        @JvmStatic
        fun createEdit(edit: Edit): Edit {
            return Edit(edit.duration, edit.mediaTime, edit.rate)
        }
    }

}