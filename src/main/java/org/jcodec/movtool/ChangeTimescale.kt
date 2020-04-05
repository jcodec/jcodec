package org.jcodec.movtool

import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.MediaHeaderBox
import org.jcodec.containers.mp4.boxes.MovieBox
import org.jcodec.containers.mp4.boxes.MovieFragmentBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import java.io.File

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object ChangeTimescale {
    @Throws(Exception::class)
    fun main1(args: Array<String>) {
        if (args.size < 2) {
            println("Syntax: chts <movie> <timescale>")
            System.exit(-1)
        }
        val ts = args[1].toInt()
        if (ts < 600) {
            println("Could not set timescale < 600")
            System.exit(-1)
        }
        InplaceMP4Editor().modify(File(args[0]), object : MP4Edit {
            override fun apply(mov: MovieBox) {
                val vt = mov.videoTrack
                val mdhd = findFirstPath(vt, path("mdia.mdhd")) as MediaHeaderBox?
                val oldTs = mdhd!!.getTimescale()
                if (oldTs > ts) {
                    throw RuntimeException("Old timescale (" + oldTs + ") is greater then new timescale (" + ts
                            + "), not touching.")
                }
                vt!!.fixMediaTimescale(ts)
                mov.fixTimescale(ts)
            }

            override fun applyToFragment(mov: MovieBox?, fragmentBox: Array<MovieFragmentBox?>?) {
                throw RuntimeException("Unsupported")
            }
        })
    }
}