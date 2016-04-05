package org.jcodec.containers.mkv;
import static org.jcodec.common.io.IOUtils.readFileToByteArray;
import static org.jcodec.containers.mkv.MKVType.Block;
import static org.jcodec.containers.mkv.MKVType.BlockGroup;
import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.SimpleBlock;
import static org.jcodec.containers.mkv.MKVType.Timecode;
import static org.jcodec.containers.mkv.MKVType.TrackEntry;
import static org.jcodec.containers.mkv.MKVType.TrackNumber;
import static org.jcodec.containers.mkv.MKVType.Tracks;
import static org.jcodec.containers.mkv.MKVType.findAllTree;
import static org.jcodec.containers.mkv.MKVType.findFirst;

import org.jcodec.codecs.vp8.VP8Decoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.jcodec.containers.mkv.util.EbmlUtil;
import org.junit.Assert;
import org.junit.Test;

import js.io.File;
import js.io.FileInputStream;
import js.io.IOException;
import js.lang.System;
import js.nio.ByteBuffer;
import js.nio.channels.FileChannel;
import js.util.List;
public class BlockOrderingTest {
    
    @Test
    public void testFixedLacing() throws IOException {
        File file = new File("src/test/resources/mkv/fixed_lacing_simple_block.ebml");
        byte[] rawFrame = IOUtils.readFileToByteArray(file);
        MkvBlock be = new MkvBlock(SimpleBlock.id);
        be.offset = 0x00;
        be.dataLen = 0xC05;
        be.dataOffset = 0x03;
        FileInputStream fileInputStream = new FileInputStream(file);
        ByteBuffer source = ByteBuffer.allocate(fileInputStream.available());
        try {
            FileChannel channel = fileInputStream.getChannel();
            channel.position(be.dataOffset);
            channel.read(source);
            source.flip();
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
        be.read(source);
        be.readFrames(source);
        Assert.assertEquals(rawFrame.length, be.size());
        byte[] data = be.getData().array();
        System.out.println(EbmlUtil.toHexString(data));
        Assert.assertArrayEquals(rawFrame, data);
    }

    @Test
    public void testEbmlLacing() throws IOException {
        File file = new File("src/test/resources/mkv/ebml_lacing_block.ebml");
        ByteBuffer rawFrame = NIOUtils.fetchFromFile(file);
        MkvBlock be = new MkvBlock(Block.id);
        be.offset = 0x00;
        be.dataLen = 0xF22;
        be.dataOffset = 0x03;
        FileInputStream fileInputStream = new FileInputStream(file);
        ByteBuffer source = ByteBuffer.allocate(fileInputStream.available());
        try {
            FileChannel channel = fileInputStream.getChannel();
            channel.position(be.dataOffset);
            channel.read(source);
            source.flip();
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
        be.read(source);
        be.readFrames(source);
        Assert.assertEquals(rawFrame.capacity(), be.size());
        Assert.assertArrayEquals(rawFrame.array(), be.getData().array());
    }
    
    @Test
    public void testName() throws Exception {
        System.out.println(VP8Decoder.printHexByte((byte)-98));
        System.out.println(VP8Decoder.printHexByte((byte)-95));
    }
    
    @Test
    public void testXiphLacing() throws IOException {
        File file = new File("src/test/resources/mkv/xiph_lacing_block.ebml");
        byte[] rawFrame = readFileToByteArray(file);
        MkvBlock be = new MkvBlock(Block.id);
        be.offset = 0x00;
        be.dataLen = 0x353;
        be.dataOffset = 0x03;
        FileInputStream fileInputStream = new FileInputStream(file);
        ByteBuffer source = ByteBuffer.allocate(fileInputStream.available());
        try {
            FileChannel channel = fileInputStream.getChannel();
            channel.position(be.dataOffset);
            channel.read(source);
            source.flip();
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
        be.read(source);
        be.readFrames(source);
        Assert.assertEquals(rawFrame.length, be.size());
        Assert.assertArrayEquals(rawFrame, be.getData().array());
    }
    
    @Test
    public void testNoLacing() throws IOException {
        File file = new File("src/test/resources/mkv/no_lacing_simple_block.ebml");
        byte[] rawFrame = readFileToByteArray(file);
        MkvBlock be = new MkvBlock(SimpleBlock.id);
        be.offset = 0x00;
        be.dataLen = 0x304;
        be.dataOffset = 0x03;
        FileInputStream fileInputStream = new FileInputStream(file);
        ByteBuffer source = ByteBuffer.allocate(fileInputStream.available());
        try {
            FileChannel channel = fileInputStream.getChannel();
            channel.position(be.dataOffset);
            channel.read(source);
            source.flip();
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
        be.read(source);
        be.readFrames(source);
        Assert.assertEquals(be.dataLen, be.getDataSize());
        Assert.assertEquals(rawFrame.length, be.size());
        Assert.assertArrayEquals(rawFrame, be.getData().array());
    }
    
    public void test() throws IOException {
        MKVTestSuite suite = MKVTestSuite.read();
        if (!suite.isSuitePresent())
            Assert.fail("MKV test suite is missing, please download from http://www.matroska.org/downloads/test_w1.html, and save to the path recorded in src/test/resources/mkv/suite.properties");
        System.out.println("Scanning file: " + suite.test1.getAbsolutePath());

        FileInputStream inputStream = new FileInputStream(suite.test1);
        try {
            MKVParser reader = new MKVParser(new FileChannelWrapper(inputStream.getChannel()));
            List<EbmlMaster> t = reader.parse();
            printTracks(t);
            printTimecodes(t);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void printTracks(List<EbmlMaster> tree) {
        MKVType[] path = { Segment, Tracks, TrackEntry, TrackNumber };
        for (EbmlUint nr : findAllTree(tree, EbmlUint.class, path))
            System.out.println("Track nr:" + nr.getUint());
        
    }

    private void printTimecodes(List<EbmlMaster> tree) {
        MKVType[] path2 = { Segment, Cluster };
        EbmlMaster[] clusters = findAllTree(tree, EbmlMaster.class, path2);
        for (EbmlMaster c : clusters) {
            MKVType[] path = { Cluster, Timecode };
            long ctc = ((EbmlUint) findFirst(c, path)).getUint();
            long bks = 0;
            for (EbmlBase e : c.children) {
                if (e instanceof MkvBlock) {
                    MkvBlock block = (MkvBlock) e;
                    int btc = block.timecode;
                    block.absoluteTimecode = btc+ctc;
                    System.out.println("        Block timecode: " + btc + " absoluete timecode: " + (btc + ctc) + " track: " + block.trackNumber + " offset: " + e.offset +" lacing "+block.lacing);
                    bks++;
                } else if (BlockGroup.equals(e.type)) {
                    MKVType[] path1 = { BlockGroup, Block };
                    MkvBlock be = (MkvBlock) findFirst(e, path1);
                    be.absoluteTimecode = be.timecode + ctc;
                    System.out.println("        Block Group timecode: " + be.timecode + " absoluete timecode: " + (be.timecode + ctc) + " track: " + be.trackNumber + " offset: " +be.offset+" lacing "+be.lacing);
                    bks++;
                }
            }
            System.out.println("    Cluster timecode: " + ctc + " offset: " + c.offset + " bks: " + bks);
        }
    }

}
