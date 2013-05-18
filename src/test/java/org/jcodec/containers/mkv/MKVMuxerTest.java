package org.jcodec.containers.mkv;

import static org.jcodec.common.IOUtils.readFileToByteArray;
import static org.jcodec.containers.mkv.Type.Cluster;
import static org.jcodec.containers.mkv.Type.Segment;
import static org.jcodec.containers.mkv.Type.SimpleBlock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import org.jcodec.common.IOUtils;
import org.jcodec.containers.mkv.ebml.Element;
import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.ebml.StringElement;
import org.jcodec.containers.mkv.ebml.UnsignedIntegerElement;
import org.jcodec.containers.mkv.elements.BlockElement;
import org.jcodec.containers.mkv.elements.Cluster;
import org.junit.Assert;
import org.junit.Test;

public class MKVMuxerTest {
    
    public static File tildeExpand(String path){
        return new File(path.replace("~", System.getProperty("user.home")));
    }

    @Test
    public void testCopyAndReadSingleFrame() throws Exception {
        byte[] rawFrame = readFileToByteArray(new File("src/test/resources/mkv/single-frame01.vp8"));
        File file = new File("src/test/resources/mkv/single-frame.webm");
        FileInputStream inputStream = new FileInputStream(file);
        SimpleEBMLParser parser = new SimpleEBMLParser(inputStream.getChannel());
        try {
            parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        List<MasterElement> tree = parser.getTree();
        FileInputStream remuxerInputStream = new FileInputStream(file);
        File outputFile = new File(file.getParent(), "copy-"+file.getName());
        FileOutputStream os = new FileOutputStream(outputFile);
        MKVRemuxer remuxer = new MKVRemuxer(tree, remuxerInputStream.getChannel());
        try {
            remuxer.mux(os.getChannel());
        } finally {
            IOUtils.closeQuietly(remuxerInputStream);
            IOUtils.closeQuietly(os);
        }
        
        FileInputStream zz = new FileInputStream(outputFile);
        FileInputStream blocksStream = new FileInputStream(outputFile);
        SimpleEBMLParser  pp = new SimpleEBMLParser(zz.getChannel());
        try {
            pp.parse();
            Cluster c = (Cluster) Type.findFirst(pp.getTree(), Type.Segment, Type.Cluster);
            List<BlockElement> bs = c.getBlocksByTrackNumber(1);
            Assert.assertNotNull(bs);
            Assert.assertEquals(1, bs.size());
            byte[][] frames = bs.get(0).getFrames(blocksStream.getChannel());
            Assert.assertNotNull(frames);
            Assert.assertEquals(1, frames.length);
            Assert.assertArrayEquals(rawFrame, frames[0]);
        } finally {
            IOUtils.closeQuietly(zz);
            IOUtils.closeQuietly(blocksStream);
        }
        
    }
    
    @Test
    public void testCopyAndRead10Frames() throws Exception {
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
        SimpleEBMLParser parser = new SimpleEBMLParser(inputStream.getChannel());
        try {
            parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        List<MasterElement> tree = parser.getTree();
        FileInputStream remuxerInputStream = new FileInputStream(file);
        File outputFile = new File(file.getParent(), "copy-"+file.getName());
        FileOutputStream os = new FileOutputStream(outputFile);
        MKVRemuxer remuxer = new MKVRemuxer(tree, remuxerInputStream.getChannel());
        try {
            remuxer.mux(os.getChannel());
        } finally {
            IOUtils.closeQuietly(remuxerInputStream);
            IOUtils.closeQuietly(os);
        }
        
        inputStream = new FileInputStream(outputFile);
        SimpleEBMLParser  pp = new SimpleEBMLParser(inputStream.getChannel());
        try {
            pp.parse();
            Cluster[] cc =  Type.findAll(pp.getTree(), Cluster.class, Type.Segment, Type.Cluster);
            
            Assert.assertNotNull(cc);
            Assert.assertEquals(2, cc.length);
            FileChannel frameReadingChannel = inputStream.getChannel();
            List<BlockElement> bs = cc[0].getBlocksByTrackNumber(1);
            Assert.assertNotNull(bs);
            Assert.assertEquals(5, bs.size());
            for (int i = 0; i < 5; i++) {
                byte[][] frames = bs.get(i).getFrames(frameReadingChannel);
                Assert.assertNotNull(frames);
                Assert.assertEquals(1, frames.length);
                Assert.assertArrayEquals(rawFrames[i], frames[0]);
            }
            bs = cc[1].getBlocksByTrackNumber(1);
            Assert.assertNotNull(bs);
            Assert.assertEquals(5, bs.size());
            for (int i = 5; i < 10; i++) {
                byte[][] frames = bs.get(i-5).getFrames(frameReadingChannel);
                Assert.assertNotNull(frames);
                Assert.assertEquals(1, frames.length);
                Assert.assertArrayEquals(rawFrames[i], frames[0]);
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
    
    @Test
    public void testRead10Frames() throws Exception {
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
        SimpleEBMLParser parser = new SimpleEBMLParser(inputStream.getChannel());
        try {
            parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        List<MasterElement> tree = parser.getTree();
        Cluster[] cc =  Type.findAll(tree, Cluster.class, Type.Segment, Type.Cluster);
        inputStream = new FileInputStream(file);
        Assert.assertNotNull(cc);
        Assert.assertEquals(2, cc.length);
        
        try {    
            FileChannel frameReadingChannel = inputStream.getChannel();
            List<BlockElement> bs = cc[0].getBlocksByTrackNumber(1);
            Assert.assertNotNull(bs);
            Assert.assertEquals(5, bs.size());
            for (int i = 0; i < 5; i++) {
                byte[][] frames = bs.get(i).getFrames(frameReadingChannel);
                Assert.assertNotNull(frames);
                Assert.assertEquals(1, frames.length);
                Assert.assertArrayEquals(rawFrames[i], frames[0]);
            }
            bs = cc[1].getBlocksByTrackNumber(1);
            Assert.assertNotNull(bs);
            Assert.assertEquals(5, bs.size());
            for (int i = 5; i < 10; i++) {
                byte[][] frames = bs.get(i-5).getFrames(frameReadingChannel);
                Assert.assertNotNull(frames);
                Assert.assertEquals(1, frames.length);
                Assert.assertArrayEquals(rawFrames[i], frames[0]);
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
    
    
    public void testCopyAndRead20Frames() throws Exception {
        MKVTestSuite suite = MKVTestSuite.read();
        byte[][] rawFrames = new byte[20][];
        rawFrames[0] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame001.mpeg4"));
        rawFrames[1] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame002.mpeg4"));
        rawFrames[2] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame003.mpeg4"));
        rawFrames[3] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame004.mpeg4"));
        rawFrames[4] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame005.mpeg4"));
        rawFrames[5] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame006.mpeg4"));
        rawFrames[6] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame007.mpeg4"));
        rawFrames[7] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame008.mpeg4"));
        rawFrames[8] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame009.mpeg4"));
        rawFrames[9] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame010.mpeg4"));
        rawFrames[10] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame011.mpeg4"));
        rawFrames[11] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame012.mpeg4"));
        rawFrames[12] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame013.mpeg4"));
        rawFrames[13] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame014.mpeg4"));
        rawFrames[14] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame015.mpeg4"));
        rawFrames[15] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame016.mpeg4"));
        rawFrames[16] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame017.mpeg4"));
        rawFrames[17] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame018.mpeg4"));
        rawFrames[18] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame019.mpeg4"));
        rawFrames[19] = readFileToByteArray(new File("./src/test/resources/mkv/test1.frame020.mpeg4"));
        FileInputStream inputStream = new FileInputStream(suite.test1);
        SimpleEBMLParser parser = new SimpleEBMLParser(inputStream.getChannel());
        try {
            parser.parse();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        
        List<MasterElement> tree = parser.getTree();
        FileInputStream remuxerInputStream = new FileInputStream(suite.test1);
        File outputFile = new File(suite.test1.getParent(), "copy-"+suite.test1.getName());
        FileOutputStream os = new FileOutputStream(outputFile);
        MKVRemuxer remuxer = new MKVRemuxer(tree, remuxerInputStream.getChannel());
        try {
            remuxer.mux(os.getChannel());
        } finally {
            IOUtils.closeQuietly(remuxerInputStream);
            IOUtils.closeQuietly(os);
        }
        
        inputStream = new FileInputStream(outputFile);
        SimpleEBMLParser  pp = new SimpleEBMLParser(inputStream.getChannel());
        try {
            pp.parse();
            Cluster[] cc =  Type.findAll(pp.getTree(), Cluster.class, Type.Segment, Type.Cluster);
            
            Assert.assertNotNull(cc);
            Assert.assertEquals(11, cc.length);
            FileChannel frameReadingChannel = inputStream.getChannel();
            List<BlockElement> bs = cc[0].getBlocksByTrackNumber(1);
            Assert.assertNotNull(bs);
            Assert.assertEquals(25, bs.size());
            for (int i = 0; i < 20; i++) {
                byte[][] frames = bs.get(i).getFrames(frameReadingChannel);
                Assert.assertNotNull(frames);
                Assert.assertEquals(1, frames.length);
                Assert.assertArrayEquals(rawFrames[i], frames[0]);
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
    
    public void justCopy() throws Exception {
        MKVTestSuite suite = MKVTestSuite.read();
        
        FileInputStream inputStream = new FileInputStream(suite.test5);
        SimpleEBMLParser parser = new SimpleEBMLParser(inputStream.getChannel());
        try {
            parser.parse();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        List<MasterElement> tree = parser.getTree();
        FileInputStream remuxerInputStream = new FileInputStream(suite.test5);
        File outputFile = new File(suite.test5.getParent(), "copy-"+suite.test5.getName());
        FileOutputStream os = new FileOutputStream(outputFile);
        MKVRemuxer remuxer = new MKVRemuxer(tree, remuxerInputStream.getChannel());
        try {
            remuxer.mux(os.getChannel());
        } finally {
            IOUtils.closeQuietly(remuxerInputStream);
            IOUtils.closeQuietly(os);
        }
    }
    
    public void showKeyFrames() throws IOException{
        MKVTestSuite suite = MKVTestSuite.read();
        FileInputStream inputStream = new FileInputStream(suite.test5);
        SimpleEBMLParser p = new SimpleEBMLParser(inputStream.getChannel());
        try {
            p.parse();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        for(Cluster c : Type.findAll(p.getTree(), Cluster.class, Type.Segment, Type.Cluster)){
            for (Element e : c.children){
                if (e.type.equals(Type.SimpleBlock)){
                    BlockElement be = (BlockElement) e;
                        System.out.println("offset: "+be.getSize()+" timecode: "+be.timecode);
                    
                } else if (e.type.equals(Type.BlockGroup)){
                    BlockElement be = (BlockElement) Type.findFirst(e, Type.BlockGroup, Type.Block);
                        System.out.println("offset: "+be.getSize()+" timecode: "+be.timecode);
                }
            }
        }
    }
    
    public void printTrackInfo() throws Exception {
        MKVTestSuite suite = MKVTestSuite.read();
        FileInputStream inputStream = new FileInputStream(suite.test1);
        SimpleEBMLParser parser = new SimpleEBMLParser(inputStream.getChannel());
        try {
            parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        List<MasterElement> tree = parser.getTree();
        UnsignedIntegerElement[] tcs = Type.findAll(tree, UnsignedIntegerElement.class, Type.Segment, Type.Cues, Type.CuePoint, Type.CueTime);
        for(UnsignedIntegerElement tc : tcs)
            System.out.println("CueTime "+tc.get()+" "+tc.offset);
    }
    
    @Test
    public void testMatroskaBytes() throws Exception {
        Assert.assertArrayEquals(new byte[]{0x6d, 0x61, 0x74, 0x72, 0x6f, 0x73, 0x6b, 0x61}, "matroska".getBytes());
    }
    
    @Test
    public void testEBMLHeaderMuxin() throws Exception {
        MasterElement ebmlHeaderElem = (MasterElement) Type.createElementByType(Type.EBML);

        StringElement docTypeElem = (StringElement) Type.createElementByType(Type.DocType);
        docTypeElem.set("matroska");

        UnsignedIntegerElement docTypeVersionElem = (UnsignedIntegerElement) Type.createElementByType(Type.DocTypeVersion);
        docTypeVersionElem.set(1);

        UnsignedIntegerElement docTypeReadVersionElem = (UnsignedIntegerElement) Type.createElementByType(Type.DocTypeReadVersion);
        docTypeReadVersionElem.set(1);

        ebmlHeaderElem.addChildElement(docTypeElem);
        ebmlHeaderElem.addChildElement(docTypeVersionElem);
        ebmlHeaderElem.addChildElement(docTypeReadVersionElem);
        ByteBuffer bb = ebmlHeaderElem.mux();
        
        System.out.println("c: "+bb.capacity()+" p: "+bb.position()+" l: "+bb.limit());
    }
    
    @Test
    public void testMasterElementMuxig() throws Exception {
        MasterElement ebmlHeaderElem = (MasterElement) Type.createElementByType(Type.EBML);

        StringElement docTypeElem = (StringElement) Type.createElementByType(Type.DocType);
        docTypeElem.set("matroska");

        ebmlHeaderElem.addChildElement(docTypeElem);
        ByteBuffer bb = ebmlHeaderElem.mux();

        Assert.assertArrayEquals(new byte[]{0x1A, 0x45, (byte)0xDF, (byte)0xA3, (byte)0x8B, 0x42, (byte)0x82, (byte)0x88, 0x6d, 0x61, 0x74, 0x72, 0x6f, 0x73, 0x6b, 0x61}, bb.array());
    }
    
    @Test
    public void testEmptyMasterElementMuxig() throws Exception {
        MasterElement ebmlHeaderElem = (MasterElement) Type.createElementByType(Type.EBML);

        ByteBuffer bb = ebmlHeaderElem.mux();

        Assert.assertArrayEquals(new byte[]{0x1A, 0x45, (byte)0xDF, (byte)0xA3, (byte)0x80}, bb.array());
    }
    
    public void copyMuxing() throws Exception {
        MKVTestSuite suite = MKVTestSuite.read();
        FileInputStream inputStream = new FileInputStream(suite.test3);
        SimpleEBMLParser parser = new SimpleEBMLParser(inputStream.getChannel());
        try {
            parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        List<MasterElement> tree = parser.getTree();
        FileInputStream remuxerInputStream = new FileInputStream(suite.test3);
        FileOutputStream os = new FileOutputStream(new File(suite.test3.getParent(), "copy-"+suite.test3.getName()));        
        
        try {
            for(BlockElement be : Type.findAll(tree, BlockElement.class, Segment, Cluster, SimpleBlock))
                be.readData(remuxerInputStream.getChannel());
            
            for (MasterElement e : tree)
                e.mux(os.getChannel());
        } finally {
            if (remuxerInputStream != null)
                remuxerInputStream.close();
            if (os != null)
                os.close();
        }

    }
    
    public void testName() throws Exception {
        MKVTestSuite suite = MKVTestSuite.read();
        FileInputStream inputStream = new FileInputStream(suite.test3);
        SimpleEBMLParser parser = new SimpleEBMLParser(inputStream.getChannel());
        try {
            parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        Cluster[] cc = Type.findAll(parser.getTree(), Cluster.class, Type.Segment, Type.Cluster);
        printCueTable(cc);
    }

    private void printCueTable(Cluster[] cc) {
        long time = 0;
        long predictedOffset = 0;
        for(Cluster c : cc){
            long csize = c.getSize();
            System.out.println("cluster "+((UnsignedIntegerElement) Type.findFirst(c, Type.Cluster, Type.Timecode)).get()+" size: "+csize+" predOffset: "+predictedOffset);
            long min = c.getMinTimecode(1);
            long max = c.getMaxTimecode(1);
            while(min <= time && time <= max){
                System.out.println("timecode: "+time+" offset: "+c.offset);
                time += 1000;
            }
            predictedOffset += csize;
        }
    }
}
