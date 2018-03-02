package org.jcodec.containers.dpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.junit.Test;

public class DPXReaderTest {

    private final String DPX_V2 = "src/test/resources/dpx/3k-0086400.dpx";
    private final String DPX_V1 = "src/test/resources/dpx/3k-MACG_S02_Ep040_ING_5893876_00000.dpx";

    @Test
    public void tryParseISO8601Date() {
        assertNull(DPXReader.tryParseISO8601Date(""));
        assertNotNull(DPXReader.tryParseISO8601Date("2017:09:05:14:14:59"));
        assertNotNull(DPXReader.tryParseISO8601Date("2017:09:05:14:14:58:-08"));
        assertNotNull(DPXReader.tryParseISO8601Date("2017:09:05:14:14:57:-0800"));
    }

    @Test
    public void parseMetadata_DPXV1() throws IOException {
        SeekableByteChannel _in = NIOUtils.readableChannel(new File(DPX_V1));

        try {
            DPXReader dpx = new DPXReader(_in);
            DPXMetadata dpxMetadata = dpx.parseMetadata();
            String timecodeString = dpxMetadata.getTimecodeString();
            System.out.println(timecodeString);
            assertEquals("00:58:30:00", timecodeString);
            assertEquals(DPXMetadata.V1, dpxMetadata.file.version);
        } finally {
            IOUtils.closeQuietly(_in);
        }
    }

    @Test
    public void parseMetadata_DPXV2() throws Exception {
        SeekableByteChannel _in = NIOUtils.readableChannel(new File(DPX_V2));

        try {
            DPXReader dpx = new DPXReader(_in);
            DPXMetadata dpxMetadata = dpx.parseMetadata();
            String timecodeString = dpxMetadata.getTimecodeString();
            System.out.println(timecodeString);
            assertEquals("01:00:00:00", timecodeString);
            assertEquals(DPXMetadata.V2, dpxMetadata.file.version);
        } finally {
            IOUtils.closeQuietly(_in);
        }
    }
}
