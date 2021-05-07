package org.jcodec.containers.mkv;
import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.CueClusterPosition;
import static org.jcodec.containers.mkv.MKVType.CuePoint;
import static org.jcodec.containers.mkv.MKVType.CueTime;
import static org.jcodec.containers.mkv.MKVType.CueTrack;
import static org.jcodec.containers.mkv.MKVType.CueTrackPositions;
import static org.jcodec.containers.mkv.MKVType.Cues;
import static org.jcodec.containers.mkv.MKVType.Timecode;
import static org.jcodec.containers.mkv.MKVType.createByType;
import static org.jcodec.containers.mkv.MKVType.findFirst;
import static org.jcodec.containers.mkv.boxes.EbmlUint.calculatePayloadSize;

import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.jcodec.containers.mkv.util.EbmlUtil;

import java.lang.System;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author The JCodec project
 * 
 */
public class CuesFactory {
    List<CuePointMock> a;
    private final long offsetBase;
    private long currentDataOffset = 0;
    private long videoTrackNr;
    
    public CuesFactory(long offset, long videoTrack){
        this.a = new ArrayList<CuePointMock>();
        this.offsetBase = offset;
        this.videoTrackNr = videoTrack;
        this.currentDataOffset += offsetBase;
    }
    
    public void addFixedSize(CuePointMock z) {
        z.elementOffset = currentDataOffset;
        z.cueClusterPositionSize = 8;
        currentDataOffset += z.size;
        a.add(z);
    }
    
    public void add(CuePointMock z) {
        z.elementOffset = currentDataOffset;
        z.cueClusterPositionSize = calculatePayloadSize(z.elementOffset);
        currentDataOffset += z.size;
        a.add(z);
    }
    
    public EbmlMaster createCues(){
        int estimatedSize = computeCuesSize();
        EbmlMaster cues = createByType(Cues);
        for (CuePointMock cpm : a){
            EbmlMaster cuePoint = createByType(CuePoint);
            
            EbmlUint cueTime = createByType(CueTime);
            cueTime.setUint(cpm.timecode);
            cuePoint.add(cueTime);
            
            EbmlMaster cueTrackPositions = createByType(CueTrackPositions);
            
            EbmlUint cueTrack = createByType(CueTrack);
            cueTrack.setUint(videoTrackNr);
            cueTrackPositions.add(cueTrack);
            
            EbmlUint cueClusterPosition = createByType(CueClusterPosition);
            cueClusterPosition.setUint(cpm.elementOffset+estimatedSize);
            if (cueClusterPosition.data.limit() != cpm.cueClusterPositionSize)
                System.err.println("estimated size of CueClusterPosition differs from the one actually used. ElementId: "+EbmlUtil.toHexString(cpm.id)+" "+cueClusterPosition.getData().limit()+" vs "+cpm.cueClusterPositionSize);
            cueTrackPositions.add(cueClusterPosition);
            
            cuePoint.add(cueTrackPositions);
            
            cues.add(cuePoint);
        }
        return cues;
    }
    
    public int computeCuesSize() {
        int cuesSize = estimateSize();
        boolean reindex = false;
        do {
            reindex = false;
            for (CuePointMock z : a) {
                int minByteSize = calculatePayloadSize(z.elementOffset + cuesSize);
                if (minByteSize > z.cueClusterPositionSize) {
//                    System.out.println(minByteSize + ">" + z.cueClusterPositionSize);
//                    System.err.println("Size "+cuesSize+" seems too small for element "+EbmlUtil.toHexString(z.id)+" increasing size by one.");
                    z.cueClusterPositionSize +=1;
                    cuesSize += 1;
                    reindex = true;
                    break;
                } else if (minByteSize < z.cueClusterPositionSize) {
                    throw new RuntimeException("Downsizing the index is not well thought through");
                    /*
                    System.out.println("Size "+cuesSize+" seems too small for element "+Reader.printAsHex(z.id)+" increasing size by one.");
                    z.cueClusterPositionSize -=1;
                    cuesSize -= 1;
                    reindex = true;
                    break;
                     */
                }
            }
        } while (reindex);
        return cuesSize;
    }

    public int estimateFixedSize(int numberOfClusters) {
        int s = 34*numberOfClusters; 
//        for (CuePointMock cpm : a) 
//            s += estimateCuePointSize(8, 8, 8);
        
        s += Cues.id.length + EbmlUtil.ebmlLength(s);
        return s;
    }
    
    public int estimateSize() {
        int s = 0;
        for (CuePointMock cpm : a) 
            s += estimateCuePointSize(calculatePayloadSize(cpm.timecode), calculatePayloadSize(videoTrackNr), calculatePayloadSize(cpm.elementOffset));
        
        s += Cues.id.length + EbmlUtil.ebmlLength(s);
        return s;
    }
    
    public static int estimateCuePointSize(int timecodeSizeInBytes, int trackNrSizeInBytes, int clusterPositionSizeInBytes) {
        int cueTimeSize = CueTime.id.length + EbmlUtil.ebmlLength(timecodeSizeInBytes) + timecodeSizeInBytes;
        int cueTrackPositionSize = CueTrack.id.length + EbmlUtil.ebmlLength(trackNrSizeInBytes) + trackNrSizeInBytes +
                                     CueClusterPosition.id.length + EbmlUtil.ebmlLength(clusterPositionSizeInBytes) + clusterPositionSizeInBytes;
        cueTrackPositionSize += CueTrackPositions.id.length + EbmlUtil.ebmlLength(cueTrackPositionSize);

        int cuePointSize = CuePoint.id.length + EbmlUtil.ebmlLength(cueTimeSize + cueTrackPositionSize) + cueTimeSize + cueTrackPositionSize;
        return cuePointSize;
    }
    
    public static class CuePointMock {

        public int cueClusterPositionSize;
        public long elementOffset;
        private long timecode;
        private long size;
        private byte[] id;
        
        public static CuePointMock make(EbmlMaster c){
            MKVType[] path = { Cluster, Timecode };
            EbmlUint tc = (EbmlUint) findFirst(c, path);
            return doMake(c.id, tc.getUint(), c.size());
        }

        public static CuePointMock doMake(byte[] id, long timecode, long size) {
            CuePointMock mock = new CuePointMock();
            mock.id = id;
            mock.timecode = timecode; 
            mock.size = size;
            return mock;
        }
        
    }
}