package org.jcodec.containers.mkv;

import static org.jcodec.containers.mkv.ebml.Element.ebmlBytes;
import static org.junit.Assert.assertArrayEquals;

import java.io.FileInputStream;
import java.nio.ByteBuffer;

import org.jcodec.common.IOUtils;
import org.jcodec.containers.mkv.ebml.SignedIntegerElement;
import org.jcodec.containers.mkv.elements.BlockElement;
import org.junit.Assert;
import org.junit.Test;

public class BlockElementMuxingTest {

    @Test
    public void testXiphLacing() {
        long[] in = new long[]{187, 630, 255, 60, 0xFFFFFF};
        byte[] expecteds = new byte[]{(byte)187, (byte)255, (byte)255, 120, (byte)255, 0, 60};
        assertArrayEquals(expecteds, BlockElement.muxXiphLacing(in));
    }
    
    @Test
    public void testEbmlLacing() {
        long[] in = new long[]{187, 630, 255, 60, 0xFFFFFF};
        long[] expecteds = new long[]{187, 443, -375, -195};
        assertArrayEquals(expecteds, BlockElement.calcEbmlLacingDiffs(in));
        
        in = new long[]{480, 576, 672, 672, 672, 672, 576, 672};
        expecteds = new long[]{480, 96, 96, 0, 0, 0, -96};
        assertArrayEquals(expecteds, BlockElement.calcEbmlLacingDiffs(in));
    }
    
    @Test
    public void testMuxEbmlLacing() throws Exception {
        long[] frameSizes = new long[]{480, 576, 672, 672, 672, 672, 576, 672};
        byte[] expected = new byte[]{0x41, (byte)0xE0, 0x60, 0x5F, 0x60, 0x5F, (byte)0xBF, (byte)0xBF, (byte)0xBF, 0x5F, (byte)0x9F};
        Assert.assertArrayEquals(expected, BlockElement.muxEbmlLacing(frameSizes));
        
        frameSizes = new long[]{480, 576};
        expected = new byte[]{0x41, (byte)0xE0};
        Assert.assertArrayEquals(expected, BlockElement.muxEbmlLacing(frameSizes));
        
        frameSizes = new long[]{480, 576, 672};
        expected = new byte[]{0x41, (byte)0xE0, 0x60, 0x5F};
        Assert.assertArrayEquals(expected, BlockElement.muxEbmlLacing(frameSizes));
    }
    
    @Test
    public void testTest() throws Exception {
        assertArrayEquals(new byte[]{0x60, 0x5F}, getSignedBytes(96));
        assertArrayEquals(new byte[]{(byte)0xBF}, getSignedBytes(0));
    }
    
    @Test
    public void readSVint() throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(new byte[]{0x60, 0x5F});
        Assert.assertEquals(96, Reader.getSignedEbmlVInt(bb));
    }
    
    @Test
    public void testReadingEbmlLacing() throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(new byte[]{(byte)0x82, 0x00, 0x00, (byte)0x86, 0x07, 0x41, (byte)0xE0, 0x60, 0x5F, 0x60, 0x5F, (byte)0xBF, (byte)0xBF, (byte)0xBF, 0x5F, (byte)0x9F});
        int startPosition = 5;
        bb.position(startPosition);
        BlockElement be = new BlockElement(Type.SimpleBlock.id);
        be.offset = 0x112;
        be.dataOffset = 0x115;
        be.size = 0x1390;
        long[] sizes = new long[bb.get(4)+1];
        be.headerSize = BlockElement.readEBMLLaceSizes(bb, sizes, (int) be.size, startPosition);
        Assert.assertNotNull(sizes);
        Assert.assertEquals(bb.capacity(), be.headerSize);
        Assert.assertEquals(480, sizes[0]);
        Assert.assertEquals(480+96, sizes[1]);
        Assert.assertEquals(480+96+96, sizes[2]);
        Assert.assertEquals(480+96+96+0, sizes[3]);
        Assert.assertEquals(480+96+96+0+0, sizes[4]);
        Assert.assertEquals(480+96+96+0+0+0, sizes[5]);
        Assert.assertEquals(480+96+96+0+0+0-96, sizes[6]);
        Assert.assertEquals(be.size-((480+96+96+0+0+0-96)+(480+96+96+0+0+0)+(480+96+96+0+0)+(480+96+96+0)+(480+96+96)+(480+96)+480+be.headerSize), sizes[7]);
    }
    
    @Test
    public void testReadingXiphLacing() throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(new byte[]{(byte)0x82, 0x00, (byte)0xAE, 0x02, 0x07, 0x51, (byte)0x92, (byte)0x92, 0x50, 0x4F, 0x4B, 0x54, (byte)0xD4, (byte)0xFD});
        int startPosition = 5;
        bb.position(startPosition);
        BlockElement be = new BlockElement(Type.Block.id);
        be.offset = 0x149B0;
        be.dataOffset = 0x149B3;
        be.size = 0x353;
        long[] sizes = new long[bb.get(4)+1];
        be.headerSize = BlockElement.readXiphLaceSizes(bb, sizes, (int)be.size, startPosition);
        Assert.assertEquals(12, be.headerSize);
        long third = bb.get(7) & 0xFFL;
        long second = bb.get(6) & 0xFFL;
        long eightth = be.size - (bb.get(5) + second + third + bb.get(8) + bb.get(9) + bb.get(10) + bb.get(11) + be.headerSize);
        Assert.assertArrayEquals(new long[]{bb.get(5), second, third, bb.get(8), bb.get(9), bb.get(10), bb.get(11), eightth }, sizes);
    }
    
    @Test
    public void testReadingXiphLacingV2() throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(new byte[]{(byte)0x82, 0x00, (byte)0xAE, 0x02, 0x04, (byte)187, (byte)255, (byte)255, 120, (byte)255, 0, 60});
        int startPosition = 5;
        bb.position(startPosition);
        BlockElement be = new BlockElement(Type.Block.id);
        be.offset = 0x149B0;
        be.dataOffset = 0x149B3;
        be.size = 0x353;
        long[] sizes = new long[bb.get(4)+1];
        be.headerSize = BlockElement.readXiphLaceSizes(bb, sizes, (int)be.size, startPosition);
        Assert.assertEquals(12, be.headerSize);
        Assert.assertArrayEquals(new long[]{187, 630, 255, 60, be.size - (187 + 630 + 255 + 60 + be.headerSize) }, sizes);
    }
    
    @Test
    public void testGetSize() throws Exception {
        MKVTestSuite suite = MKVTestSuite.read();
        if (!suite.isSuitePresent())
            Assert.fail("MKV test suite is missing, please download from http://www.matroska.org/downloads/test_w1.html, and save to the path recorded in src/test/resources/mkv/suite.properties");
        
        System.out.println("Scanning file: " + suite.test1.getAbsolutePath());

        FileInputStream inputStream = new FileInputStream(suite.test1);
        try {
            SimpleEBMLParser reader = new SimpleEBMLParser(inputStream.getChannel());
            reader.parse();
            BlockElement[] blocks = Type.findAll(reader.getTree(), BlockElement.class, Type.Segment, Type.Cluster, Type.SimpleBlock);
            for (BlockElement be : blocks)
                if (be.lacingPresent){
                    Assert.assertEquals("    "+be.lacing+" Lacing block offset "+be.offset, be.size+(be.dataOffset-be.offset), be.getSize());
                }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private byte[] getSignedBytes(long i) {
        int numBytes = SignedIntegerElement.getSerializedSize(i);
        long l = i+SignedIntegerElement.signedComplement[numBytes];
        return ebmlBytes(l, numBytes);
    }

}
