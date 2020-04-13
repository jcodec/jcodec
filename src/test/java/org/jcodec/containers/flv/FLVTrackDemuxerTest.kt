package org.jcodec.containers.flv

import org.jcodec.asSequence
import org.jcodec.common.io.NIOUtils
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class FLVTrackDemuxerTest {
    @Test
    fun demux() {
        val file = File("src/test/resources/test.flv")
        val demuxer = FLVTrackDemuxer(NIOUtils.readableChannel(file))
        assertEquals(2, demuxer.tracks.size)
        val apackets = demuxer.audioTrack.asSequence().toList()
        assertEquals(422, apackets.size)
        val vpackets = demuxer.videoTrack.asSequence().toList()
        assertEquals(422, vpackets.size)

    }
}