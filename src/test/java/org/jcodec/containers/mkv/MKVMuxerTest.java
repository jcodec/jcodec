package org.jcodec.containers.mkv;
import static org.jcodec.common.io.IOUtils.readFileToByteArray;
import static org.jcodec.containers.mkv.MKVType.Block;
import static org.jcodec.containers.mkv.MKVType.BlockGroup;
import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.DocType;
import static org.jcodec.containers.mkv.MKVType.DocTypeReadVersion;
import static org.jcodec.containers.mkv.MKVType.DocTypeVersion;
import static org.jcodec.containers.mkv.MKVType.EBML;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.SimpleBlock;
import static org.jcodec.containers.mkv.MKVType.Timecode;
import static org.jcodec.containers.mkv.MKVType.findFirst;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlString;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MKVMuxerTest {

    private MKVTestSuite suite;

    
    @Ignore @Test
    public void _testRead10Frames() throws Exception {
        byte[][] rawFrames = new byte[10][];
        rawFrames[0] = readFileToByteArray(new File("src/test/resources/mkv/10frames01.vp8"));
        rawFrames[1] = readFileToByteArray(new File("src/test/resources/mkv/10frames02.vp8"));
        rawFrames[2] = readFileToByteArray(new File("src/test/resources/mkv/10frames03.vp8"));
        rawFrames[3] = readFileToByteArray(new File("src/test/resources/mkv/10frames04.vp8"));
        rawFrames[4] = readFileToByteArray(new File("src/test/resources/mkv/10frames05.vp8"));
        rawFrames[5] = readFileToByteArray(new File("src/test/resources/mkv/10frames06.vp8"));
        rawFrames[6] = readFileToByteArray(new File("src/test/resources/mkv/10frames07.vp8"));
        rawFrames[7] = readFileToByteArray(new File("src/test/resources/mkv/10frames08.vp8"));
        rawFrames[8] = readFileToByteArray(new File("src/test/resources/mkv/10frames09.vp8"));
        rawFrames[9] = readFileToByteArray(new File("src/test/resources/mkv/10frames10.vp8"));
        File file = new File("src/test/resources/mkv/10frames.webm");
        FileInputStream inputStream = new FileInputStream(file);
        MKVParser parser = new MKVParser(new FileChannelWrapper(inputStream.getChannel()));
        List<EbmlMaster> tree = null;
        try {
            tree = parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        MKVType[] path = { MKVType.Segment, MKVType.Cluster };
        EbmlMaster[] cc =  MKVType.findAllTree(tree, EbmlMaster.class, path);
        inputStream = new FileInputStream(file);
        Assert.assertNotNull(cc);
        Assert.assertEquals(2, cc.length);
        
        try {    
            FileChannel frameReadingChannel = inputStream.getChannel();
            List<MkvBlock> bs = getBlocksByTrackNumber(cc[0], 1);
            Assert.assertNotNull(bs);
            Assert.assertEquals(5, bs.size());
            for (int i = 0; i < 5; i++) {
                FileChannel channel = frameReadingChannel;
                ByteBuffer bb = ByteBuffer.allocate(bs.get(i).dataLen);
                frameReadingChannel.position(bs.get(i).dataOffset);
                frameReadingChannel.read(bb);
                ByteBuffer[] frames = bs.get(i).getFrames(bb);
                
                Assert.assertNotNull(frames);
                Assert.assertEquals(1, frames.length);
                Assert.assertArrayEquals(rawFrames[i], MKVMuxerTest.bufferToArray(frames[0]));
            }
            bs = getBlocksByTrackNumber(cc[1], 1);
            Assert.assertNotNull(bs);
            Assert.assertEquals(5, bs.size());
            for (int i = 5; i < 10; i++) {
                ByteBuffer bb = ByteBuffer.allocate(bs.get(i-5).dataLen);
                frameReadingChannel.position(bs.get(i-5).dataOffset);
                frameReadingChannel.read(bb);
                ByteBuffer[] frames = bs.get(i - 5).getFrames(bb);
                Assert.assertNotNull(frames);
                Assert.assertEquals(1, frames.length);
                Assert.assertArrayEquals(rawFrames[i], MKVMuxerTest.bufferToArray(frames[0]));
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Ignore @Test
    public void showKeyFrames() throws IOException {
        FileInputStream inputStream = new FileInputStream(suite.test5);
        MKVParser p = new MKVParser(new FileChannelWrapper(inputStream.getChannel()));
        List<EbmlMaster> t = null;
        try {
            t = p.parse();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        MKVType[] path1 = { Segment, Cluster };
        for (EbmlMaster c : MKVType.findAllTree(t, EbmlMaster.class, path1)) {
            for (EbmlBase e : c.children) {
                if (e.type.equals(SimpleBlock)) {
                    MkvBlock be = (MkvBlock) e;
                    System.out.println("offset: " + be.size() + " timecode: " + be.timecode);

                } else if (e.type.equals(BlockGroup)) {
                    MKVType[] path = { BlockGroup, MKVType.Block };
                    MkvBlock be = (MkvBlock) MKVType.findFirst(e, path);
                    System.out.println("offset: " + be.size() + " timecode: " + be.timecode);
                }
            }
        }
    }

    public void printTrackInfo() throws Exception {
        FileInputStream inputStream = new FileInputStream(suite.test1);
        MKVParser parser = new MKVParser(new FileChannelWrapper(inputStream.getChannel()));
        List<EbmlMaster> tree = null;
        try {
            tree = parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        MKVType[] path = { MKVType.Segment, MKVType.Cues, MKVType.CuePoint, MKVType.CueTime };
        EbmlUint[] tcs = MKVType.findAllTree(tree, EbmlUint.class, path);
        for (EbmlUint tc : tcs)
            System.out.println("CueTime " + tc.getUint() + " " + tc.offset);
    }

    @Ignore @Test
    public void _testEBMLHeaderMuxin() throws Exception {
        EbmlMaster ebmlHeaderElem = (EbmlMaster) MKVType.createByType(EBML);

        EbmlString docTypeElem = (EbmlString) MKVType.createByType(DocType);
        docTypeElem.setString("matroska");

        EbmlUint docTypeVersionElem = (EbmlUint) MKVType.createByType(DocTypeVersion);
        docTypeVersionElem.setUint(1);

        EbmlUint docTypeReadVersionElem = (EbmlUint) MKVType.createByType(DocTypeReadVersion);
        docTypeReadVersionElem.setUint(1);

        ebmlHeaderElem.add(docTypeElem);
        ebmlHeaderElem.add(docTypeVersionElem);
        ebmlHeaderElem.add(docTypeReadVersionElem);
        ByteBuffer bb = ebmlHeaderElem.getData();

        System.out.println("c: " + bb.capacity() + " p: " + bb.position() + " l: " + bb.limit());
    }

    @Ignore @Test
    public void _testEbmlMasterMuxig() throws Exception {
        EbmlMaster ebmlHeaderElem = (EbmlMaster) MKVType.createByType(EBML);

        EbmlString docTypeElem = (EbmlString) MKVType.createByType(DocType);
        docTypeElem.setString("matroska");

        ebmlHeaderElem.add(docTypeElem);
        ByteBuffer bb = ebmlHeaderElem.getData();

        Assert.assertArrayEquals(new byte[] { 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, (byte) 0x8B, 0x42, (byte) 0x82, (byte) 0x88, 0x6d, 0x61, 0x74, 0x72, 0x6f, 0x73, 0x6b, 0x61 }, bb.array());
    }

    @Ignore @Test
    public void _testEmptyEbmlMasterMuxig() throws Exception {
        EbmlMaster ebmlHeaderElem = (EbmlMaster) MKVType.createByType(EBML);

        ByteBuffer bb = ebmlHeaderElem.getData();

        Assert.assertArrayEquals(new byte[] { 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, (byte) 0x80 }, bb.array());
    }

    @Before
    public void setUp() throws IOException {
        suite = MKVTestSuite.read();
        if (!suite.isSuitePresent())
            Assert.fail("MKV test suite is missing, please download from http://www.matroska.org/downloads/test_w1.html, and save to the path recorded in src/test/resources/mkv/suite.properties");
    }

    @Ignore @Test
    public void _copyMuxing() throws Exception {
        FileInputStream inputStream = new FileInputStream(suite.test3);
        MKVParser parser = new MKVParser(new FileChannelWrapper(inputStream.getChannel()));
        List<EbmlMaster> tree = null;
        try {
            tree = parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        
        
        
        FileInputStream remuxerInputStream = new FileInputStream(suite.test3);
        FileOutputStream os = new FileOutputStream(new File(suite.test3.getParent(), "copy-" + suite.test3.getName()));
        

        try {
            FileChannel channel = remuxerInputStream.getChannel();
            MKVType[] path = { Segment, Cluster, SimpleBlock };
            for (MkvBlock be : MKVType.findAllTree(tree, MkvBlock.class, path)) {
                ByteBuffer bb = ByteBuffer.allocate(be.dataLen);
                
                channel.position(be.dataOffset);
                int read = channel.read(bb);
                bb.flip();
                be.readFrames(bb);
            }
            MKVType[] path1 = { Segment, Cluster, BlockGroup, Block };
            
            for (MkvBlock be : MKVType.findAllTree(tree, MkvBlock.class, path1)) {
                ByteBuffer bb = ByteBuffer.allocate(be.dataLen);
                
                channel.position(be.dataOffset);
                int read = channel.read(bb);
                bb.flip();
                be.readFrames(bb);
            }

            for (EbmlMaster e : tree)
                e.mux(new FileChannelWrapper(os.getChannel()));
        } finally {
            if (remuxerInputStream != null)
                remuxerInputStream.close();
            if (os != null)
                os.close();
        }

    }

    public void testName() throws Exception {
        FileInputStream inputStream = new FileInputStream(suite.test3);
        MKVParser parser = new MKVParser(new FileChannelWrapper(inputStream.getChannel()));
        List<EbmlMaster> tree = null;
        try {
            tree = parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        MKVType[] path = { MKVType.Segment, MKVType.Cluster };
        EbmlMaster[] cc = MKVType.findAllTree(tree, EbmlMaster.class, path);
        printCueTable(cc);
    }

    private void printCueTable(EbmlMaster[] cc) {
        long time = 0;
        long predictedOffset = 0;
        for (EbmlMaster c : cc) {
            long csize = c.size();
            MKVType[] path = { MKVType.Cluster, MKVType.Timecode };
            System.out.println("cluster " + ((EbmlUint) MKVType.findFirst(c, path)).getUint() + " size: " + csize + " predOffset: " + predictedOffset);
            long min = getMinTimecode(c, 1);
            long max = getMaxTimecode(c, 1);
            while (min <= time && time <= max) {
                System.out.println("timecode: " + time + " offset: " + c.offset);
                time += 1000;
            }
            predictedOffset += csize;
        }
    }
    
    public static long getMinTimecode(EbmlMaster c, int trackNr) {
        MKVType[] path = { Cluster, Timecode };
        EbmlUint timecode = (EbmlUint) MKVType.findFirst(c, path);
        long clusterTimecode = timecode.getUint();
        long minTimecode = clusterTimecode;
        for (MkvBlock be : getBlocksByTrackNumber(c, trackNr))
            if (clusterTimecode + be.timecode < minTimecode)
                minTimecode = clusterTimecode + be.timecode;

        return minTimecode;
    }

    public static long getMaxTimecode(EbmlMaster c, int trackNr) {
        MKVType[] path = { Cluster, Timecode };
        EbmlUint timecode = (EbmlUint) findFirst(c, path);
        long clusterTimecode = timecode.getUint();
        long maxTimecode = clusterTimecode;
        for (MkvBlock be : getBlocksByTrackNumber(c, trackNr))
            if (clusterTimecode + be.timecode > maxTimecode)
                maxTimecode = clusterTimecode + be.timecode;

        return maxTimecode;
    }
    
    public static List<MkvBlock> getBlocksByTrackNumber(EbmlMaster c, long nr) {
        List<MkvBlock> blocks = new ArrayList<MkvBlock>();
        for (EbmlBase child : c.children) {
            MkvBlock block = null;
            if (child.type.equals(SimpleBlock))
                block = (MkvBlock) child;
            else if (child.type.equals(BlockGroup)) {
                MKVType[] path = { BlockGroup, Block };
                block = (MkvBlock) findFirst(child, path);
            } else
                continue;

            if (block.trackNumber == nr)
                blocks.add(block);
        }
        return blocks;
    }

    public static byte[] bufferToArray(ByteBuffer bb) {
        byte[] ar = new byte[bb.remaining()];
        bb.get(ar);
        return ar;
    }

}
