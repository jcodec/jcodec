package org.jcodec.containers.mxf;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.jcodec.TestData;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.containers.mxf.MXFDemuxer.MXFDemuxerTrack;
import org.junit.Test;

public class MXFDemuxerTest {
    
    @Test
    public void testOpenAvidMxf() throws Exception {
        MXFDemuxer demuxer = new MXFDemuxer(NIOUtils.readableChannel(TestData.AVID_MXF));
        MXFDemuxerTrack videoTrack = demuxer.getVideoTrack();
        assertNotNull(videoTrack);
        Packet nextFrame = videoTrack.nextFrame();
        assertNotNull(nextFrame);
    }

    @Test
    public void testMxfTimecode() throws Exception {
        String timecode = timecodeFromMxf(TestData.TIMECODE_MXF);
        assertEquals("02:00:00:00", timecode);
    }

    private static String timecodeFromMxf(File mxf) throws IOException {
        TapeTimecode _timecodeFromMxf = MXFDemuxer.readTapeTimecode(mxf);
        return _timecodeFromMxf == null ? null : _timecodeFromMxf.toString(); 
    }
    
    @Test
    public void testTimecodeWithOffset() throws Exception {
        String tc = timecodeFromMxf(TestData.TIMECODE_OFFSET_MXF);
        System.out.println(tc);
        assertEquals("06:01:40:11", tc);
    }

    @Test
    public void testTimecodeWithOffset2() throws Exception {
        String tc = timecodeFromMxf(TestData.TWO_TIMECODES_MXF);
        System.out.println(tc);
        assertEquals("01:00:04:05", tc);
    }

    @Test
    public void testOneMore() throws Exception {
        String tc = timecodeFromMxf(TestData.TIMECODE_MXF_3);
        System.out.println(tc);
        assertEquals("01:02:31:06", tc);

    }
}
