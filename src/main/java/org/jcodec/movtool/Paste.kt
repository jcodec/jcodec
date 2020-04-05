package org.jcodec.movtool

import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.MP4Util.createRefFullMovie
import org.jcodec.containers.mp4.MP4Util.writeFullMovie
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.Box.Companion.path
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirst
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.platform.Platform
import java.io.File
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Paste on ref movies
 *
 * @author The JCodec project
 */
class Paste {
    fun paste(to: MovieBox, from: MovieBox, sec: Double) {
        val videoTrack = to.videoTrack
        if (videoTrack != null && videoTrack.timescale != to.timescale) to.fixTimescale(videoTrack.timescale)
        val displayTv = (to.timescale * sec).toLong()
        Util.forceEditListMov(to)
        Util.forceEditListMov(from)
        val fromTracks = from.tracks
        val toTracks = to.tracks
        val matches = findMatches(fromTracks, toTracks)
        for (i in 0 until matches[0].size) {
            val localTrack = to.importTrack(from, fromTracks[i])
            if (matches[0][i] != -1) {
                Util.insertTo(to, toTracks[matches[0][i]], localTrack, displayTv)
            } else {
                to.appendTrack(localTrack)
                Util.shift(to, localTrack, displayTv)
            }
        }
        for (i in 0 until matches[1].size) {
            if (matches[1][i] == -1) {
                Util.spread(to, toTracks[i], displayTv, to.rescale(from.duration, from.timescale.toLong()))
            }
        }
        to.updateDuration()
    }

    fun addToMovie(to: MovieBox, from: MovieBox) {
        val tracks = from.tracks
        for (i in tracks.indices) {
            val track = tracks[i]
            to.appendTrack(to.importTrack(from, track))
        }
    }

    var tv: LongArray? = null
    private fun getFrameTv(videoTrack: TrakBox, frame: Int): Long {
        if (tv == null) {
            tv = Util.getTimevalues(videoTrack)
        }
        return tv!![frame]
    }

    private fun findMatches(fromTracks: Array<TrakBox>, toTracks: Array<TrakBox>): Array<IntArray> {
        val f2t = IntArray(fromTracks.size)
        val t2f = IntArray(toTracks.size)
        Arrays.fill(f2t, -1)
        Arrays.fill(t2f, -1)
        for (i in fromTracks.indices) {
            if (f2t[i] != -1) continue
            for (j in toTracks.indices) {
                if (t2f[j] != -1) continue
                if (matches(fromTracks[i], toTracks[j])) {
                    f2t[i] = j
                    t2f[j] = i
                    break
                }
            }
        }
        return arrayOf(f2t, t2f)
    }

    private fun matches(trakBox1: TrakBox, trakBox2: TrakBox): Boolean {
        return (trakBox1.handlerType == trakBox2.handlerType && matchHeaders(trakBox1, trakBox2)
                && matchSampleSizes(trakBox1, trakBox2) && matchMediaHeader(trakBox1, trakBox2)
                && matchClip(trakBox1, trakBox2) && matchLoad(trakBox1, trakBox2))
    }

    private fun matchSampleSizes(trakBox1: TrakBox, trakBox2: TrakBox): Boolean {
        val stsz1 = findFirstPath(trakBox1, path("mdia.minf.stbl.stsz")) as SampleSizesBox?
        val stsz2 = findFirstPath(trakBox1, path("mdia.minf.stbl.stsz")) as SampleSizesBox?
        return stsz1!!.defaultSize == stsz2!!.defaultSize
    }

