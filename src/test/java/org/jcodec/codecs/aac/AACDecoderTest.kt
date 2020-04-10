package org.jcodec.codecs.aac

import org.jcodec.*
import org.jcodec.codecs.wav.WavDemuxer
import org.jcodec.codecs.wav.WavMuxer
import org.jcodec.common.AudioCodecMeta
import org.jcodec.common.AudioFormat
import org.jcodec.common.Codec
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.Packet
import org.jcodec.containers.mp4.demuxer.MP4Demuxer
import org.jcodec.containers.mp4.demuxer.MP4DemuxerTrackMeta
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AACDecoderTest {
    @Test
    fun testMono() {
        val demuxer = MP4Demuxer.createRawMP4Demuxer(NIOUtils.readableChannel(File("src/test/resources/mono_sine.m4a")))
        val aactrack = demuxer.audioTracks.first()
        val codecPrivate = aactrack.meta.codecPrivate
        assertNotNull(codecPrivate)
        assertEquals("1188", codecPrivate.toByteArray().toHex())
        val aacDecoder = AACDecoder(codecPrivate)

        val actual = aactrack.asSequence().map {
            aacDecoder.decodeFrame(it.data, ByteBuffer.allocate(1024 * 2)).data
        }.toList().toByteArray()

        val expected = WavDemuxer(NIOUtils.readableChannel(File("src/test/resources/mono_sine_decoded.wav"))).use {
            it.audioTracks.first().asSequence().map { it.data }.toList().toByteArray()
        }

        assertArrayEquals(expected, actual)
    }


    @Test
    fun testRecordStereo() {
        val demuxer = MP4Demuxer.createRawMP4Demuxer(NIOUtils.readableChannel(File("stereo_sine.m4a")))
        val aactrack = demuxer.audioTracks.first()
        val codecPrivate = aactrack.meta.codecPrivate
        assertNotNull(codecPrivate)
//        assertEquals("1190", codecPrivate.toByteArray().toHex())
        val aacDecoder = AACDecoder(codecPrivate)

        val muxer = WavMuxer(File("tmp/stereo.wav").writableChannel())
        val wavtrack = muxer.addAudioTrack(Codec.RAW, AudioCodecMeta.fromAudioFormat(AudioFormat.STEREO_48K_S16_LE))
        aactrack.asSequence().map {
            println(it.data)
            val dec = aacDecoder.decodeFrame(it.data, ByteBuffer.allocate(1024 * 4)).data
            println(dec)
            Packet.createPacketWithData(it, dec)
        }.fold(wavtrack) { wt, p -> wt.addFrame(p); wt }
        muxer.close()
//
//        val actual = aactrack.asSequence().map {
//            aacDecoder.decodeFrame(it.data, ByteBuffer.allocate(1024 * 2)).data
//        }.toList().toByteArray()
//
//        val expected = WavDemuxer(NIOUtils.readableChannel(File("src/test/resources/mono_sine_decoded.wav"))).use {
//            it.audioTracks.first().asSequence().map { it.data }.toList().toByteArray()
//        }
//
//        assertArrayEquals(expected, actual)
    }
}
