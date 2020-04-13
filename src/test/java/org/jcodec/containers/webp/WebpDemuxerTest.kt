package org.jcodec.containers.webp

import org.jcodec.asByteBuffer
import org.jcodec.asSequence
import org.jcodec.common.io.NIOUtils
import org.jcodec.md5
import org.jcodec.toByteArray
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebpDemuxerTest {
    @Test
    fun demux() {
        WebpDemuxer(NIOUtils.readableChannel(File("src/test/resources/test.webp"))).use { demuxer ->
            assertTrue(demuxer.audioTracks.isEmpty())
            assertEquals(1, demuxer.tracks.size)
            val track = demuxer.videoTracks.first()
            val frames = track.asSequence().toList()
            assertEquals(1, frames.size)
            val frame = frames.first()
            val md5 = frame.data.toByteArray().md5()
            assertEquals("47ea7fcb341514923715f8220269b493", md5)
        }
    }

    @Test
    fun probe() {
        assertTrue(50 >= WebpDemuxer.PROBE.probe("RIFF....".toByteArray().asByteBuffer()))
        assertTrue(50 >= WebpDemuxer.PROBE.probe("OLOL....GAGA....".toByteArray().asByteBuffer()))
        assertTrue(50 >= WebpDemuxer.PROBE.probe("RIFF....GAGA....".toByteArray().asByteBuffer()))
        assertTrue(50 <= WebpDemuxer.PROBE.probe("RIFF....WEBP....".toByteArray().asByteBuffer()))
    }
}