package org.jcodec.codecs.h264;
import org.jcodec.platform.Platform;
import org.junit.Assert;
import org.junit.Test;

import java.lang.System;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class H264UtilsTest {

    @Test
    public void testEscapeNAL() {
        short[] src = new short[] { 0x64, 0x00, 0x15, 0xac, 0xb2, 0x01, 0x00, 0x4b, 0x7f, 0xe0, 0x00, 0x60, 0x00, 0x82,
                0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x64, 0x1e, 0x2c, 0x5c, 0x90 };
        short[] tgt = new short[] { 0x64, 0x00, 0x15, 0xac, 0xb2, 0x01, 0x00, 0x4b, 0x7f, 0xe0, 0x00, 0x60, 0x00, 0x82,
                0x00, 0x00, 0x03, 0x00, 0x02, 0x00, 0x00, 0x03, 0x00, 0x64, 0x1e, 0x2c, 0x5c, 0x90 };
        
        byte[] b = Platform.copyOfByte(asByteArray(src), src.length + 2);
        ByteBuffer bb = ByteBuffer.wrap(b);
        bb.limit(bb.limit() - 2);

        H264Utils.escapeNALinplace(bb);
        
        for (byte c : b) {
            System.out.println(String.format("%02x", c & 0xff));
        }
        
        Assert.assertArrayEquals(asByteArray(tgt), b);
    }

    byte[] asByteArray(short[] src) {
        byte[] result = new byte[src.length];
        for (int i = 0; i < src.length; i++) {
            result[i] = (byte) src[i];
        }
        return result;
    }
}
