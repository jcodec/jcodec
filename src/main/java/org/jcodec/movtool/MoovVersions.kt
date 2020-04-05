package org.jcodec.movtool

import org.jcodec.common.Ints
import org.jcodec.common.io.IOUtils
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.BoxFactory.Companion.default
import org.jcodec.containers.mp4.BoxUtil.containsBox
import org.jcodec.containers.mp4.BoxUtil.parseBox
import org.jcodec.containers.mp4.MP4Util.Atom
import org.jcodec.containers.mp4.MP4Util.findFirstAtomInFile
import org.jcodec.containers.mp4.MP4Util.getRootAtoms
import org.jcodec.containers.mp4.boxes.Header
import org.jcodec.containers.mp4.boxes.Header.Companion.createHeader
import org.jcodec.containers.mp4.boxes.MovieBox
import org.jcodec.containers.mp4.boxes.NodeBox
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.*

object MoovVersions {
    private val MOOV_FOURCC = byteArrayOf('m'.toByte(), 'o'.toByte(), 'o'.toByte(), 'v'.toByte())

    @kotlin.jvm.JvmStatic
    @Throws(IOException::class)
    fun listMoovVersionAtoms(file: File?): List<Atom> {
        val result = ArrayList<Atom>()
        var `is`: SeekableByteChannel? = null
        try {
            `is` = NIOUtils.readableChannel(file)
            val version = 0
            for (atom in getRootAtoms(`is`)) {
                if ("free" == atom.header.fourcc && isMoov(`is`, atom)) {
                    result.add(atom)
                }
                if ("moov" == atom.header.fourcc) {
                    result.add(atom)
                    break
                }
            }
        } finally {
            IOUtils.closeQuietly(`is`)
        }
        return result
    }

    /**
     * Appends moov to file and sets previous moov to 'free'
     */
    @kotlin.jvm.JvmStatic
    @Throws(IOException::class)
    fun addVersion(file: File?, moov: MovieBox) {
        val hdrsize = moov.header.size
        val estimate = moov.estimateSize().toLong()
        val size = Math.max(hdrsize, estimate) * 2
        val allocate = ByteBuffer.allocate(Ints.checkedCast(size))
        moov.write(allocate)
        allocate.flip()
        val oldmoov = findFirstAtomInFile("moov", file)
        var raf: RandomAccessFile? = null
        try {
            raf = RandomAccessFile(file, "rw")
            raf.seek(oldmoov!!.offset + 4)
            raf.write(Header.FOURCC_FREE)
            raf.seek(raf.length())
            raf.write(NIOUtils.toArray(allocate))
        } finally {
            IOUtils.closeQuietly(raf)
        }
    }

    /**
     * Reverts to previous moov version
     *
     * @throws NoSuchElementException if undo is not possible, e.g. only one version is available
     */
    @kotlin.jvm.JvmStatic
    @Throws(IOException::class)
    fun undo(file: File?) {
        val versions = listMoovVersionAtoms(file)
        if (versions.size < 2) {
            throw NoSuchElementException("Nowhere to rollback")
        }
        rollback(file, versions[versions.size - 2])
    }

    /**
     * Reverts to specific moov version. Use listMoovVersionAtoms for versions list.
     */
    @kotlin.jvm.JvmStatic
    @Throws(IOException::class)
    fun rollback(file: File?, version: Atom) {
        val oldmoov = findFirstAtomInFile("moov", file)
        require(oldmoov!!.offset != version.offset) { "Already at version you are trying to rollback to" }
        var raf: RandomAccessFile? = null
        try {
            raf = RandomAccessFile(file, "rw")
            raf.seek(version.offset + 4)
            raf.write(MOOV_FOURCC)
            raf.seek(oldmoov.offset + 4)
            raf.write(Header.FOURCC_FREE)
        } finally {
            IOUtils.closeQuietly(raf)
        }
    }

    @Throws(IOException::class)
    private fun isMoov(`is`: SeekableByteChannel?, atom: Atom): Boolean {
        val header = atom.header
        `is`!!.setPosition(atom.offset + header.headerSize())
        return try {
            val mov = parseBox(NIOUtils.fetchFromChannel(`is`, header.size.toInt()), createHeader("moov", header.size), default)
            mov is MovieBox && containsBox(mov as NodeBox, "mvhd")
        } catch (t: Throwable) {
            false
        }
    }
}