package org.jcodec.codecs.wav

import org.jcodec.common.*
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Packet
import java.io.IOException

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Outputs integer samples into wav file
 *
 * @author The JCodec project
 */
class WavMuxer(protected val out: SeekableByteChannel) : Muxer, MuxerTrack {
    protected var header: WavHeader? = null
    protected var written = 0
    private var format: AudioFormat? = null

    @Throws(IOException::class)
    override fun addFrame(outPacket: Packet) {
        written += out.write(outPacket.getData())
    }

    @Throws(IOException::class)
    fun close() {
        out.setPosition(0)
        WavHeader.createWavHeader(format, format!!.bytesToFrames(written)).write(out)
        NIOUtils.closeQuietly(out)
    }

    override fun addVideoTrack(codec: Codec, meta: VideoCodecMeta): MuxerTrack? {
        return null
    }

    override fun addAudioTrack(codec: Codec, meta: AudioCodecMeta): MuxerTrack {
        val h = WavHeader.createWavHeader(meta.format, 0)
        header = h
        format = meta.format
        try {
            h.write(out)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return this
    }

    @Throws(IOException::class)
    override fun finish() {
        // NOP
    }

}