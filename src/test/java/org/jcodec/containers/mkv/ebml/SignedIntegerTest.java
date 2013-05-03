package org.jcodec.containers.mkv.ebml;

import static org.jcodec.containers.mkv.Reader.printAsHex;
import static org.jcodec.containers.mkv.ebml.Element.ebmlBytes;
import static org.jcodec.containers.mkv.ebml.SignedIntegerElement.convertToBytes;
import static org.jcodec.containers.mkv.ebml.SignedIntegerElement.convertToUnsigned;
import static org.jcodec.containers.mkv.ebml.SignedIntegerElement.getSerializedSize;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.containers.mkv.Type;
import org.junit.Assert;
import org.junit.Test;

public class SignedIntegerTest {
    
    @Test
    public void testPacking() throws Exception {
        Assert.assertEquals(1, getSerializedSize(1));
        Assert.assertEquals(1, getSerializedSize(2));
        Assert.assertEquals(2, getSerializedSize(128));
        Assert.assertEquals(3, getSerializedSize(32768));
        
        int size = getSerializedSize(100500);
        Assert.assertEquals(3, size);
        System.out.println(printAsHex(ebmlBytes(100500, size)));
    }
    
    
    @Test
    public void testNegativeVals() throws Exception {
        Assert.assertEquals(1, getSerializedSize(-3));
        Assert.assertEquals(1, getSerializedSize(0));
        Assert.assertEquals(1, getSerializedSize(10));
        Assert.assertEquals(1, getSerializedSize(6));
        Assert.assertEquals(1, getSerializedSize(27));
        Assert.assertEquals(1, getSerializedSize(5));
    }
    
    @Test
    public void testUnsignedToSignedConversion() throws Exception {
        Assert.assertEquals(60, convertToUnsigned(-3));
        Assert.assertEquals(63, convertToUnsigned(0));
        Assert.assertEquals(73, convertToUnsigned(10));
        Assert.assertEquals(69, convertToUnsigned(6));
        Assert.assertEquals(90, convertToUnsigned(27));
        Assert.assertEquals(68, convertToUnsigned(5));
    }
    
    @Test
    public void testBytePacking() throws Exception {
        Assert.assertArrayEquals(new byte[]{0x5f, 0x3f}, convertToBytes(-192));
        Assert.assertArrayEquals(new byte[]{0x5f, (byte)0x9f}, convertToBytes(-96));
        Assert.assertArrayEquals(new byte[]{0x60, 0x5f}, convertToBytes(96));
        Assert.assertArrayEquals(new byte[]{(byte) 0xBF}, convertToBytes(0));
        
        int value = -192;
        int size = getSerializedSize(value);
        value += SignedIntegerElement.signedComplement[size];
        Assert.assertEquals(2, size);
        Assert.assertArrayEquals(new byte[]{0x5f, 0x3f}, Element.ebmlBytes(value, size));
    }

    @Test
    public void test() throws IOException {
        SignedIntegerElement sie = new SignedIntegerElement(Type.BlockDuration.id);
        sie.setValue(100500);
        ByteBuffer bb = sie.mux();
        
        Assert.assertArrayEquals(new byte[]{(byte)0x9B, (byte)0x83, 0x31, (byte)0x88, (byte)0x93}, bb.array());
    }
    
    @Test
    public void testEdgeCase() throws Exception {
        SignedIntegerElement sie = new SignedIntegerElement(Type.BlockDuration.id);
        sie.setValue(-0x0FFFFF);
        ByteBuffer bb = sie.mux();
        Assert.assertArrayEquals(new byte[]{(byte)0x9B, (byte)0x83, 0x20, 0x00, 0x00}, bb.array());
    }

}
