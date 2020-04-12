package org.jcodec.containers.y4m

import org.jcodec.asByteBuffer
import org.jcodec.common.io.ByteBufferSeekableByteChannel
import org.jcodec.common.model.Rational
import org.jcodec.common.model.Size
import org.jcodec.toByteArray
import org.junit.Test
import kotlin.test.assertEquals

class Y4MDemuxerTest {
    @Test
    fun demux() {
        val input = "YUV4MPEG2 W8 H8 F25:1 Ip A0:0 C420jpeg XYSCSS=420JPEG\nFRAME\n************************"
        Y4MDemuxer(ByteBufferSeekableByteChannel.readFromByteBuffer(input.toByteArray().asByteBuffer())).use { demuxer ->
            assertEquals(Rational(25, 1), demuxer.fps)
            assertEquals(Size(8,8), demuxer.meta.videoCodecMeta!!.size)
            val videoTracks = demuxer.videoTracks
            assertEquals(1, videoTracks.size)
            val track = videoTracks.first()
            val packet = track.nextFrame()
            assertEquals("************************", String(packet.data.toByteArray()))

        }
    }
}