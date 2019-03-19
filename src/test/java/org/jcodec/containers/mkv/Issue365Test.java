package org.jcodec.containers.mkv;

import java.io.File;
import java.io.IOException;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.junit.Test;

public class Issue365Test {
    @Test
    public void testDualAud() throws IOException {
        FileChannelWrapper sourceStream = NIOUtils.readableFileChannel("src/test/resources/issue365/test.mkv");
        MKVDemuxer demuxer = new MKVDemuxer(sourceStream);
    }
}
