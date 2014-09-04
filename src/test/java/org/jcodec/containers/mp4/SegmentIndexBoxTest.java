package org.jcodec.containers.mp4;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.jcodec.containers.mp4.boxes.SegmentIndexBox;
import org.junit.Test;

public class SegmentIndexBoxTest {

    /** Example sidx box
     * <pre>
     * 00000000  00 00 00 2c 73 69 64 78  00 00 00 00 00 00 00 01  |...,sidx........|
     * 00000010  00 01 5f 90 00 00 00 00  00 00 00 00 00 00 00 01  |.._.............|
     * 00000020  00 06 dc 00 00 06 99 30  90 00 00 00              |.......0....|
     * </pre>
     */
    private final static byte[] expected = new byte[] { 0x00, 0x00, 0x00, 0x2c, 0x73, 0x69, 0x64, 0x78, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x5f, (byte) 0x90, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x06, (byte) 0xdc, 0x00, 0x00, 0x06, (byte) 0x99, 0x30,
            (byte) 0x90, 0x00, 0x00, 0x00, };

    @Test
    public void testParse() throws Exception {
        SegmentIndexBox sidx = new SegmentIndexBox();
        ByteBuffer input = ByteBuffer.wrap(expected);
        input.position(8);
        sidx.parse(input);
        assertEquals(90000L, sidx.timescale);
        assertEquals(1, sidx.reference_count);

        ByteBuffer actual = ByteBuffer.allocate(expected.length);
        sidx.write(actual);

        assertArrayEquals(expected, actual.array());
    }

}
