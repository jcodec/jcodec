package org.jcodec.movtool

import org.jcodec.containers.mp4.MP4Util.createRefFullMovieFromFile
import java.io.File
import java.io.IOException

/**
 * A full fledged MP4 editor.
 *
 * Parses MP4 file, applies the edit and saves the result in a new file.
 *
 * Unlike InplaceMP4Edit any changes are allowed. This class will take care of
 * adjusting all the sample offsets so the result file will be correct.
 *
 * @author The JCodec project
 */
class ReplaceMP4Editor {
    @Throws(IOException::class)
    fun modifyOrReplace(src: File, edit: MP4Edit) {
        val modify = InplaceMP4Editor().modify(src, edit)
        if (!modify) replace(src, edit)
    }

    @Throws(IOException::class)
    fun replace(src: File, edit: MP4Edit) {
        val tmp = File(src.parentFile, "." + src.name)
        copy(src, tmp, edit)
        tmp.renameTo(src)
    }

    @Throws(IOException::class)
    fun copy(src: File?, dst: File?, edit: MP4Edit) {
        val movie = createRefFullMovieFromFile(src!!)
        edit.apply(movie!!.moov)
        val fl = Flatten()
        fl.flatten(movie, dst)
    }
}