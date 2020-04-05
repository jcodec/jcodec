package org.jcodec.containers.mp4

import org.jcodec.common.AutoFileChannelWrapper
import org.jcodec.common.Codec
import org.jcodec.common.io.FileChannelWrapper
import org.jcodec.common.io.IOUtils
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.logging.Logger
import org.jcodec.containers.mp4.BoxFactory
import org.jcodec.containers.mp4.BoxUtil.parseBox
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.Header.Companion.createHeader
import org.jcodec.containers.mp4.boxes.Header.Companion.read
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object MP4Util {
    private val codecMapping: MutableMap<Codec, String> = HashMap()

    @JvmStatic
    @Throws(IOException::class)
    fun createRefMovie(input: SeekableByteChannel?, url: String?): MovieBox? {
        val movie = parseMovieChannel(input)
        val tracks = movie!!.tracks
        for (i in tracks.indices) {
            val trakBox = tracks[i]
            trakBox.setDataRef(url)
        }
        return movie
    }

    @JvmStatic
    @Throws(IOException::class)
    fun parseMovieChannel(input: SeekableByteChannel?): MovieBox? {
        for (atom in getRootAtoms(input)) {
            if ("moov" == atom.header.fourcc) {
                return atom.parseBox(input) as MovieBox
            }
        }
        return null
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createRefFullMovie(input: SeekableByteChannel?, url: String?): Movie? {
        val movie = parseFullMovieChannel(input)
        val tracks = movie!!.moov.tracks
        for (i in tracks.indices) {
            val trakBox = tracks[i]
            trakBox.setDataRef(url)
        }
        return movie
    }

    @JvmStatic
    @Throws(IOException::class)
    fun parseFullMovieChannel(input: SeekableByteChannel?): Movie? {
        var ftyp: FileTypeBox? = null
        for (atom in getRootAtoms(input)) {
            if ("ftyp" == atom.header.fourcc) {
                ftyp = atom.parseBox(input) as FileTypeBox
            } else if ("moov" == atom.header.fourcc) {
                return Movie(ftyp, atom.parseBox(input) as MovieBox)
            }
        }
        return null
    }

    @Throws(IOException::class)
    fun parseMovieFragments(input: SeekableByteChannel?): List<MovieFragmentBox> {
        var moov: MovieBox? = null
        val fragments = LinkedList<MovieFragmentBox>()
        for (atom in getRootAtoms(input)) {
            if ("moov" == atom.header.fourcc) {
                moov = atom.parseBox(input) as MovieBox
            } else if ("moof".equals(atom.header.fourcc, ignoreCase = true)) {
                fragments.add(atom.parseBox(input) as MovieFragmentBox)
            }
        }
        for (fragment in fragments) {
            fragment.movie = moov
        }
        return fragments
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getRootAtoms(input: SeekableByteChannel?): List<Atom> {
        input!!.setPosition(0)
        val result: MutableList<Atom> = ArrayList()
        var off: Long = 0
        var atom: Header?
        while (off < input.size()) {
            input.setPosition(off)
            atom = read(NIOUtils.fetchFromChannel(input, 16))
            if (atom == null) break
            result.add(Atom(atom, off))
            off += atom.size
        }
        return result
    }

    @JvmStatic
    @Throws(IOException::class)
    fun findFirstAtomInFile(fourcc: String, input: File?): Atom? {
        val c: SeekableByteChannel = AutoFileChannelWrapper(input)
        return try {
            findFirstAtom(fourcc, c)
        } finally {
            IOUtils.closeQuietly(c)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun findFirstAtom(fourcc: String, input: SeekableByteChannel?): Atom? {
        val rootAtoms = getRootAtoms(input)
        for (atom in rootAtoms) {
            if (fourcc == atom.header.fourcc) return atom
        }
        return null
    }

    @Throws(IOException::class)
    fun atom(input: SeekableByteChannel): Atom? {
        val off = input.position()
        val atom = read(NIOUtils.fetchFromChannel(input, 16))
        return atom?.let { Atom(it, off) }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun parseMovie(source: File?): MovieBox? {
        var input: SeekableByteChannel? = null
        return try {
            input = NIOUtils.readableChannel(source)
            parseMovieChannel(input)
        } finally {
            input?.close()
        }
    }

    @Throws(IOException::class)
    fun createRefMovieFromFile(source: File): MovieBox? {
        var input: SeekableByteChannel? = null
        return try {
            input = NIOUtils.readableChannel(source)
            createRefMovie(input, "file://" + source.canonicalPath)
        } finally {
            input?.close()
        }
    }

    @Throws(IOException::class)
    fun writeMovieToFile(f: File?, movie: MovieBox) {
        var out: SeekableByteChannel? = null
        try {
            out = NIOUtils.writableChannel(f)
            writeMovie(out, movie)
        } finally {
            IOUtils.closeQuietly(out)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeMovie(out: SeekableByteChannel?, movie: MovieBox) {
        doWriteMovieToChannel(out, movie, 0)
    }

    @Throws(IOException::class)
    fun doWriteMovieToChannel(out: SeekableByteChannel?, movie: MovieBox, additionalSize: Int) {
        val sizeHint = estimateMoovBoxSize(movie) + additionalSize
        Logger.debug("Using $sizeHint bytes for MOOV box")
        val buf = ByteBuffer.allocate(sizeHint * 4)
        movie.write(buf)
        buf.flip()
        out!!.write(buf)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun parseFullMovie(source: File?): Movie? {
        var input: SeekableByteChannel? = null
        return try {
            input = NIOUtils.readableChannel(source)
            parseFullMovieChannel(input)
        } finally {
            input?.close()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createRefFullMovieFromFile(source: File): Movie? {
        var input: SeekableByteChannel? = null
        return try {
            input = NIOUtils.readableChannel(source)
            createRefFullMovie(input, "file://" + source.canonicalPath)
        } finally {
            input?.close()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeFullMovieToFile(f: File?, movie: Movie) {
        var out: SeekableByteChannel? = null
        try {
            out = NIOUtils.writableChannel(f)
            writeFullMovie(out, movie)
        } finally {
            IOUtils.closeQuietly(out)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeFullMovie(out: SeekableByteChannel?, movie: Movie) {
        doWriteFullMovieToChannel(out, movie, 0)
    }

    @Throws(IOException::class)
    fun doWriteFullMovieToChannel(out: SeekableByteChannel?, movie: Movie, additionalSize: Int) {
        val sizeHint = estimateMoovBoxSize(movie.moov) + additionalSize
        Logger.debug("Using $sizeHint bytes for MOOV box")
        val buf = ByteBuffer.allocate(sizeHint + 128)
        movie.ftyp!!.write(buf)
        movie.moov.write(buf)
        buf.flip()
        out!!.write(buf)
    }

    /**
     * Estimate buffer size needed to write MOOV box based on the amount of stuff in
     * there
     *
     * @param movie
     * @return
     */
    fun estimateMoovBoxSize(movie: MovieBox): Int {
        return movie.estimateSize() + (4 shl 10)
    }

    fun getFourcc(codec: Codec): String? {
        return codecMapping[codec]
    }

    @JvmStatic
    fun writeBox(box: Box, approxSize: Int): ByteBuffer {
        val buf = ByteBuffer.allocate(approxSize)
        box.write(buf)
        buf.flip()
        return buf
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeMdat(out: FileChannelWrapper, mdatPos: Long, mdatSize: Long) {
        out.setPosition(mdatPos)
        var mdat = createHeader("mdat", mdatSize + 16)
        if (mdat.headerSize() != 16L) {
            mdat = createHeader("mdat", mdatSize + 8)
            createHeader("free", 8).writeChannel(out)
        }
        mdat.writeChannel(out)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun mdatPlaceholder(out: FileChannelWrapper): Long {
        val mdatPos = out.position()
        out.write(ByteBuffer.wrap(ByteArray(16)))
        return mdatPos
    }

    fun traceBox(box: Box) {
        traceBoxR(box, 0)
    }

    fun traceBoxR(b: Box, level: Int) {
        if (b is NodeBox) {
            for (box2 in b.getBoxes()) {
                traceBoxR(box2, level + 1)
            }
        } else {
            val bld = StringBuilder()
            for (i in 0 until level) bld.append(' ')
            println(bld.toString() + b.header.fourcc)
        }
    }

    fun getMdat(rootAtoms: List<Atom>): Atom? {
        for (atom in rootAtoms) {
            if ("mdat" == atom.header.fourcc) {
                return atom
            }
        }
        return null
    }

    fun getMoov(rootAtoms: List<Atom>): Atom? {
        for (atom in rootAtoms) {
            if ("moov" == atom.header.fourcc) {
                return atom
            }
        }
        return null
    }

    class Movie(val ftyp: FileTypeBox?, val moov: MovieBox)

    class Atom(val header: Header, val offset: Long) {

        @Throws(IOException::class)
        fun parseBox(input: SeekableByteChannel?): Box {
            input!!.setPosition(offset + header.headerSize())
            return parseBox(NIOUtils.fetchFromChannel(input, header.bodySize.toInt()), header,
                    BoxFactory.default)
        }

        @Throws(IOException::class)
        fun copyContents(input: SeekableByteChannel, out: WritableByteChannel?) {
            input.setPosition(offset + header.headerSize())
            NIOUtils.copy(input, out, header.bodySize)
        }

        @Throws(IOException::class)
        fun copy(input: SeekableByteChannel, out: WritableByteChannel?) {
            input.setPosition(offset)
            NIOUtils.copy(input, out, header.size)
        }

    }

    init {
        codecMapping[Codec.MPEG2] = "m2v1"
        codecMapping[Codec.H264] = "avc1"
        codecMapping[Codec.H265] = "hev1"
        codecMapping[Codec.J2K] = "mjp2"
    }
}