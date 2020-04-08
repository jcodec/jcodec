package org.jcodec.containers.mkv;
import static org.jcodec.containers.mkv.MKVType.Block;
import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.SimpleBlock;
import static org.jcodec.containers.mkv.MKVType.findAllTree;
import static org.junit.Assert.assertArrayEquals;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.FileInputStream;
import java.lang.System;
import java.nio.ByteBuffer;

public class MkvBlockTest {
    
    @Test
    public void testXiph() {
        int[] _in = new int[]{187, 630, 255, 60, 0xFFFFFF};
        byte[] expecteds = new byte[]{(byte)187, (byte)255, (byte)255, 120, (byte)255, 0, 60};
        assertArrayEquals(expecteds, MkvBlock.muxXiphLacing(_in));
    }
    
    @Test
    public void testReadEbmlInt() throws Exception {
        Assert.assertEquals(577, MkvBlock.ebmlDecode(ByteBuffer.wrap(new byte[] { 0x42, 0x41 })));

        Assert.assertEquals(33, MkvBlock.ebmlDecode(ByteBuffer.wrap(new byte[] { (byte) 0xA1 })));

        Assert.assertEquals(577, MkvBlock.ebmlDecode(ByteBuffer.wrap(new byte[] { 0x42, 0x41, 0x22, 0x22, 0x22 })));
    }
    
    @Test
    public void testEbml() {
        int[] _in = new int[]{187, 630, 255, 60, 0xFFFFFF};
        long[] expecteds = new long[]{187, 443, -375, -195};
        assertArrayEquals(expecteds, MkvBlock.calcEbmlLacingDiffs(_in));
        
        _in = new int[]{480, 576, 672, 672, 672, 672, 576, 672};
        expecteds = new long[]{480, 96, 96, 0, 0, 0, -96};
        assertArrayEquals(expecteds, MkvBlock.calcEbmlLacingDiffs(_in));
    }
    
    @Test
    public void testMuxEbml() throws Exception {
        int[] frameSizes = new int[]{480, 576, 672, 672, 672, 672, 576, 672};
        byte[] expected = new byte[]{0x41, (byte)0xE0, 0x60, 0x5F, 0x60, 0x5F, (byte)0xBF, (byte)0xBF, (byte)0xBF, 0x5F, (byte)0x9F};
        Assert.assertArrayEquals(expected, MkvBlock.muxEbmlLacing(frameSizes));
        
        frameSizes = new int[]{480, 576};
        expected = new byte[]{0x41, (byte)0xE0};
        Assert.assertArrayEquals(expected, MkvBlock.muxEbmlLacing(frameSizes));
        
        frameSizes = new int[]{480, 576, 672};
        expected = new byte[]{0x41, (byte)0xE0, 0x60, 0x5F};
        Assert.assertArrayEquals(expected, MkvBlock.muxEbmlLacing(frameSizes));
    }
    
