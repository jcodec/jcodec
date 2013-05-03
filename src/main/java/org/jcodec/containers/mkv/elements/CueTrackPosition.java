package org.jcodec.containers.mkv.elements;

import java.util.List;

import org.jcodec.containers.mkv.EBMLException;

public class CueTrackPosition {

    public final List<Long> trackIds;
    public final List<Long> clusterPositions;
    public final long blockNumber;

    public CueTrackPosition(List<Long> trackIds, List<Long> clusterPositions, long blockNumber){
        this.trackIds = trackIds;
        this.clusterPositions = clusterPositions;
        this.blockNumber = blockNumber;
        
    }
    
    public static CueTrackPosition create(List<Long> trackIds, List<Long> clusterPositions, long blockNumber) throws EBMLException {
        if (trackIds == null || trackIds.isEmpty())
            throw new EBMLException("CuePosition element has no CueTrack block!");
        
        if (clusterPositions == null || clusterPositions.isEmpty())
            throw new EBMLException("CuePosition element has no CueClusterPosition block!");
        
        return new CueTrackPosition(trackIds, clusterPositions, blockNumber);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Long i:trackIds)
            sb.append("TrackId: ").append(i.longValue()).append("\n");
        
        for (Long i:clusterPositions)
            sb.append("ClusterPostition: ").append(i.longValue()).append("\n");
        
        sb.append("BlockNumber: ").append(blockNumber).append("\n");
        return sb.toString();
    }

    
}
