package org.jcodec.containers.mkv;

import java.io.FileInputStream;
import java.nio.channels.FileChannel;

import org.jcodec.containers.mkv.ebml.Element;
import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.ebml.StringElement;
import org.junit.Test;

public class TestBrokenFile {

    public void test() throws Exception {
        MKVTestSuite suite = MKVTestSuite.read();
        Element level0 = null;
        Element level1 = null;
        Element level2 = null;
        Element level3 = null;
        Element level4 = null;
        FileInputStream ioDS = new FileInputStream(suite.test7);
        FileChannel channel = ioDS.getChannel();
        Reader reader = new Reader(channel);

        level0 = reader.readNextElement();
        if (level0 == null) {
            throw new java.lang.RuntimeException("Error: Unable to scan for EBML elements");
        }

        if (level0.isSameEbmlId(Type.EBML.id)) {
            level1 = ((MasterElement) level0).readNextChild(reader);

            while (level1 != null) {
                level1.readData(channel);
                if (level1.isSameEbmlId(Type.DocType.id)) {
                    String DocType = ((StringElement) level1).get();
                    if (DocType.compareTo("matroska") != 0 && DocType.compareTo("webm") != 0) {
                        throw new java.lang.RuntimeException("Error: DocType is not matroska, \""
                                + ((StringElement) level1).get() + "\"");
                    }
                }
                level1 = ((MasterElement) level0).readNextChild(reader);
            }
        } else {
            throw new java.lang.RuntimeException("Error: EBML Header not the first element in the file");
        }

        level0 = reader.readNextElement();
        if (level0.isSameEbmlId(Type.Segment.id)) {
            level1 = ((MasterElement) level0).readNextChild(reader);

            while (level1 != null) {
                if (level1.isSameEbmlId(Type.Info.id)) {
                    System.out.println("Reading sergment info");
                    level1.skipData(channel);

                } else if (level1.isSameEbmlId(Type.Tracks.id)) {
                    System.out.println("Reading tracks");
                    level1.skipData(channel);

                } else if (level1.isSameEbmlId(Type.Tags.id)) {
                    System.out.println("Reading tags");
                    level1.skipData(channel);

                } else if (level1.isSameEbmlId(Type.Cues.id)) {
                    System.out.println("Reading cues");
                    parseCues(level1);
                } else if (level1.isSameEbmlId(Type.Cluster.id)) {
                    System.out.println("Reading cluster");
                    parseCluster(level1);
                }

                level1.skipData(channel);
                level1 = ((MasterElement) level0).readNextChild(reader);
            }
        } else {
            throw new java.lang.RuntimeException("Error: Segment not the second element in the file");
        }
    }

    private void parseCluster(Element level1) throws EBMLException {
        // TODO Auto-generated method stub
        
    }

    private void parseCues(Element level1) throws EBMLException{
        // TODO Auto-generated method stub
        
    }

}
