package org.jcodec.containers.mkv.ebml;

import java.nio.ByteBuffer;

import org.jcodec.containers.mkv.Type;
import org.junit.Assert;
import org.junit.Test;

public class UnsignedIntegerTest {

    @Test
    public void test() {
        Assert.assertEquals(1, Element.getEbmlSize(0x00));
        Assert.assertEquals(1, Element.getEbmlSize(0x7F));
        Assert.assertEquals(2, Element.getEbmlSize(0x80));
        Assert.assertEquals(2, Element.getEbmlSize(128));
    }
    
    @Test
    public void testAbs(){
        Assert.assertEquals(4, abs(-4));
        Assert.assertEquals(1L, abs(0xFFFFFFFFFFFFFFFFL));
        
        // int overflow error
        Assert.assertEquals(-9223372036854775807L, abs(0x7FFFFFFFFFFFFFFFL));
    }
    
    @Test
    public void testEbmlBytes() throws Exception {
        Assert.assertArrayEquals(new byte[]{(byte)0x81}, Element.ebmlBytes(1));
        Assert.assertArrayEquals(new byte[]{0x40, (byte)0x80}, Element.ebmlBytes(128));
        Assert.assertArrayEquals(new byte[]{0x20, 0x40, 0x00}, Element.ebmlBytes(16384));
    }
    
    @Test
    public void testUnsignedIntegerData() throws Exception {
        UnsignedIntegerElement uie1 = new UnsignedIntegerElement(Type.BlockDuration.id);
        uie1.set(128);
        ByteBuffer bb = uie1.mux();
        Assert.assertArrayEquals(new byte[]{(byte)0x9B, (byte)0x81, (byte)0x80}, bb.array());
        
        UnsignedIntegerElement uie2 = new UnsignedIntegerElement(Type.TrackNumber.id, 1);
        bb = uie2.mux();
        Assert.assertArrayEquals(new byte[]{(byte)0xD7, (byte)0x81, 0x01}, bb.array());
    }
    
    @Test
    public void testElementMuxing2() throws Exception {
        UnsignedIntegerElement uie = new UnsignedIntegerElement(Type.CueClusterPosition.id, 145582);
        ByteBuffer bb = uie.mux();
        Assert.assertArrayEquals(new byte[]{(byte)0xF1, (byte)0x83, 0x02, 0x38, (byte)0xAE}, bb.array());
    }
    
    @Test
    public void testMax() throws Exception {
        Assert.assertEquals(-4, max(-4,-5));
        
        // int overflow error
        Assert.assertEquals(Long.MIN_VALUE, max(Long.MIN_VALUE, Long.MAX_VALUE));
    }
    
    
    public static long abs(long v){
//        long mask = v >>> 8 * 8 - 1;
        long mask = v >>> 56L;

        return (v + mask) ^ mask;
    }
    
    public static long max(long x, long y){
        long c = (x - y) >>> 63;//(x < y);
        return x ^ ((x ^ y) & -c);
    }

}
