package org.jcodec.containers.mxf

import org.jcodec.TestData
import org.jcodec.asSequence
import org.jcodec.common.io.NIOUtils
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.io.IOException
import kotlin.test.assertEquals

class MXFDemuxerTest {
    @Test
    @Ignore //at this point we do not support empty index mxf files
    //https://github.com/jcodec/jcodec/issues/282
    @Throws(Exception::class)
    fun _testOpenAvidMxf() {
        val demuxer = MXFDemuxer(NIOUtils.readableChannel(TestData.AVID_EMPTY_INDEX_MXF))
        val videoTrack = demuxer.videoTrack
        Assert.assertNotNull(videoTrack)
        val nextFrame = videoTrack.nextFrame()
        Assert.assertNotNull(nextFrame)
    }

    @Test
    fun testOp() {
        val file = File("src/test/resources/mxf/LTD16562_SYN16562-PUNCH.mxf")
        val mxfDemuxer = MXFDemuxer(NIOUtils.readableChannel(file))
        val op = mxfDemuxer.op
        assertEquals(MXFDemuxer.OP.OPAtom, op)
    }

    @Test
    //TODO: fix it, it fails because mxf support is bad
    fun testAudio() {
        val file = File("src/test/resources/mxf/mono_sine.mxf")
        val mxfDemuxer = MXFDemuxer(NIOUtils.readableChannel(file))
        assertEquals(1, mxfDemuxer.audioTracks.size)
        val track = mxfDemuxer.audioTracks.first()
        val asSequence = track.asSequence()
        val toList = asSequence.map { println(it); it }.toList()
        val sumBy = toList.sumBy { it.data.remaining() }
        assertEquals(90 * 1024, sumBy)
    }

    @Test
    //TODO: fix it, it fails because mxf support is bad
    fun testVideo() {
        val file = File("src/test/resources/mxf/LTD16562_SYN16562-PUNCH.mxf")
        val mxfDemuxer = MXFDemuxer(NIOUtils.readableChannel(file))
        val track = mxfDemuxer.videoTrack
        val asSequence = track.asSequence()
        val toList = asSequence.map { println(it); it }.toList()
        val sumBy = toList.sumBy { it.data.remaining() }
        assertEquals(90 * 1024, sumBy)
    }

    @Test
    @Throws(Exception::class)
    fun testMxfTimecode() {
        val timecode = timecodeFromMxf(TestData.TIMECODE_MXF)
        Assert.assertEquals("02:00:00:00", timecode)
    }

    @Test
    @Throws(Exception::class)
    fun testTimecodeWithOffset() {
        val tc = timecodeFromMxf(TestData.TIMECODE_OFFSET_MXF)
        println(tc)
        Assert.assertEquals("06:01:40:11", tc)
    }

    @Test
    @Throws(Exception::class)
    fun testTimecodeWithOffset2() {
        val tc = timecodeFromMxf(TestData.TWO_TIMECODES_MXF)
        println(tc)
        Assert.assertEquals("01:00:04:05", tc)
    }

    @Test
    @Throws(Exception::class)
    fun testOneMore() {
        val tc = timecodeFromMxf(TestData.TIMECODE_MXF_3)
        println(tc)
        Assert.assertEquals("01:02:31:06", tc)
    }

    companion object {
        @Throws(IOException::class)
        private fun timecodeFromMxf(mxf: File): String? {
            val _timecodeFromMxf = MXFDemuxer.readTapeTimecode(mxf)
            return _timecodeFromMxf?.toString()
        }
    }
}