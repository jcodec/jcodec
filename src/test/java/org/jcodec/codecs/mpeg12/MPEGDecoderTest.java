package org.jcodec.codecs.mpeg12;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class MPEGDecoderTest {

    @Test
    public void testProbe() {
        Assert.assertEquals(50, MPEGDecoder.probe(ByteBuffer.wrap(MPEGTestConst.mpeg())));
        Assert.assertEquals(0, MPEGDecoder.probe(ByteBuffer.wrap(MPEGTestConst.prores())));
    }
}
