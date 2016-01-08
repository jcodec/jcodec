package org.jcodec.containers.mkv;

import static org.jcodec.containers.mkv.MKVType.Cluster;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class CuesFactoryTest {

    @Ignore @Test
    public void testWithValidCues() throws IOException {
        FileInputStream inputStream = new FileInputStream(MKVMuxerTest.tildeExpand("~/References/mkv.test/test2.webm"));
        FileChannel iFS = inputStream.getChannel();
        MKVParser p = new MKVParser(new FileChannelWrapper(iFS));
        List<EbmlMaster> t = null;
        try {
            t = p.parse();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        EbmlMaster[] ccs = MKVType.findAll(t, EbmlMaster.class, MKVType.Segment, MKVType.Cluster);
        long baseOffset = 0;
        
        baseOffset += getSizeIfPresent(MKVType.<EbmlBase>findFirst(t, MKVType.Segment, MKVType.SeekHead));
        baseOffset += getSizeIfPresent(MKVType.<EbmlBase>findFirst(t, MKVType.Segment, MKVType.Info));
        baseOffset += getSizeIfPresent(MKVType.<EbmlBase>findFirst(t, MKVType.Segment, MKVType.Tracks));
        baseOffset += getSizeIfPresent(MKVType.<EbmlBase>findFirst(t, MKVType.Segment, MKVType.Tags ));

        System.out.println(" baseOffset "+baseOffset);
        CuesFactory indexer = new CuesFactory(baseOffset, 1);
        EbmlBase origCues = MKVType.findFirst(t, MKVType.Segment, MKVType.Cues);
        for (EbmlMaster c : ccs)
            indexer.add(CuesFactory.CuePointMock.make(c));
        
        EbmlMaster cues = indexer.createCues();
//        Assert.assertEquals(131, cues.size());
        Assert.assertEquals("Number of CuePoints must match", MKVType.findAll(origCues, EbmlBase.class, MKVType.Cues, MKVType.CuePoint).length, MKVType.findAll(cues, EbmlBase.class, MKVType.Cues, MKVType.CuePoint).length);
        Assert.assertEquals("Number of CueClusterPositions must match", MKVType.findAll(origCues, EbmlBase.class, MKVType.Cues, MKVType.CuePoint, MKVType.CueTrackPositions, MKVType.CueClusterPosition).length, MKVType.findAll(cues, EbmlBase.class, MKVType.Cues, MKVType.CuePoint, MKVType.CueTrackPositions, MKVType.CueClusterPosition).length);
        Assert.assertEquals(origCues.size(), cues.size());
        
        EbmlUint cueClusterPosition = (EbmlUint) MKVType.findFirst(cues, MKVType.Cues, MKVType.CuePoint, MKVType.CueTrackPositions, MKVType.CueClusterPosition);
//        Assert.assertEquals(292, cueClusterPosition.get());
        
        EbmlUint origCueClusterPosition = (EbmlUint) MKVType.findFirst(origCues, MKVType.Cues, MKVType.CuePoint, MKVType.CueTrackPositions, MKVType.CueClusterPosition);
        Assert.assertEquals(cueClusterPosition.get(), origCueClusterPosition.get());
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
        cf.add(CuesFactory.CuePointMock.make(Cluster.id, 0, 278539));
        byte[] array = cf.createCues().getData().array();
        Assert.assertEquals(19, array.length);
    }

    @Test
    public void testLengthOfIndexWithTwoEntries() throws Exception {
        CuesFactory cf = new CuesFactory(1024, 1);
        cf.add(CuesFactory.CuePointMock.make(Cluster.id, 0, 278539));
        cf.add(CuesFactory.CuePointMock.make(Cluster.id, 2, 278539));
        byte[] array = cf.createCues().getData().array();
        Assert.assertEquals(34, array.length);
    }
    
}
