package org.jcodec.containers.raw

import org.jcodec.common.*
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Packet
import java.io.IOException

class RawMuxer(private val ch: SeekableByteChannel) : Muxer, MuxerTrack {
    private var hasVideo = false
    private var hasAudio = false
    override fun addVideoTrack(codec: Codec, meta: VideoCodecMeta): MuxerTrack {
        if (hasAudio) throw RuntimeException("Raw muxer supports either video or audio track but not both.")
        hasVideo = true
        return this
    }

    override fun addAudioTrack(codec: Codec, meta: AudioCodecMeta): MuxerTrack {
        if (hasVideo) throw RuntimeException("Raw muxer supports either video or audio track but not both.")
        hasAudio = true
        return this
    }

    @Throws(IOException::class)
    override fun finish() {
    }

    @Throws(IOException::class)
    override fun addFrame(outPacket: Packet) {
        ch.write(outPacket.getData().duplicate())
    }

}