    @Test
    public void testReadingEbml() throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(new byte[]{(byte)0x82, 0x00, 0x00, (byte)0x86, 0x07, 0x41, (byte)0xE0, 0x60, 0x5F, 0x60, 0x5F, (byte)0xBF, (byte)0xBF, (byte)0xBF, 0x5F, (byte)0x9F});
        int startPosition = 5;
        bb.position(startPosition);
        MkvBlock be = new MkvBlock(SimpleBlock.id);
        be.offset = 0x112;
        be.dataOffset = 0x115;
        be.dataLen = 0x1390;
        int[] sizes = new int[bb.get(4)+1];
        be.headerSize = MkvBlock.readEBMLLaceSizes(bb, sizes, (int) be.dataLen, startPosition);
        Assert.assertNotNull(sizes);
        Assert.assertEquals(bb.capacity(), be.headerSize);
        Assert.assertEquals(480, sizes[0]);
        Assert.assertEquals(480+96, sizes[1]);
        Assert.assertEquals(480+96+96, sizes[2]);
        Assert.assertEquals(480+96+96+0, sizes[3]);
        Assert.assertEquals(480+96+96+0+0, sizes[4]);
        Assert.assertEquals(480+96+96+0+0+0, sizes[5]);
        Assert.assertEquals(480+96+96+0+0+0-96, sizes[6]);
        Assert.assertEquals(be.dataLen-((480+96+96+0+0+0-96)+(480+96+96+0+0+0)+(480+96+96+0+0)+(480+96+96+0)+(480+96+96)+(480+96)+480+be.headerSize), sizes[7]);
    }
    
    @Test
    public void testReadingXiph() throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(new byte[]{(byte)0x82, 0x00, (byte)0xAE, 0x02, 0x07, 0x51, (byte)0x92, (byte)0x92, 0x50, 0x4F, 0x4B, 0x54, (byte)0xD4, (byte)0xFD});
        int startPosition = 5;
        bb.position(startPosition);
        MkvBlock be = new MkvBlock(Block.id);
        be.offset = 0x149B0;
        be.dataOffset = 0x149B3;
        be.dataLen = 0x353;
        int[] sizes = new int[bb.get(4)+1];
        be.headerSize = MkvBlock.readXiphLaceSizes(bb, sizes, (int)be.dataLen, startPosition);
        Assert.assertEquals(12, be.headerSize);
        int third = bb.get(7) & 0xFF;
        int second = bb.get(6) & 0xFF;
        int eightth = (int)(be.dataLen) - (bb.get(5) + second + third + bb.get(8) + bb.get(9) + bb.get(10) + bb.get(11) + be.headerSize);
        Assert.assertArrayEquals(new int[]{bb.get(5), second, third, bb.get(8), bb.get(9), bb.get(10), bb.get(11), eightth }, sizes);
    }

    @Test
    public void testReadingXiphV2() throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(new byte[]{(byte)0x82, 0x00, (byte)0xAE, 0x02, 0x04, (byte)187, (byte)255, (byte)255, 120, (byte)255, 0, 60});
        int startPosition = 5;
        bb.position(startPosition);
        MkvBlock be = new MkvBlock(Block.id);
        be.offset = 0x149B0;
        be.dataOffset = 0x149B3;
        be.dataLen = 0x353;
        int[] sizes = new int[bb.get(4)+1];
        be.headerSize = MkvBlock.readXiphLaceSizes(bb, sizes, (int)be.dataLen, startPosition);
        Assert.assertEquals(12, be.headerSize);
        Assert.assertArrayEquals(new int[]{187, 630, 255, 60, (int)(be.dataLen) - (187 + 630 + 255 + 60 + be.headerSize) }, sizes);
    }
    
    @Ignore @Test
    public void _testGetSize() throws Exception {
        MKVTestSuite suite = MKVTestSuite.read();
        if (!suite.isSuitePresent())
            Assert.fail("MKV test suite is missing, please download from http://www.matroska.org/downloads/test_w1.html, and save to the path recorded in src/test/resources/mkv/suite.properties");
        
        System.out.println("Scanning file: " + suite.test1.getAbsolutePath());

        FileInputStream inputStream = new FileInputStream(suite.test1);
        try {
            MKVParser reader = new MKVParser(new FileChannelWrapper(inputStream.getChannel()));
            MKVType[] path = { Segment, Cluster, SimpleBlock };
            
            MkvBlock[] blocks = findAllTree(reader.parse(), path).toArray(new MkvBlock[0]);
            for (MkvBlock be : blocks)
                if (be.lacingPresent){
                    Assert.assertEquals("    "+be.lacing+" Lacing block offset "+be.offset, be.dataLen+(be.dataOffset-be.offset), be.size());
                }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
    
    @Test
    public void testReadingSignedInt() throws Exception {
        Assert.assertEquals(-30, MkvBlock.ebmlDecodeSigned(ByteBuffer.wrap(new byte[]{ (byte) 0xA1})));
        Assert.assertEquals(-7614, MkvBlock.ebmlDecodeSigned(ByteBuffer.wrap(new byte[]{0x42, 0x41})));
    }

}
