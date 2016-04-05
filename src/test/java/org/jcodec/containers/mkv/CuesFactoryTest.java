package org.jcodec.containers.mkv;
import static org.jcodec.containers.mkv.MKVType.Cluster;

import org.jcodec.Utils;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import js.io.FileInputStream;
import js.io.IOException;
import js.lang.System;
import js.nio.channels.FileChannel;
import js.util.List;

public class CuesFactoryTest {

    @Ignore @Test
    public void testWithValidCues() throws IOException {
        FileInputStream inputStream = new FileInputStream(Utils.tildeExpand("~/References/mkv.test/test2.webm"));
        FileChannel iFS = inputStream.getChannel();
        MKVParser p = new MKVParser(new FileChannelWrapper(iFS));
        List<EbmlMaster> t = null;
        try {
            t = p.parse();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        MKVType[] path7 = { MKVType.Segment, MKVType.Cluster };
        EbmlMaster[] ccs = MKVType.findAllTree(t, EbmlMaster.class, path7);
        long baseOffset = 0;
        MKVType[] path = { MKVType.Segment, MKVType.SeekHead };
        
        baseOffset += getSizeIfPresent(MKVType.<EbmlBase>findFirstTree(t, path));
        MKVType[] path1 = { MKVType.Segment, MKVType.Info };
        baseOffset += getSizeIfPresent(MKVType.<EbmlBase>findFirstTree(t, path1));
        MKVType[] path2 = { MKVType.Segment, MKVType.Tracks };
        baseOffset += getSizeIfPresent(MKVType.<EbmlBase>findFirstTree(t, path2));
        MKVType[] path3 = { MKVType.Segment, MKVType.Tags };
        baseOffset += getSizeIfPresent(MKVType.<EbmlBase>findFirstTree(t, path3));

        System.out.println(" baseOffset "+baseOffset);
        CuesFactory indexer = new CuesFactory(baseOffset, 1);
        MKVType[] path4 = { MKVType.Segment, MKVType.Cues };
        EbmlBase origCues = MKVType.findFirstTree(t, path4);
        for (EbmlMaster c : ccs)
            indexer.add(CuesFactory.CuePointMock.make(c));
        
        EbmlMaster cues = indexer.createCues();
        MKVType[] path8 = { MKVType.Cues, MKVType.CuePoint };
        MKVType[] path9 = { MKVType.Cues, MKVType.CuePoint };
//        Assert.assertEquals(131, cues.size());
        Assert.assertEquals("Number of CuePoints must match", MKVType.findAll(origCues, EbmlBase.class, false, path8).length, MKVType.findAll(cues, EbmlBase.class, false, path9).length);
        MKVType[] path10 = { MKVType.Cues, MKVType.CuePoint, MKVType.CueTrackPositions, MKVType.CueClusterPosition };
        MKVType[] path11 = { MKVType.Cues, MKVType.CuePoint, MKVType.CueTrackPositions, MKVType.CueClusterPosition };
        Assert.assertEquals("Number of CueClusterPositions must match", MKVType.findAll(origCues, EbmlBase.class, false, path10).length, MKVType.findAll(cues, EbmlBase.class, false, path11).length);
        Assert.assertEquals(origCues.size(), cues.size());
        MKVType[] path5 = { MKVType.Cues, MKVType.CuePoint, MKVType.CueTrackPositions, MKVType.CueClusterPosition };
        
        EbmlUint cueClusterPosition = (EbmlUint) MKVType.findFirst(cues, path5);
        MKVType[] path6 = { MKVType.Cues, MKVType.CuePoint, MKVType.CueTrackPositions, MKVType.CueClusterPosition };
//        Assert.assertEquals(292, cueClusterPosition.get());
        
        EbmlUint origCueClusterPosition = (EbmlUint) MKVType.findFirst(origCues, path6);
        Assert.assertEquals(cueClusterPosition.getUint(), origCueClusterPosition.getUint());
    }

    private long getSizeIfPresent(EbmlBase element) {
        if (element != null){
            System.out.println("Add "+element.type+" size "+element.size());
            return element.size();
        }
        return 0;
    }
    
    @Test
    public void testEntryLength() throws Exception {
        System.out.println(CuesFactory.estimateCuePointSize(8,8,8));
    }
    
    @Test
    public void testLengthOfIndexWithSingleEntry() throws Exception {
        CuesFactory cf = new CuesFactory(1024, 1);
        cf.add(CuesFactory.CuePointMock.doMake(Cluster.id, 0, 278539));
        byte[] array = cf.createCues().getData().array();
        Assert.assertEquals(19, array.length);
    }

    @Test
    public void testLengthOfIndexWithTwoEntries() throws Exception {
        CuesFactory cf = new CuesFactory(1024, 1);
        cf.add(CuesFactory.CuePointMock.doMake(Cluster.id, 0, 278539));
        cf.add(CuesFactory.CuePointMock.doMake(Cluster.id, 2, 278539));
        byte[] array = cf.createCues().getData().array();
        Assert.assertEquals(34, array.length);
    }
    
}
