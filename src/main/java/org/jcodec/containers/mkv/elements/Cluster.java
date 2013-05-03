package org.jcodec.containers.mkv.elements;

import java.util.ArrayList;
import java.util.List;

import org.jcodec.containers.mkv.Type;
import org.jcodec.containers.mkv.ebml.Element;
import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.ebml.UnsignedIntegerElement;

public class Cluster extends MasterElement implements Comparable<Cluster> {

    public List<Block> simpleBlockContentOffsets;
    public List<BlockGroup> blockGroups;
    public long timecode;
    public long prevsize;
    
    public Cluster(byte[] b){
        super(b);
    }

    public Cluster(List<Block> simpleBlockContentOffsets, List<BlockGroup> blockGroups, long timecode, long prevsize) {
        super(Type.Cluster.id);
        this.simpleBlockContentOffsets = simpleBlockContentOffsets;
        this.blockGroups = blockGroups;
        this.timecode = timecode;
        this.prevsize = prevsize;
    }

    public static Cluster create(List<Block> simpleBlockContentOffsets, List<BlockGroup> blockGroups, long timecode,
            long prevsize) {
        return new Cluster(simpleBlockContentOffsets, blockGroups, timecode, prevsize);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Timecode: ").append(timecode).append("\n");
        sb.append("Prevsize: ").append(prevsize).append("\n");
        for (Block simple : simpleBlockContentOffsets)
            sb.append("SimpleBlock: ").append(simple.toString());
        for(BlockGroup bg : blockGroups)
            sb.append("BlockGroup: ").append(bg.toString());
        return sb.toString();
    }
    
    public List<BlockElement> getBlocksByTrackNumber(long nr){
        List<BlockElement> blocks = new ArrayList<BlockElement>();
        for(Element child : children){
            BlockElement block = null;
            if (child.type.equals(Type.SimpleBlock))
                block = (BlockElement) child;
            else if (child.type.equals(Type.BlockGroup))
                block = (BlockElement) Type.findFirst(child, Type.BlockGroup, Type.Block);
            else 
                continue;
            
            if (block.trackNumber == nr)
                blocks.add(block);
        }
        return blocks;
    }

    public long getMinTimecode(int trackNr) {
        UnsignedIntegerElement timecode  = (UnsignedIntegerElement) Type.findFirst(this, Type.Cluster, Type.Timecode);
        long clusterTimecode = timecode.get();
        long minTimecode = clusterTimecode;
        for (BlockElement be :  getBlocksByTrackNumber(trackNr))
            if (clusterTimecode + be.timecode < minTimecode)
                minTimecode = clusterTimecode + be.timecode;
        
        return minTimecode;
    }
    
    public long getMaxTimecode(int trackNr) {
        UnsignedIntegerElement timecode  = (UnsignedIntegerElement) Type.findFirst(this, Type.Cluster, Type.Timecode);
        long clusterTimecode = timecode.get();
        long maxTimecode = clusterTimecode;
        for (BlockElement be :  getBlocksByTrackNumber(trackNr))
            if (clusterTimecode + be.timecode > maxTimecode)
                maxTimecode = clusterTimecode + be.timecode;
        
        return maxTimecode;
    }

    @Override
    public int compareTo(Cluster o) {
        return (int)(this.timecode - o.timecode);
    }
}
