package org.jcodec.codecs.h264.io.model;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

public class NalUnitTest
{

    @Test
    public void testCanReadNalUnitFromValidNal()
    {
        byte[] data = new byte[] { (byte) 0x7C, (byte) 0x81 };

        final NALUnit actual = NALUnit.read(ByteBuffer.wrap(data));

        assertEquals("Wrong conversion on valid packet - idc", 3, actual.nal_ref_idc);
        assertEquals("Wrong conversion on valid packet - type", NALUnitType.FU_A, actual.type);
    }


    @Test
    public void testCanWriteNalUnitFromValidNal()
    {
        byte[] data = new byte[] { (byte) 0x7C, (byte) 0x81 };

        final NALUnit actual = NALUnit.read(ByteBuffer.wrap(data));

        assertEquals("Wrong conversion on valid packet - idc", 3, actual.nal_ref_idc);
        assertEquals("Wrong conversion on valid packet - type", NALUnitType.FU_A, actual.type);

        byte[] data2 = new byte[1];
        actual.write(ByteBuffer.wrap(data2));

        assertEquals("wrong data generated", data[0], data2[0]);
    }

}
