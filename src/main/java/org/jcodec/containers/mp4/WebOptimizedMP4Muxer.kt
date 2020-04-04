package org.jcodec.containers.mp4

import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.logging.Logger
import org.jcodec.containers.mp4.boxes.Header.Companion.createHeader
import org.jcodec.containers.mp4.boxes.MovieBox
import org.jcodec.containers.mp4.muxer.MP4Muxer
import java.io.IOException
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class WebOptimizedMP4Muxer(output: SeekableByteChannel, brand: Brand, headerSize: Int) : MP4Muxer(output, brand.fileTypeBox) {
    private val header: ByteBuffer
    private val headerPos: Long

    @Throws(IOException::class)
    override fun storeHeader(movie: MovieBox?) {
        val mdatEnd = out.position()
        val mdatSize = mdatEnd - mdatOffset + 8
        out.setPosition(mdatOffset)
        NIOUtils.writeLong(out, mdatSize)
        out.setPosition(headerPos)
        try {
            movie!!.write(header)
            header.flip()
            val rem = header.capacity() - header.limit()
            if (rem < 8) {
                header.duplicate().putInt(header.capacity())
            }
            out.write(header)
            if (rem >= 8) createHeader("free", rem.toLong()).writeChannel(out)
        } catch (e: ArrayIndexOutOfBoundsException) {
            Logger.warn("Could not web-optimize, header is bigger then allocated space.")
            createHeader("free", header.remaining().toLong()).writeChannel(out)
            out.setPosition(mdatEnd)
            MP4Util.writeMovie(out, movie)
        }
    }

    companion object {
        @Throws(IOException::class)
        fun withOldHeader(output: SeekableByteChannel, brand: Brand, oldHeader: MovieBox): WebOptimizedMP4Muxer {
            var size = oldHeader.header.size.toInt()
            val vt = oldHeader.videoTrack
            val stsc = vt!!.stsc
            size -= stsc.getSampleToChunk().size * 12
            size += 12
            val stco = vt.stco
            if (stco != null) {
                size -= stco.getChunkOffsets().size shl 2
                size += vt.frameCount shl 3
            } else {
                val co64 = vt.co64
                size -= co64!!.getChunkOffsets().size shl 3
                size += vt.frameCount shl 3
            }
            return WebOptimizedMP4Muxer(output, brand, size + (size shr 1))
        }
    }

    init {
        headerPos = output.position() - 24
        output.setPosition(headerPos)
        header = ByteBuffer.allocate(headerSize)
        output.write(header)
        header.clear()
        createHeader("wide", 8).writeChannel(output)
        createHeader("mdat", 1).writeChannel(output)
        mdatOffset = output.position()
        NIOUtils.writeLong(output, 0)
    }
}