    private fun matchMediaHeader(trakBox1: TrakBox, trakBox2: TrakBox): Boolean {
        val vmhd1 = findFirstPath(trakBox1, path("mdia.minf.vmhd")) as VideoMediaHeaderBox?
        val vmhd2 = findFirstPath(trakBox2, path("mdia.minf.vmhd")) as VideoMediaHeaderBox?
        if (vmhd1 != null && vmhd2 == null || vmhd1 == null && vmhd2 != null) return false else if (vmhd1 != null && vmhd2 != null) {
            return vmhd1.graphicsMode == vmhd2.graphicsMode && vmhd1.getbOpColor() == vmhd2.getbOpColor() && vmhd1.getgOpColor() == vmhd2.getgOpColor() && vmhd1.getrOpColor() == vmhd2.getrOpColor()
        } else {
            val smhd1 = findFirstPath(trakBox1, path("mdia.minf.smhd")) as SoundMediaHeaderBox?
            val smhd2 = findFirstPath(trakBox2, path("mdia.minf.smhd")) as SoundMediaHeaderBox?
            if (smhd1 == null && smhd2 != null || smhd1 != null && smhd2 == null) return false else if (smhd1 != null && smhd2 != null) return smhd1.balance == smhd1.balance
        }
        return true
    }

    private fun matchHeaders(trakBox1: TrakBox, trakBox2: TrakBox): Boolean {
        val th1 = trakBox1.trackHeader
        val th2 = trakBox2.trackHeader
        return (("vide" == trakBox1.handlerType && Platform.arrayEqualsInt(th1.matrix, th2.matrix)
                && th1.layer == th2.layer && th1.getWidth() == th2.getWidth() && th1.getHeight() == th2
                .getHeight())
                || "soun" == trakBox1.handlerType && th1.volume == th2.volume
                || "tmcd" == trakBox1.handlerType)
    }

    private fun matchLoad(trakBox1: TrakBox, trakBox2: TrakBox): Boolean {
        val load1 = findFirst(trakBox1, "load") as LoadSettingsBox?
        val load2 = findFirst(trakBox2, "load") as LoadSettingsBox?
        if (load1 != null && load2 != null) {
            return load1.preloadStartTime == load2.preloadStartTime && load1.preloadDuration == load2.preloadDuration && load1.preloadFlags == load2.preloadFlags && load1.defaultHints == load2.defaultHints
        }
        return if (load1 == null && load2 == null) true else false
    }

    private fun matchClip(trakBox1: TrakBox, trakBox2: TrakBox): Boolean {
        val crgn1 = findFirstPath(trakBox1, path("clip.crgn")) as ClipRegionBox?
        val crgn2 = findFirstPath(trakBox2, path("clip.crgn")) as ClipRegionBox?
        if (crgn1 != null && crgn2 != null) {
            return crgn1.rgnSize == crgn2.rgnSize && crgn1.x == crgn2.x && crgn1.y == crgn2.y && crgn1.width == crgn2.width && crgn1.height == crgn2.height
        }
        return if (crgn1 == null && crgn2 == null) true else false
    }

    companion object {
        @Throws(Exception::class)
        fun main1(args: Array<String>) {
            if (args.size < 2) {
                println("Syntax: paste <to movie> <from movie> [second]")
                System.exit(-1)
            }
            val toFile = File(args[0])
            var to: SeekableByteChannel? = null
            var from: SeekableByteChannel? = null
            var out: SeekableByteChannel? = null
            try {
                val outFile = File(toFile.parentFile, toFile.name.replace("\\.mov$".toRegex(), "") + ".paste.mov")
                Platform.deleteFile(outFile)
                out = NIOUtils.writableChannel(outFile)
                to = NIOUtils.writableChannel(toFile)
                val fromFile = File(args[1])
                from = NIOUtils.readableChannel(fromFile)
                val toMov = createRefFullMovie(to, "file://" + toFile.canonicalPath)
                val fromMov = createRefFullMovie(from, "file://" + fromFile.canonicalPath)
                Strip().strip(fromMov!!.moov)
                if (args.size > 2) {
                    Paste().paste(toMov!!.moov, fromMov.moov, args[2].toDouble())
                } else {
                    Paste().addToMovie(toMov!!.moov, fromMov.moov)
                }
                writeFullMovie(out, toMov)
            } finally {
                to?.close()
                from?.close()
                out?.close()
            }
        }
    }
}