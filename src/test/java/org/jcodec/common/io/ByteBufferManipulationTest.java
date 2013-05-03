package org.jcodec.common.io;

import java.nio.ByteBuffer;

import junit.framework.Assert;

import org.junit.Test;

public class ByteBufferManipulationTest {

    @Test
    public void test() {
        ByteBuffer bb = ByteBuffer.wrap(new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05});
        bb.limit(3);
        byte[] a = bb.array();
        Assert.assertEquals(6, a.length);
    }

}
