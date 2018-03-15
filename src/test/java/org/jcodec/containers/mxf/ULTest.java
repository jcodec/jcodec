package org.jcodec.containers.mxf;

import org.jcodec.containers.mxf.model.UL;
import org.junit.Assert;
import org.junit.Test;

public class ULTest {
    @Test
    public void testULFromString() {
        UL expected = new UL(new byte[]{(byte) 0xff, 0xb, 1, 2, (byte) 0xff, 0xb, 1, 2});
        UL fromString = UL.newUL("ff.b.1.2.ff.b.1.2");
        Assert.assertEquals(expected, fromString);
    }

    @Test
    public void testULFromInts() {
        UL expected = new UL(new byte[]{(byte) 0xff, 0xb, 1, 2, (byte) 0xff, 0xb, 1, 2});
        UL fromInts = UL.newULFromInts(new int[]{0xff, 0xb, 1, 2, 0xff, 0xb, 1, 2});
        Assert.assertEquals(expected, fromInts);
    }
}