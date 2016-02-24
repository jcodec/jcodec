package org.jcodec.containers.mkv;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.List;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MKVParserTest {
    
    MKVTestSuite suite;
    
    @Before
    public void setUp() throws IOException{
        suite = MKVTestSuite.read();
        if (!suite.isSuitePresent())
            Assert.fail("MKV test suite is missing, please download from http://www.matroska.org/downloads/test_w1.html, and save to the path recorded in src/test/resources/mkv/suite.properties");
    }

    public void test() throws IOException {
        System.out.println("Scanning file: " + suite.test1.getAbsolutePath());

        FileInputStream fileInputStream = new FileInputStream(suite.test1);
        MKVParser reader = new MKVParser(new FileChannelWrapper(fileInputStream.getChannel()));
        reader.parse();
        IOUtils.closeQuietly(fileInputStream);
    }
    
    @Ignore @Test
    public void testFindAll() throws IOException {
        for (File aFile : suite.allTests()) {
            System.out.println("Scanning file: " + aFile.getAbsolutePath());
            FileInputStream stream = new FileInputStream(aFile);
            MKVParser reader = new MKVParser(new FileChannelWrapper(stream.getChannel()));
            List<EbmlMaster> tree;
            try {
                tree = reader.parse();
            } finally {
                IOUtils.closeQuietly(stream);
            }
            MkvBlock[] simpleBlocks = MKVType.findAll(tree, MkvBlock.class, MKVType.Segment, MKVType.Cluster, MKVType.SimpleBlock);
            if (simpleBlocks == null || simpleBlocks.length == 0){
                simpleBlocks = MKVType.findAll(tree, MkvBlock.class, MKVType.Segment, MKVType.Cluster, MKVType.BlockGroup, MKVType.Block);
                if (simpleBlocks == null || simpleBlocks.length == 0)
                    System.err.println("No simple blocks / block groups found. Looks suspicious");
            }
            System.out.println(" simple blocks found: "+simpleBlocks.length);
        }
    }

    @Ignore @Test
    public void testFind() throws IOException {
        System.out.println("Scanning file: " + suite.test5.getAbsolutePath());
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(suite.test5);
            FileChannel iFS = stream.getChannel();
            MKVParser reader = new MKVParser(new FileChannelWrapper(iFS));
            List<EbmlMaster> t = reader.parse();
            // reader.printParsedTree();
            EbmlMaster[] allSegments = MKVType.findAll(t, EbmlMaster.class, MKVType.Segment);
            Assert.assertNotNull(allSegments);
            Assert.assertEquals(1, allSegments.length);
            
            EbmlMaster[] allClusters = MKVType.findAll(t, EbmlMaster.class, MKVType.Segment, MKVType.Cluster);
            Assert.assertNotNull(allClusters);
            Assert.assertEquals(25, allClusters.length);
        } finally {
            stream.close();
        }
    }

    @Ignore @Test
    public void testFirstElementAndSizeAsBytes() throws Exception {
        FileInputStream fis = new FileInputStream("./src/test/resources/mkv/10frames.webm");
        try {
            SeekableByteChannel channel = new FileChannelWrapper(fis.getChannel());
            Assert.assertArrayEquals(MKVType.EBML.id, MKVParser.readEbmlId(channel));
            channel.setPosition(0x0C);
            Assert.assertArrayEquals(new byte[]{0x42, (byte) 0x86}, MKVParser.readEbmlId(channel));
        } finally {
            fis.close();
        }
    }
    
    @Ignore @Test
    public void testFirstElement() throws Exception {
        FileInputStream fis = new FileInputStream("./src/test/resources/mkv/10frames.webm");
        try {
            FileChannelWrapper source = new FileChannelWrapper(fis.getChannel());
            Assert.assertArrayEquals(MKVType.EBML.id, MKVParser.readEbmlId(source));
            Assert.assertEquals(31, MKVParser.readEbmlInt(source));
        } finally {
            fis.close();
        }
    }
    
    public static void printParsedTree(OutputStream os, List<EbmlMaster> tree) throws IOException {
        for (EbmlMaster e : tree) {
            printTree(0, e, os);
        }

    }

    private static void printTree(int i, EbmlBase e, OutputStream os) throws IOException {
        os.write(printPaddedType(i, e).toString().getBytes());
        os.write("\n".getBytes());
        if (e instanceof EbmlMaster) {
            EbmlMaster parent = (EbmlMaster) e;
            for (EbmlBase child : parent.children) {
                printTree(i + 1, child, os);
            }
            os.write(printPaddedType(i, e).append(" CLOSED.").toString().getBytes());
            os.write("\n".getBytes());
        }
    }

    private static StringBuilder printPaddedType(int size, EbmlBase e) {
        StringBuilder sb = new StringBuilder();
        for (; size > 0; size--) {
            sb.append("    ");
        }
        sb.append(e.type);
        return sb;
    }
    
}
