package org.jcodec.codecs.aac;

import org.jcodec.common.AudioFormat;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.demuxer.MP4DemuxerTest;
import org.jcodec.platform.Platform;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public class AACUtilsTest {
    @Test
    public void testParseEsds() throws IOException {
        URL resource = Platform.getResource(MP4DemuxerTest.class, "a01_0023.mp4");
        File source = new File(resource.getFile());
        AudioSampleEntry sampleEntry = (AudioSampleEntry) MP4Util.parseMovie(source).getAudioTracks().get(0).getSampleEntries()[0];
        AudioFormat format = AACUtils.getMetadata(sampleEntry).getFormat();
        assertEquals(16, format.getSampleSizeInBits());
        assertEquals(48000, format.getSampleRate());
        assertEquals(2, format.getChannels());
    }
}
