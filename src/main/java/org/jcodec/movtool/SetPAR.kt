package org.jcodec.movtool

import org.jcodec.common.model.Rational
import org.jcodec.common.model.Size
import org.jcodec.containers.mp4.BoxUtil.containsBox
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.MovieBox
import org.jcodec.containers.mp4.boxes.MovieFragmentBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.containers.mp4.boxes.SampleDescriptionBox
import org.jcodec.containers.mp4.boxes.VideoSampleEntry
import java.io.File

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object SetPAR {
    @Throws(Exception::class)
    fun main1(args: Array<String?>) {
        if (args.size < 2) {
            println("Syntax: setpasp <movie> <num:den>")
            System.exit(-1)
        }
        val newPAR = Rational.parse(args[1])
        InplaceMP4Editor().modify(File(args[0]), object : MP4Edit {
            override fun apply(mov: MovieBox) {
                val vt = mov.videoTrack
                vt!!.pAR = newPAR
                val box = (findFirstPath(vt, path("mdia.minf.stbl.stsd")) as SampleDescriptionBox?)!!.getBoxes()[0]
                if (box != null && box is VideoSampleEntry) {
                    val vs = box
                    val codedWidth = vs.width.toInt()
                    val codedHeight = vs.height.toInt()
                    val displayWidth = codedWidth * newPAR.getNum() / newPAR.getDen()
                    vt.trackHeader.setWidth(displayWidth.toFloat())
                    if (containsBox(vt, "tapt")) {
                        vt.setAperture(Size(codedWidth, codedHeight), Size(displayWidth, codedHeight))
                    }
                }
            }

            override fun applyToFragment(mov: MovieBox?, fragmentBox: Array<MovieFragmentBox?>?) {
                throw RuntimeException("Unsupported")
            }
        })
    }
}