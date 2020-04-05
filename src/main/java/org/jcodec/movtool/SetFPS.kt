package org.jcodec.movtool

import org.jcodec.common.logging.Logger
import org.jcodec.common.model.RationalLarge
import org.jcodec.common.tools.MainUtils
import org.jcodec.containers.mp4.boxes.MovieBox
import org.jcodec.containers.mp4.boxes.MovieFragmentBox
import java.io.File

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 *
 * Changes FPS on an MP4 file.
 *
 * @author Stan Vitvitskyy
 */
object SetFPS {
    private const val MIN_TIMESCALE_ALLOWED = 25

    @Throws(Exception::class)
    fun main1(args: Array<String?>?) {
        val cmd = MainUtils.parseArguments(args, arrayOf())
        if (cmd.argsLength() < 2) {
            MainUtils.printHelpNoFlags("movie", "num:den")
            System.exit(-1)
        }
        val newFPS = RationalLarge.parse(cmd.getArg(1))
        InplaceMP4Editor().modify(File(cmd.getArg(0)), SetFPSEdit(newFPS))
    }

    class SetFPSEdit(private val newFPS: RationalLarge) : MP4Edit {
        override fun apply(mov: MovieBox) {
            val vt = mov.videoTrack
            val stts = vt!!.stts
            val entries = stts.getEntries()
            var nSamples: Long = 0
            var totalDuration: Long = 0
            for (e in entries) {
                nSamples += e!!.sampleCount.toLong()
                totalDuration += e.sampleCount * e.sampleDuration.toLong()
            }
            val newTimescale = newFPS.multiply(RationalLarge(totalDuration, nSamples)).scalarClip().toInt()
            if (newTimescale >= MIN_TIMESCALE_ALLOWED) {
                // Playing with timescale if possible
                vt.timescale = newTimescale
                val newDuration = totalDuration * mov.timescale / vt.timescale
                mov.duration = newDuration
                vt.duration = newDuration
            } else {
                // Playing with actual sample durations
                val mul = RationalLarge(vt.timescale * totalDuration, nSamples).divideBy(newFPS)
                        .scalar()
                Logger.info("Applying multiplier to sample durations: $mul")
                for (e in entries) {
                    e!!.sampleDuration = (e.sampleDuration * mul * 100).toInt()
                }
                vt.timescale = vt.timescale * 100
            }
            if (newTimescale != vt.timescale) {
                Logger.info("Changing timescale to: " + vt.timescale)
                val newDuration = totalDuration * mov.timescale / vt.timescale
                mov.duration = newDuration
                vt.duration = newDuration
            }
        }

        override fun applyToFragment(mov: MovieBox?, fragmentBox: Array<MovieFragmentBox?>?) {
            throw RuntimeException("Unsupported")
        }

    }
}