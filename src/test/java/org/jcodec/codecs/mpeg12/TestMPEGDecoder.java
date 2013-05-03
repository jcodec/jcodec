package org.jcodec.codecs.mpeg12;

import java.nio.ByteBuffer;

import junit.framework.Assert;

import org.junit.Test;

public class TestMPEGDecoder {

    @Test
    public void testProbe() {
        MPEGDecoder decoder = new MPEGDecoder();

        Assert.assertEquals(50, decoder.probe(ByteBuffer.wrap(MPEGTestConst.mpeg())));
        Assert.assertEquals(0, decoder.probe(ByteBuffer.wrap(MPEGTestConst.prores())));

    }
}
