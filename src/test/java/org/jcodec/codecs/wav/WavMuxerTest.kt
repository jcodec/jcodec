package org.jcodec.codecs.wav

import org.jcodec.asByteBuffer
import org.jcodec.common.AudioCodecMeta
import org.jcodec.common.AudioFormat
import org.jcodec.common.Codec
import org.jcodec.common.io.ByteBufferSeekableByteChannel
import org.jcodec.common.model.Packet
import org.jcodec.toByteArray
import org.jcodec.toHex
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.test.assertEquals

class WavMuxerTest {
    @Test
    fun mux() {
        val outbuf = ByteBuffer.allocate(44 + 42)
        val wavMuxer = WavMuxer(ByteBufferSeekableByteChannel.writeToByteBuffer(outbuf))
        val track = wavMuxer.addAudioTrack(Codec.RAW, AudioCodecMeta.fromAudioFormat(AudioFormat.MONO_48K_S16_LE))
        val data = ByteArray(42).also { it.fill(42) }
        track.addFrame(Packet.createPacket(data.asByteBuffer(), 0, 0, 0, 0, Packet.FrameType.UNKNOWN, null))
        wavMuxer.close()
        outbuf.clear()
        val expected = "524946464e00057415645666d7420100001010-80-45000771020100646174612a0002a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a"
        assertEquals(expected, outbuf.toByteArray().toHex())
    }
}