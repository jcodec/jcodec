package org.jcodec.containers.mps

import org.jcodec.asByteBuffer
import org.jcodec.asSequence
import org.jcodec.common.io.ByteBufferSeekableByteChannel
import org.jcodec.common.io.NIOUtils
import org.jcodec.md5
import org.jcodec.toByteArray
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.channels.Channels
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MTSDemuxerTest {
    @Test
    //TODO: fix it
    fun probe() {
        assertTrue(50 <= MTSDemuxer.PROBE.probe(File("src/test/resources/test.ts").readBytes().asByteBuffer()))
        assertTrue(50 >= MTSDemuxer.PROBE.probe(File("src/test/resources/ascii.mp3").readBytes().asByteBuffer()))
    }

    @Test
    fun demux() {
        val demuxer = MTSDemuxer(NIOUtils.readableChannel(File("src/test/resources/test.ts")))
        val programs = demuxer.programs
        assertEquals(2, programs.size)
        Assert.assertArrayEquals(intArrayOf(65, 66), demuxer.programs.toIntArray().sortedArray())
        demuxer.getProgram(65).use { program ->
            val pesbytes = Channels.newInputStream(program).readAllBytes()
            val mpsDemuxer = MPSDemuxer(ByteBufferSeekableByteChannel.readFromByteBuffer(pesbytes.asByteBuffer()))
            val tracks = mpsDemuxer.tracks
            assertEquals(1, tracks.size)
            val frames = tracks.first().asSequence().toList()
            assertEquals(420, frames.size)
            assertEquals("e68175bf3a4c25277ae8fb2d92436530", frames.map { it.data }.toByteArray().md5())

        }
    }
}