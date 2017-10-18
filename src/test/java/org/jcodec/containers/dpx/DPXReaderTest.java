package org.jcodec.containers.dpx;

import java.io.File;

import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.junit.Assert;
import org.junit.Test;

public class DPXReaderTest {

    private final String DPX_HEADER_PATH = "src/test/resources/dpx/3k-0086400.dpx";

    @Test
    public void parseMetadata() throws Exception {
        SeekableByteChannel _in = NIOUtils.readableChannel(new File(DPX_HEADER_PATH));

        try {
            DPXReader dpx = new DPXReader(_in);
            DPXMetadata dpxMetadata = dpx.parseMetadata();
            String timecodeString = dpxMetadata.getTimecodeString();
            System.out.println(timecodeString);
            Assert.assertEquals("01:00:00:00", timecodeString);
        } finally {
            IOUtils.closeQuietly(_in);
        }
    }

}