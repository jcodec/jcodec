package org.jcodec.containers.imgseq

import org.jcodec.asSequence
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageSequenceDemuxerTest {
    @Test
    fun demux() {
        val demuxer = ImageSequenceDemuxer("src/test/resources/png/img%d.png", 7)
        assertTrue(demuxer.audioTracks.isEmpty())
        assertEquals(1, demuxer.videoTracks.size)
        assertEquals(6, demuxer.getMaxAvailableFrame())
        assertEquals(7, demuxer.meta.totalFrames)
        val track = demuxer.videoTracks.first()
        val toList = track.asSequence().toList()
        assertEquals(7, toList.size)
    }
}