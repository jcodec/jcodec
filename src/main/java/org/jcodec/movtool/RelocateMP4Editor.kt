package org.jcodec.movtool

import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.logging.Logger
import org.jcodec.containers.mp4.BoxFactory.Companion.default
import org.jcodec.containers.mp4.BoxUtil.parseBox
import org.jcodec.containers.mp4.MP4Util.Atom
import org.jcodec.containers.mp4.MP4Util.getRootAtoms
import org.jcodec.containers.mp4.MP4Util.writeMovie
import org.jcodec.containers.mp4.boxes.Box
import org.jcodec.containers.mp4.boxes.Header
import org.jcodec.containers.mp4.boxes.Header.Companion.read
import org.jcodec.containers.mp4.boxes.MovieBox
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Parses MP4 file, applies the edit and saves the result in a new file.
 *
 * Relocates the movie header to the end of the file if necessary.
 *
 * @author The JCodec project
 */
class RelocateMP4Editor {
    @Throws(IOException::class)
    fun modifyOrRelocate(src: File?, edit: MP4Edit) {
        val modify = InplaceMP4Editor().modify(src, edit)
        if (!modify) relocate(src, edit)
    }

    @Throws(IOException::class)
    fun relocate(src: File?, edit: MP4Edit) {
        var f: SeekableByteChannel? = null
        try {
            f = NIOUtils.rwChannel(src)
            val moovAtom = getMoov(f)
            val moovBuffer = fetchBox(f, moovAtom)
            val moovBox = parseBox(moovBuffer) as MovieBox
            edit.apply(moovBox)
            if (moovAtom!!.offset + moovAtom.header.size < f.size()) {
                Logger.info("Relocating movie header to the end of the file.")
                f.setPosition(moovAtom.offset + 4)
                f.write(ByteBuffer.wrap(Header.FOURCC_FREE))
                f.setPosition(f.size())
            } else {
                f.setPosition(moovAtom.offset)
            }
            writeMovie(f, moovBox)
        } finally {
            NIOUtils.closeQuietly(f)
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
}