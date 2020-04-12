package org.jcodec.codecs.y4m

import org.jcodec.asByteBuffer
import org.jcodec.common.Codec
import org.jcodec.common.VideoCodecMeta
import org.jcodec.common.io.ByteBufferSeekableByteChannel
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Size
import org.jcodec.hexDump
import org.jcodec.toByteArray
import org.jcodec.toHex
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.test.assertEquals

class Y4MMuxerTest {
    @Test
    fun mux() {
        val allocate = ByteBuffer.allocate(420)
        val muxer = Y4MMuxer(ByteBufferSeekableByteChannel.writeToByteBuffer(allocate))
        val addVideoTrack = muxer.addVideoTrack(Codec.RAW, VideoCodecMeta.createSimpleVideoCodecMeta(Size(8, 8), ColorSpace.YUV420J))
        addVideoTrack.addFrame(Packet.createPacket(ByteArray(4 * 4 * 3 / 2).also { it.fill(42) }.asByteBuffer(), 0, 0, 0, 0, Packet.FrameType.UNKNOWN, null))
        allocate.flip()
        val expected = "YUV4MPEG2 W8 H8 F25:1 Ip A0:0 C420jpeg XYSCSS=420JPEG\nFRAME\n************************"
        val actual = String(allocate.toByteArray())
        assertEquals(expected, actual)

    }
}