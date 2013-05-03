package org.jcodec.containers.mkv.elements;

import java.util.List;

public class BlockGroup {

    public final List<Long> referenceBlocks;
    public final long blockDuration;
    public final Block block;

    public BlockGroup(long contentOffset, long contentSize, List<Long> referenceBlocks, long blockDuration) {
        this.block = new Block(contentOffset, contentSize);
        this.referenceBlocks = referenceBlocks;
        this.blockDuration = blockDuration;
    }

    public static BlockGroup create(long contentOffset, long contentSize, List<Long> referenceBlocks, long blockDuration) {
        BlockGroup bg = new BlockGroup(contentOffset, contentSize, referenceBlocks, blockDuration);
        return bg;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(block.toString());
        for(Long ref : referenceBlocks){
            sb.append("Reference: ").append(ref).append("\n");
        }
        sb.append("Duration: ").append(blockDuration).append("\n");
        return sb.toString();
    }
    
    

}
