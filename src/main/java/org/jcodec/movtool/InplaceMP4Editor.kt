package org.jcodec.movtool

import org.jcodec.common.Preconditions
import org.jcodec.common.Tuple
import org.jcodec.common.Tuple._2
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.BoxFactory.Companion.default
import org.jcodec.containers.mp4.BoxUtil.containsBox
import org.jcodec.containers.mp4.BoxUtil.parseBox
import org.jcodec.containers.mp4.MP4Util.Atom
import org.jcodec.containers.mp4.MP4Util.getRootAtoms
import org.jcodec.containers.mp4.boxes.Box
import org.jcodec.containers.mp4.boxes.Header.Companion.read
import org.jcodec.containers.mp4.boxes.MovieBox
import org.jcodec.containers.mp4.boxes.MovieFragmentBox
import java.io.File
import java.io.IOException
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Parses MP4 header and allows custom MP4Editor to modify it, then tries to put
 * the resulting header into the same place relatively to a file.
 *
 * This might not work out, for example if the resulting header is bigger then
 * the original.
 *
 * Use this class to make blazing fast changes to MP4 files when you know your
 * are not adding anything new to the header, perhaps only patching some values
 * or removing stuff from the header.
 *
 * @author The JCodec project
 */
class InplaceMP4Editor {
    /**
     * Tries to modify movie header in place according to what's implemented in
     * the edit, the file gets pysically modified if the operation is
     * successful. No temporary file is created.
     *
     * @param file
     * A file to be modified
     * @param edit
     * An edit to be carried out on a movie header
     * @return Whether or not edit was successful, i.e. was there enough place
     * to put the new header
     * @throws IOException
     * @throws Exception
     */
    @Throws(IOException::class)
    fun modify(file: File?, edit: MP4Edit): Boolean {
        var fi: SeekableByteChannel? = null
        return try {
            fi = NIOUtils.rwChannel(file)
            val fragments = doTheFix(fi, edit) ?: return false

            // If everything is clean, only then actually writing stuff to the
            // file
            for (fragment in fragments) {
                replaceBox(fi, fragment.v0, fragment.v1)
            }
            true
        } finally {
            NIOUtils.closeQuietly(fi)
        }
    }

    /**
     * Tries to modify movie header in place according to what's implemented in
     * the edit. Copies modified contents to a new file.
     *
     * Note: The header is still edited in-place, so the new file will have
     * all-the-same sample offsets.
     *
     * Note: Still subject to the same limitations as 'modify', i.e. the new
     * header must 'fit' into an old place.
     *
     * This method is useful when you can't write to the original file, for ex.
     * you don't have permission.
     *
     * @param src
     * An original file
     * @param dst
     * A file to store the modified copy
     * @param edit
     * An edit logic to apply
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun copy(src: File?, dst: File?, edit: MP4Edit): Boolean {
        var fi: SeekableByteChannel? = null
        var fo: SeekableByteChannel? = null
        return try {
            fi = NIOUtils.readableChannel(src)
            fo = NIOUtils.writableChannel(dst)
            val fragments = doTheFix(fi, edit) ?: return false
            val fragOffsets = Tuple._2map0(fragments, object : Tuple.Mapper<Atom?, Long?> {
                override fun map(t: Atom?): Long? {
                    return t!!.offset
                }
            })

            // If everything is clean, only then actually start writing file
            val rewrite = Tuple.asMap(fragOffsets)
            for (atom in getRootAtoms(fi)) {
                val byteBuffer = rewrite[atom.offset]
                if (byteBuffer != null) fo.write(byteBuffer) else atom.copy(fi, fo)
            }
            true
        } finally {
            NIOUtils.closeQuietly(fi)
            NIOUtils.closeQuietly(fo)
        }
    }

    /**
     * Tries to modify movie header in place according to what's implemented in
     * the edit. Copies modified contents to a new file with the same name
     * erasing the original file if successful.
     *
     * This is a shortcut for 'copy' when you want the new file to have the same
     * name but for some reason can not modify the original file in place. Maybe
     * modifications of files are expensive or not supported on your filesystem.
     *
     * @param src
     * A source and destination file
     * @param edit
     * An edit to be applied
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun replace(src: File, edit: MP4Edit): Boolean {
        val tmp = File(src.parentFile, "." + src.name)
        if (copy(src, tmp, edit)) {
            tmp.renameTo(src)
            return true
        }
        return false
    }

    @Throws(IOException::class)
    private fun doTheFix(fi: SeekableByteChannel?, edit: MP4Edit): List<_2<Atom?, ByteBuffer>>? {
        val moovAtom = getMoov(fi)
        Preconditions.checkNotNull(moovAtom)
        val moovBuffer = fetchBox(fi, moovAtom)
        val moovBox = parseBox(moovBuffer) as MovieBox
        val fragments: MutableList<_2<Atom?, ByteBuffer>> = LinkedList()
        if (containsBox(moovBox, "mvex")) {
            val temp: MutableList<_2<ByteBuffer, MovieFragmentBox>> = LinkedList()
            for (fragAtom in getFragments(fi)) {
                val fragBuffer = fetchBox(fi, fragAtom)
                fragments.add(Tuple.pair(fragAtom, fragBuffer))
                val fragBox = parseBox(fragBuffer) as MovieFragmentBox
                fragBox.movie = moovBox
                temp.add(Tuple.pair(fragBuffer, fragBox))
            }
            edit.applyToFragment(moovBox, Tuple._2_project1(temp).toTypedArray())
            for (frag in temp) {
                if (!rewriteBox(frag.v0, frag.v1)) return null
            }
        } else edit.apply(moovBox)
        if (!rewriteBox(moovBuffer, moovBox)) return null
        fragments.add(Tuple.pair(moovAtom, moovBuffer))
        return fragments
    }

    @Throws(IOException::class)
    private fun replaceBox(fi: SeekableByteChannel?, atom: Atom?, buffer: ByteBuffer) {
        fi!!.setPosition(atom!!.offset)
        fi.write(buffer)
    }

    private fun rewriteBox(buffer: ByteBuffer, box: Box): Boolean {
        return try {
            buffer.clear()
            box.write(buffer)
            if (buffer.hasRemaining()) {
                if (buffer.remaining() < 8) return false
                buffer.putInt(buffer.remaining())
                buffer.put(byteArrayOf('f'.toByte(), 'r'.toByte(), 'e'.toByte(), 'e'.toByte()))
            }
            buffer.flip()
            true
        } catch (e: BufferOverflowException) {
            false
        }
    }

    @Throws(IOException::class)
    private fun fetchBox(fi: SeekableByteChannel?, moov: Atom?): ByteBuffer {
        fi!!.setPosition(moov!!.offset)
        return NIOUtils.fetchFromChannel(fi, moov.header.size.toInt())
    }

    private fun parseBox(oldMov: ByteBuffer): Box {
        val header = read(oldMov)
        return parseBox(oldMov, header!!, default)
    }

    @Throws(IOException::class)
    private fun getMoov(f: SeekableByteChannel?): Atom? {
        for (atom in getRootAtoms(f)) {
            if ("moov" == atom.header.fourcc) {
                return atom
            }
        }
        return null
    }

    @Throws(IOException::class)
    private fun getFragments(f: SeekableByteChannel?): List<Atom> {
        val result: MutableList<Atom> = LinkedList()
        for (atom in getRootAtoms(f)) {
            if ("moof" == atom.header.fourcc) {
                result.add(atom)
            }
        }
        return result
    }
}