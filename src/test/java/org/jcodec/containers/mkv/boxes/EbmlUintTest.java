package org.jcodec.containers.mkv.boxes;

import static org.jcodec.containers.mkv.MKVType.BlockDuration;
import static org.jcodec.containers.mkv.MKVType.CueClusterPosition;
import static org.jcodec.containers.mkv.MKVType.TrackNumber;
import static org.jcodec.containers.mkv.boxes.EbmlUint.calculatePayloadSize;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

public class EbmlUintTest {
    
    @Test
    public void testAbs(){
        Assert.assertEquals(4, abs(-4));
        Assert.assertEquals(1L, abs(0xFFFFFFFFFFFFFFFFL));
        
        // int overflow error
        Assert.assertEquals(-9223372036854775807L, abs(0x7FFFFFFFFFFFFFFFL));
    }
    
    @Test
    public void testUnsignedIntegerData() throws Exception {
        EbmlUint uie1 = new EbmlUint(BlockDuration.id);
        uie1.set(128);
        ByteBuffer bb = uie1.getData();
        Assert.assertArrayEquals(new byte[]{(byte)0x9B, (byte)0x81, (byte)0x80}, bb.array());
        
        EbmlUint uie2 = new EbmlUint(TrackNumber.id, 1);
        bb = uie2.getData();
        Assert.assertArrayEquals(new byte[]{(byte)0xD7, (byte)0x81, 0x01}, bb.array());
    }
    
    @Test
    public void testElementMuxing2() throws Exception {
        EbmlUint uie = new EbmlUint(CueClusterPosition.id, 145582);
        ByteBuffer bb = uie.getData();
        Assert.assertArrayEquals(new byte[]{(byte)0xF1, (byte)0x83, 0x02, 0x38, (byte)0xAE}, bb.array());
    }
    
    @Test
    public void testMax() throws Exception {
        Assert.assertEquals(-4, max(-4,-5));
        
        // int overflow error
        Assert.assertEquals(Long.MIN_VALUE, max(Long.MIN_VALUE, Long.MAX_VALUE));
    }
    
    
    public static long abs(long v){
        long mask = v >>> 56L;

        return (v + mask) ^ mask;
    }
    
    public static long max(long x, long y){
        long c = (x - y) >>> 63;//(x < y);
        return x ^ ((x ^ y) & -c);
    }

    
    @Test
    public void testPayloadSize() throws Exception {
        Assert.assertEquals(1, calculatePayloadSize(0x0021));
        Assert.assertEquals(1, calculatePayloadSize(0x00));
        Assert.assertEquals(3, calculatePayloadSize(0x210000));
        Assert.assertEquals(2, calculatePayloadSize(0x0100));
    }
}
