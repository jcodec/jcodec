package org.jcodec.codecs.y4m

import org.jcodec.common.*
import org.jcodec.common.model.Packet
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel

/**
 * Stores frames into Y4M file.
 *
 * @author Stanislav Vitvitskiy
 */
class Y4MMuxer(private val ch: WritableByteChannel) : Muxer, MuxerTrack {
    private var headerWritten = false
    private var meta: VideoCodecMeta? = null

    @Throws(IOException::class)
    protected fun writeHeader() {
        val size = meta!!.size
        val bytes = "YUV4MPEG2 W${size.width} H${size.height} F25:1 Ip A0:0 C420jpeg XYSCSS=420JPEG\n".toByteArray()
        ch.write(ByteBuffer.wrap(bytes))
    }

    @Throws(IOException::class)
    override fun addFrame(outPacket: Packet) {
        if (!headerWritten) {
            writeHeader()
            headerWritten = true
        }
        ch.write(ByteBuffer.wrap(frameTag))
        ch.write(outPacket.data.duplicate())
    }

    override fun addVideoTrack(codec: Codec, meta: VideoCodecMeta): MuxerTrack {
        this.meta = meta
        return this
    }

    override fun addAudioTrack(codec: Codec, meta: AudioCodecMeta): MuxerTrack {
        throw RuntimeException("Y4M doesn't support audio")
    }

    @Throws(IOException::class)
    override fun finish() {
    }

    companion object {
        val frameTag = "FRAME\n".toByteArray()
    }

}