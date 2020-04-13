package org.jcodec.containers.raw

import org.jcodec.asByteBuffer
import org.jcodec.common.AudioCodecMeta
import org.jcodec.common.AudioFormat
import org.jcodec.common.Codec
import org.jcodec.common.VideoCodecMeta
import org.jcodec.common.io.ByteBufferSeekableByteChannel
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Size
import org.jcodec.toByteArray
import org.junit.Test
import java.lang.Exception
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RawMuxerTest {
    @Test
    fun muxVideo() {
        val output = ByteBuffer.allocate(420)
        val muxer = RawMuxer(ByteBufferSeekableByteChannel.writeToByteBuffer(output))
        val track = muxer.addVideoTrack(Codec.RAW, VideoCodecMeta.createSimpleVideoCodecMeta(Size(4, 4), ColorSpace.RGB))
        track.addFrame(Packet.createPacket(ByteArray(4 * 4 * 3).also { it.fill(42) }.asByteBuffer(), 0, 0, 0, 0, Packet.FrameType.UNKNOWN, null))
        val expected = "*".repeat(48)
        assertEquals(expected, String(output.flip().toByteArray()))
    }

    @Test
    fun muxAudio() {
        val output = ByteBuffer.allocate(420)
        val muxer = RawMuxer(ByteBufferSeekableByteChannel.writeToByteBuffer(output))
        val track = muxer.addAudioTrack(Codec.RAW, AudioCodecMeta.fromAudioFormat(AudioFormat.MONO_44K_S16_LE))
        track.addFrame(Packet.createPacket(ByteArray(4 * 4 * 3).also { it.fill(42) }.asByteBuffer(), 0, 0, 0, 0, Packet.FrameType.UNKNOWN, null))
        val expected = "*".repeat(48)
        assertEquals(expected, String(output.flip().toByteArray()))
    }

    @Test
    fun videoAfterAudio() {
        val muxer = RawMuxer(ByteBufferSeekableByteChannel.writeToByteBuffer(ByteBuffer.allocate(420)))
        val atrack = muxer.addAudioTrack(Codec.RAW, AudioCodecMeta.fromAudioFormat(AudioFormat.MONO_44K_S16_LE))
        assertFailsWith<Exception> {
            val vtrack = muxer.addVideoTrack(Codec.RAW, VideoCodecMeta.createSimpleVideoCodecMeta(Size(4, 4), ColorSpace.RGB))
        }
    }

    @Test
    fun audioAfterVideo() {
        val muxer = RawMuxer(ByteBufferSeekableByteChannel.writeToByteBuffer(ByteBuffer.allocate(420)))
        val vtrack = muxer.addVideoTrack(Codec.RAW, VideoCodecMeta.createSimpleVideoCodecMeta(Size(4, 4), ColorSpace.RGB))
        assertFailsWith<Exception> {
            val atrack = muxer.addAudioTrack(Codec.RAW, AudioCodecMeta.fromAudioFormat(AudioFormat.MONO_44K_S16_LE))
        }
    }

}