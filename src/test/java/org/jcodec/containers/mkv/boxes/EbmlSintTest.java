package org.jcodec.containers.mkv.boxes;
import static org.jcodec.containers.mkv.MKVType.BlockDuration;
import static org.jcodec.containers.mkv.boxes.EbmlSint.convertToBytes;
import static org.jcodec.containers.mkv.boxes.EbmlSint.ebmlSignedLength;
import static org.jcodec.containers.mkv.boxes.EbmlSint.signedComplement;
import static org.jcodec.containers.mkv.util.EbmlUtil.ebmlEncodeLen;

import org.jcodec.containers.mkv.util.EbmlUtil;
import org.junit.Assert;
import org.junit.Test;

import js.io.IOException;
import js.lang.System;
import js.nio.ByteBuffer;

public class EbmlSintTest {
    
    @Test
    public void testPacking() throws Exception {
        Assert.assertEquals(1, ebmlSignedLength(1));
        Assert.assertEquals(1, ebmlSignedLength(2));
        Assert.assertEquals(2, ebmlSignedLength(128));
        Assert.assertEquals(3, ebmlSignedLength(32768));
        
        int size = ebmlSignedLength(100500);
        Assert.assertEquals(3, size);
        System.out.println(EbmlUtil.toHexString(ebmlEncodeLen(100500, size)));
    }
    
    
    @Test
    public void testNegativeVals() throws Exception {
        Assert.assertEquals(1, ebmlSignedLength(-3));
        Assert.assertEquals(1, ebmlSignedLength(0));
        Assert.assertEquals(1, ebmlSignedLength(10));
        Assert.assertEquals(1, ebmlSignedLength(6));
        Assert.assertEquals(1, ebmlSignedLength(27));
        Assert.assertEquals(1, ebmlSignedLength(5));
    }
    
    @Test
    public void testBytePacking() throws Exception {
        Assert.assertArrayEquals(new byte[]{0x5f, 0x3f}, convertToBytes(-192));
        Assert.assertArrayEquals(new byte[]{0x5f, (byte)0x9f}, convertToBytes(-96));
        Assert.assertArrayEquals(new byte[]{0x60, 0x5f}, convertToBytes(96));
        Assert.assertArrayEquals(new byte[]{(byte) 0xBF}, convertToBytes(0));
        
        int value = -192;
        int size = ebmlSignedLength(value);
        value += signedComplement[size];
        Assert.assertEquals(2, size);
        Assert.assertArrayEquals(new byte[]{0x5f, 0x3f}, ebmlEncodeLen(value, size));
    }

    @Test
    public void test() throws IOException {
        EbmlSint sie = new EbmlSint(BlockDuration.id);
        sie.setLong(100500);
        ByteBuffer bb = sie.getData();
        
        Assert.assertArrayEquals(new byte[]{(byte)0x9B, (byte)0x83, 0x31, (byte)0x88, (byte)0x93}, bb.array());
    }
    
    @Test
    public void testEdgeCase() throws Exception {
        EbmlSint sie = new EbmlSint(BlockDuration.id);
        sie.setLong(-0x0FFFFF);
        ByteBuffer bb = sie.getData();
        Assert.assertArrayEquals(new byte[]{(byte)0x9B, (byte)0x83, 0x20, 0x00, 0x00}, bb.array());
    }

}
