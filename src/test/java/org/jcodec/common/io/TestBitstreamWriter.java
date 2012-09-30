package org.jcodec.common.io;

import java.io.ByteArrayOutputStream;

import org.junit.Assert;
import org.junit.Test;

public class TestBitstreamWriter {

    @Test
    public void testWrite() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BitstreamWriter bw = new BitstreamWriter(out);
        bw.writeNBit(1, 1);
        bw.writeNBit(12345, 16);
        bw.writeNBit(85412, 15);
        bw.writeNBit(452688, 20);
        bw.writeNBit(25274, 15);
        bw.flush();
        Assert.assertArrayEquals(
                new byte[] { (byte) Short.parseShort("10011000", 2), (byte) Short.parseShort("00011100", 2),
                        (byte) Short.parseShort("11001101", 2), (byte) Short.parseShort("10100100", 2),
                        (byte) Short.parseShort("01101110", 2), (byte) Short.parseShort("10000101", 2),
                        (byte) Short.parseShort("00001100", 2), (byte) Short.parseShort("01010111", 2),
                        (byte) Short.parseShort("01000000", 2) }, out.toByteArray());
    }
}
