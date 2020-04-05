package org.jcodec.movtool

import org.jcodec.common.JCodecUtil2
import org.jcodec.common.StringUtils
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.BoxFactory.Companion.default
import org.jcodec.containers.mp4.MP4Util.Movie
import org.jcodec.containers.mp4.MP4Util.createRefFullMovie
import org.jcodec.containers.mp4.MP4Util.writeFullMovie
import org.jcodec.containers.mp4.boxes.Edit
import org.jcodec.containers.mp4.boxes.MovieBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.cloneBox
import org.jcodec.containers.mp4.boxes.TrakBox
import java.io.File
import java.io.IOException
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Cut on ref movies
 *
 * @author The JCodec project
 */
class Cut {
    class Slice(val inSec: Double, val outSec: Double)

    fun cut(movie: Movie?, commands: List<Slice>): List<Movie> {
        val moov = movie!!.moov
        val videoTrack = moov.videoTrack
        if (videoTrack != null && videoTrack.timescale != moov.timescale) moov.fixTimescale(videoTrack.timescale)
        val tracks = moov.tracks
        for (i in tracks.indices) {
            val trakBox = tracks[i]
            Util.forceEditList(moov, trakBox)
            val edits = trakBox.edits
            for (cut in commands) {
                split(edits, cut.inSec, moov, trakBox)
                split(edits, cut.outSec, moov, trakBox)
            }
        }
        val result = ArrayList<Movie>()
        for (cut in commands) {
            val clone = cloneBox(moov, 16 * 1024 * 1024, default) as MovieBox?
            for (trakBox in clone!!.tracks) {
                selectInner(trakBox.edits, cut, moov, trakBox)
            }
            result.add(Movie(movie.ftyp, clone))
        }
        var movDuration: Long = 0
        for (trakBox in moov.tracks) {
            selectOuter(trakBox.edits, commands, moov, trakBox)
            trakBox.edits = trakBox.edits
            movDuration = Math.max(movDuration, trakBox.duration)
        }
        moov.duration = movDuration
        return result
    }

    private fun selectOuter(edits: MutableList<Edit>?, commands: List<Slice>, movie: MovieBox, trakBox: TrakBox) {
        val inMv = LongArray(commands.size)
        val outMv = LongArray(commands.size)
        for (i in commands.indices) {
            inMv[i] = (commands[i].inSec * movie.timescale).toLong()
            outMv[i] = (commands[i].outSec * movie.timescale).toLong()
        }
        var editStartMv: Long = 0
        val lit = edits!!.listIterator()
        while (lit.hasNext()) {
            val edit = lit.next()
            for (i in inMv.indices) {
                if (editStartMv + edit.duration > inMv[i] && editStartMv < outMv[i]) lit.remove()
            }
            editStartMv += edit.duration
        }
    }

    private fun selectInner(edits: MutableList<Edit>?, cut: Slice, movie: MovieBox, trakBox: TrakBox) {
        val inMv = (movie.timescale * cut.inSec).toLong()
        val outMv = (movie.timescale * cut.outSec).toLong()
        var editStart: Long = 0
        val lit = edits!!.listIterator()
        while (lit.hasNext()) {
            val edit = lit.next()
            if (editStart + edit.duration <= inMv || editStart >= outMv) lit.remove()
            editStart += edit.duration
        }
    }

    private fun split(edits: List<Edit>?, sec: Double, movie: MovieBox, trakBox: TrakBox) {
        Util.split(movie, trakBox, (sec * movie.timescale).toLong())
    }

    companion object {
        @Throws(Exception::class)
        fun main1(args: Array<String>) {
            if (args.size < 1) {
                println("""Syntax: cut [-command arg]...[-command arg] [-self] <movie file>
	Creates a reference movie out of the file and applies a set of changes specified by the commands to it.""")
                System.exit(-1)
            }
            val slices: MutableList<Slice> = ArrayList()
            val sliceNames: MutableList<String?> = ArrayList()
            var selfContained = false
            var shift = 0
            while (true) {
                if ("-cut" == args[shift]) {
                    val pt = StringUtils.splitS(args[shift + 1], ":")
                    slices.add(Slice(pt[0].toInt().toDouble(), pt[1].toInt().toDouble()))
                    if (pt.size > 2) sliceNames.add(pt[2]) else sliceNames.add(null)
                    shift += 2
                } else if ("-self" == args[shift]) {
                    ++shift
                    selfContained = true
                } else break
            }
            val source = File(args[shift])
            var input: SeekableByteChannel? = null
            var out: SeekableByteChannel? = null
            val outs: List<SeekableByteChannel> = ArrayList()
            try {
                input = NIOUtils.readableChannel(source)
                val movie = createRefFullMovie(input, "file://" + source.canonicalPath)
                val slicesMovs: List<Movie>
                if (!selfContained) {
                    out = NIOUtils.writableChannel(File(source.parentFile, JCodecUtil2.removeExtension(source.name)
                            + ".ref.mov"))
                    slicesMovs = Cut().cut(movie, slices)
                    writeFullMovie(out, movie!!)
                } else {
                    out = NIOUtils.writableChannel(File(source.parentFile, JCodecUtil2.removeExtension(source.name)
                            + ".self.mov"))
                    slicesMovs = Cut().cut(movie, slices)
                    Strip().strip(movie!!.moov)
                    Flatten().flattenChannel(movie, out)
                }
                saveSlices(slicesMovs, sliceNames, source.parentFile)
            } finally {
                input?.close()
                out?.close()
                for (o in outs) {
                    o.close()
                }
            }
        }

        @Throws(IOException::class)
        private fun saveSlices(slices: List<Movie>, names: List<String?>, parentFile: File) {
            for (i in slices.indices) {
                if (names[i] == null) continue
                var out: SeekableByteChannel? = null
                try {
                    out = NIOUtils.writableChannel(File(parentFile, names[i]))
                    writeFullMovie(out, slices[i])
                } finally {
                    NIOUtils.closeQuietly(out)
                }
            }
        }
    }
}