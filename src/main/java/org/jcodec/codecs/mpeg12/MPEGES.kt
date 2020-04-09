package org.jcodec.codecs.mpeg12

import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.Packet.FrameType
import org.jcodec.containers.mps.MPEGPacket
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Pulls frames from MPEG elementary stream
 *
 * @author The JCodec project
 */
class MPEGES(channel: ReadableByteChannel?, fetchSize: Int) : SegmentReader(channel, fetchSize) {
    private var frameNo = 0
    var lastKnownDuration: Long = 0

    /**
     * Reads one MPEG1/2 video frame from MPEG1/2 elementary stream into a
     * provided buffer.
     *
     * @param buffer
     * A buffer to use for the data.
     * @return A packet with a video frame or null at for end of the stream. The
     * data buffer inside the packet will be a sub-buffer of a 'buffer'
     * provided as an argument.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun frame(buffer: ByteBuffer): MPEGPacket? {
        val dup = buffer.duplicate()
        while (curMarker != 0x100 && curMarker != 0x1b3 && skipToMarker());
        while (curMarker != 0x100 && readToNextMarker(dup));
        readToNextMarker(dup)
        while (curMarker != 0x100 && curMarker != 0x1b3 && readToNextMarker(dup));
        dup.flip()
        val ph = MPEGDecoder.getPictureHeader(dup.duplicate())
        return if (dup.hasRemaining()) MPEGPacket(dup, 0, 90000, 0, frameNo++.toLong(),
                if (ph.picture_coding_type <= MPEGConst.IntraCoded) FrameType.KEY else FrameType.INTER, null) else null
    }// Reading to the frame header, sequence header, sequence header
    // extensions and group header go in here

    // Reading the frame header

    // Reading the slices, will stop on encounter of a frame header of the
    // next frame or a sequence header

    /**
     * Reads one MPEG1/2 video frame from MPEG1/2 elementary stream.
     *
     * @return A packet with a video frame or null at the end of stream.
     * @throws IOException
     */
    @get:Throws(IOException::class)
    val frame: MPEGPacket?
        get() {
            while (curMarker != 0x100 && curMarker != 0x1b3 && skipToMarker());
            val buffers: List<ByteBuffer> = ArrayList()
            // Reading to the frame header, sequence header, sequence header
            // extensions and group header go in here
            while (curMarker != 0x100 && !done) readToNextMarkerBuffers(buffers)

            // Reading the frame header
            readToNextMarkerBuffers(buffers)

            // Reading the slices, will stop on encounter of a frame header of the
            // next frame or a sequence header
            while (curMarker != 0x100 && curMarker != 0x1b3 && !done) readToNextMarkerBuffers(buffers)
            val dup = NIOUtils.combineBuffers(buffers)
            val ph = MPEGDecoder.getPictureHeader(dup.duplicate())
            return if (dup.hasRemaining()) MPEGPacket(dup, 0, 90000, 0, frameNo++.toLong(),
                    if (ph.picture_coding_type <= MPEGConst.IntraCoded) FrameType.KEY else FrameType.INTER, null) else null
        }
}