package org.jcodec.containers.mkv;

import static org.jcodec.containers.mkv.Type.CueClusterPosition;
import static org.jcodec.containers.mkv.Type.CuePoint;
import static org.jcodec.containers.mkv.Type.CueTime;
import static org.jcodec.containers.mkv.Type.CueTrack;
import static org.jcodec.containers.mkv.Type.CueTrackPositions;
import static org.jcodec.containers.mkv.Type.Cues;
import static org.jcodec.containers.mkv.Type.createElementByType;
import static org.jcodec.containers.mkv.ebml.Element.getEbmlSize;
import static org.jcodec.containers.mkv.ebml.UnsignedIntegerElement.getMinByteSizeUnsigned;

import java.util.ArrayList;
import java.util.List;

import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.ebml.UnsignedIntegerElement;
import org.jcodec.containers.mkv.elements.Cluster;

public class CuesIndexer {
    List<CuesIndexer.CuePointMock> a = new ArrayList<CuesIndexer.CuePointMock>();
    private final long offsetBase;
    private long currentDataOffset = 0;
    private long videoTrackNr;
    
    public CuesIndexer(long offset, long videoTrack){
        this.offsetBase = offset;
        this.videoTrackNr = videoTrack;
        this.currentDataOffset += offsetBase;
    }
    
    public void add(CuesIndexer.CuePointMock z) {
        z.dataOffset = currentDataOffset;
        z.cueClusterPositionSize = getMinByteSizeUnsigned(z.dataOffset);
        currentDataOffset += z.size;
//        System.out.println("Added id:"+Reader.printAsHex(z.id)+" offset:"+z.dataOffset+" size:"+z.size+" cueClusterPosition size:"+z.cueClusterPositionSize);
        a.add(z);
    }
    
    public MasterElement createCues(){
        int estimatedSize = computeCuesSize();
        MasterElement cues = Type.createElementByType(Cues);
        for (CuesIndexer.CuePointMock cpm : a){
            MasterElement cuePoint = createElementByType(CuePoint);
            
            UnsignedIntegerElement cueTime = createElementByType(CueTime);
            cueTime.set(cpm.timecode);
            cuePoint.addChildElement(cueTime);
            
            MasterElement cueTrackPositions = createElementByType(CueTrackPositions);
            
            UnsignedIntegerElement cueTrack = createElementByType(CueTrack);
            cueTrack.set(videoTrackNr);
            cueTrackPositions.addChildElement(cueTrack);
            
            UnsignedIntegerElement cueClusterPosition = createElementByType(CueClusterPosition);
            cueClusterPosition.set(cpm.dataOffset+estimatedSize);
            if (cueClusterPosition.getData().length != cpm.cueClusterPositionSize)
                System.err.println("estimated size of CueClusterPosition differs from the one actually used. ElementId: "+Reader.printAsHex(cpm.id)+" "+cueClusterPosition.getData().length+" vs "+cpm.cueClusterPositionSize);
            cueTrackPositions.addChildElement(cueClusterPosition);
            
            cuePoint.addChildElement(cueTrackPositions);
            
            cues.addChildElement(cuePoint);
        }
        return cues;
    }
    
    public int computeCuesSize() {
        int cuesSize = estimateSize();
        boolean reindex = false;
        do {
            reindex = false;
            for (CuesIndexer.CuePointMock z : a) {
                int minByteSize = getMinByteSizeUnsigned(z.dataOffset + cuesSize);
                if (minByteSize > z.cueClusterPositionSize) {
                    System.err.println("Size "+cuesSize+" seems too small for element "+Reader.printAsHex(z.id)+" increasing size by one.");
                    z.cueClusterPositionSize +=1;
                    cuesSize += 1;
                    reindex = true;
                    break;
                } else if (minByteSize < z.cueClusterPositionSize){
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

    int estimateSize() {
        int s = 0;
        for (CuesIndexer.CuePointMock cpm : a) 
            s += estimateCuePointSize(getMinByteSizeUnsigned(cpm.timecode), getMinByteSizeUnsigned(videoTrackNr), getMinByteSizeUnsigned(cpm.dataOffset));
        
        s += Cues.id.length + getEbmlSize(s);
        return s;
    }
    
    public static int estimateCuePointSize(int timecodeSizeInBytes, int trackNrSizeInBytes, int clusterPositionSizeInBytes) {
        int cueTimeSize = CueTime.id.length + getEbmlSize(timecodeSizeInBytes) + timecodeSizeInBytes;
        int cueTrackPositionSize = Type.CueTrack.id.length + getEbmlSize(trackNrSizeInBytes) + trackNrSizeInBytes +
                                     CueClusterPosition.id.length + getEbmlSize(clusterPositionSizeInBytes) + clusterPositionSizeInBytes;
        cueTrackPositionSize += Type.CueTrackPositions.id.length + getEbmlSize(cueTrackPositionSize);

        int cuePointSize = CuePoint.id.length + getEbmlSize(cueTimeSize + cueTrackPositionSize) + cueTimeSize + cueTrackPositionSize;
        return cuePointSize;
    }
    
    public static class CuePointMock {

        public int cueClusterPositionSize;
        public long dataOffset;
        private long timecode;
        private long size;
        private byte[] id;
        
        public static CuePointMock make(Cluster c){
            UnsignedIntegerElement tc = (UnsignedIntegerElement) Type.findFirst(c, Type.Cluster, Type.Timecode);
            return make(c.id, tc.get(), c.getSize());
        }

        public static CuePointMock make(byte[] id, long timecode, long size) {
            CuesIndexer.CuePointMock mock = new CuePointMock();
            mock.id = id;
            mock.timecode = timecode; 
            mock.size = size;
            return mock;
        }
        
    }
}