package org.jcodec.codecs.mpeg12;
import org.junit.Assert;
import org.junit.Test;

import js.nio.ByteBuffer;

public class MPEGDecoderTest {

    @Test
    public void testProbe() {
        MPEGDecoder decoder = new MPEGDecoder();

        Assert.assertEquals(50, decoder.probe(ByteBuffer.wrap(MPEGTestConst.mpeg())));
        Assert.assertEquals(0, decoder.probe(ByteBuffer.wrap(MPEGTestConst.prores())));

    }
}
